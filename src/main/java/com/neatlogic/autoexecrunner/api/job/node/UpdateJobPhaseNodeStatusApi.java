/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.neatlogic.autoexecrunner.api.job.node;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.exception.MongodbException;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Output;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.neatlogic.autoexecrunner.util.FileUtil;
import com.neatlogic.autoexecrunner.util.JobUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;


/**
 * @author lvzk
 * @since 2022/9/30 15:31
 **/
@Component
public class UpdateJobPhaseNodeStatusApi extends PrivateApiComponentBase {
    private static final Logger logger = LoggerFactory.getLogger(UpdateJobPhaseNodeStatusApi.class);

    @Override
    public String getName() {
        return "重置作业阶段节点";
    }

    @Resource
    MongoTemplate mongoTemplate;

    @Input({
            @Param(name = "jobId", type = ApiParamType.STRING, desc = "作业Id", isRequired = true),
            @Param(name = "phaseName", type = ApiParamType.STRING, desc = "作业剧本Name", isRequired = true),
            @Param(name = "phaseNodeList", type = ApiParamType.JSONARRAY, desc = "阶段节点列表"),
            @Param(name = "sqlName", type = ApiParamType.STRING, desc = "sql名"),
            @Param(name = "nodeStatus", type = ApiParamType.STRING, desc = "节点状态", isRequired = true),
            @Param(name = "execMode", type = ApiParamType.STRING, desc = "执行方式", isRequired = true)
    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        String phase = jsonObj.getString("phaseName");
        String execMode = jsonObj.getString("execMode");
        JSONArray phaseNodeList = jsonObj.getJSONArray("phaseNodeList");
        String nodeStatus = jsonObj.getString("nodeStatus");
        //更新mongodb 节点状态
        if (!Objects.equals("sqlfile", execMode)) {
            List<Long> resourceIdList = new ArrayList<>();
            if (Objects.equals(execMode, "runner")) {
                resourceIdList.add(0L);
            } else {
                resourceIdList = phaseNodeList.stream().map(o -> (JSONObject.parseObject(JSONObject.toJSONString(o))).getLong("resourceId")).collect(Collectors.toList());
            }
            Bson filterDoc = combine(in("resourceId", resourceIdList), eq("phase", phase));
            Bson updateDoc = set("data.status", nodeStatus);
            try {
                mongoTemplate.getCollection("_node_status").updateMany(filterDoc, updateDoc);
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                throw new MongodbException();
            }
        }
        //更新节点状态文件
        if (CollectionUtils.isNotEmpty(phaseNodeList)) {
            phaseNodeList.forEach(n -> {
                JSONObject node = JSONObject.parseObject(JSONObject.toJSONString(n));
                String host = node.getString("host");
                Integer port = node.getInteger("port");
                //删除对应status文件记录
                String nodeStatusPath = Config.AUTOEXEC_HOME() + File.separator + JobUtil.getJobPath(jobId.toString(), new StringBuilder()) + File.separator + "status" + File.separator + phase + File.separator;
                if (Objects.equals("sqlfile", execMode)) {
                    nodeStatusPath += host + "-" + (port == null ? StringUtils.EMPTY : port) + "-" + node.getString("resourceId") + File.separator + node.getString("sqlFile") + ".txt";
                } else if (Arrays.asList("target", "runner_target").contains(execMode)) {
                    nodeStatusPath += host + "-" + (port == null ? StringUtils.EMPTY : port) + "-" + node.getString("resourceId") + ".json";
                } else {
                    nodeStatusPath += "local-0-0.json";
                }
                try {
                    String nodeStatusJsonStr = FileUtil.getReadFileContent(nodeStatusPath);
                    JSONObject nodeStatusJson = JSONObject.parseObject(nodeStatusJsonStr);
                    nodeStatusJson.put("status", nodeStatus);
                    FileUtil.saveFile(nodeStatusJson.toJSONString(), nodeStatusPath);
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            });
        }
        return null;
    }

    @Override
    public String getToken() {
        return "/job/phase/node/status/update";
    }
}
