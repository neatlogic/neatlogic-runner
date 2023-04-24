/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package com.techsure.autoexecrunner.api.job.node;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.common.config.Config;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Output;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.techsure.autoexecrunner.util.FileUtil;
import com.techsure.autoexecrunner.util.JobUtil;
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
            if(Objects.equals(execMode,"runner")){
                resourceIdList.add(0L);
            }else {
                resourceIdList = phaseNodeList.stream().map(o -> (JSONObject.parseObject(JSONObject.toJSONString(o))).getLong("resourceId")).collect(Collectors.toList());
            }
            Bson filterDoc = combine(in("resourceId", resourceIdList), eq("phase", phase));
            Bson updateDoc = set("data.status", nodeStatus);
            mongoTemplate.getCollection("_node_status").updateMany(filterDoc, updateDoc);
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
