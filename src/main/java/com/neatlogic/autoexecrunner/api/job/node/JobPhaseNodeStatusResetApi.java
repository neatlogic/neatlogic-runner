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
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Output;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.neatlogic.autoexecrunner.util.FileUtil;
import com.neatlogic.autoexecrunner.util.JobUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.Objects;


/**
 * @author lvzk
 * @since 2021/6/2 15:31
 **/
@Component
public class JobPhaseNodeStatusResetApi extends PrivateApiComponentBase {
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
        //重置单个或多个节点
        if (CollectionUtils.isNotEmpty(phaseNodeList)) {
            phaseNodeList.forEach(n -> {
                //删除db对应的status记录
                JSONObject node = JSONObject.parseObject(JSONObject.toJSONString(n));
                String host = node.getString("host");
                Integer port = node.getInteger("port");
                Document document = new Document();
                document.put("jobId", jobId.toString());
                document.put("phase", phase);
                document.put("resourceId",node.getLong("resourceId"));
                Document result = mongoTemplate.getCollection("_node_status").findOneAndDelete(document);
                //删除对应status文件记录
                String nodeStatusPath = Config.AUTOEXEC_HOME() + File.separator + JobUtil.getJobPath(jobId.toString(), new StringBuilder()) + File.separator + "status" + File.separator + phase + File.separator;
                if(Objects.equals("sqlfile", execMode)){
                    nodeStatusPath += host + "-" + (port==null?StringUtils.EMPTY:port) + "-" +node.getString("resourceId") + File.separator+ node.getString("sqlFile")+".txt";
                } else if(Arrays.asList("target","runner_target").contains(execMode)) {
                    nodeStatusPath += host + "-" + (port==null?StringUtils.EMPTY:port) + "-" +node.getString("resourceId") + ".json";
                } else {
                    nodeStatusPath += "local-0-0.json";
                }
                FileUtil.deleteDirectoryOrFile(nodeStatusPath);
            });
        } else {
            //重置整个phase
            Document document = new Document();
            document.put("jobId", jobId.toString());
            document.put("phase", phase);
            Document result = mongoTemplate.getCollection("_node_status").findOneAndDelete(document);
            //删除对应status文件记录
            String nodeStatusPath = Config.AUTOEXEC_HOME() + File.separator + JobUtil.getJobPath(jobId.toString(), new StringBuilder()) + File.separator + "status" + File.separator + phase;
            FileUtil.deleteDirectoryOrFile(nodeStatusPath);
        }
        return null;
    }

    @Override
    public String getToken() {
        return "/job/phase/node/status/reset";
    }
}
