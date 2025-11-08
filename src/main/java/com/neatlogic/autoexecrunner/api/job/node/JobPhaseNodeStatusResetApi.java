/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
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
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;


/**
 * @author lvzk
 * @since 2021/6/2 15:31
 **/
@Component
public class JobPhaseNodeStatusResetApi extends PrivateApiComponentBase {
    static Logger logger = LoggerFactory.getLogger(JobPhaseNodeStatusResetApi.class);

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
        JSONArray jobPhaseNodeSqlList = jsonObj.getJSONArray("jobPhaseNodeSqlList");

        //重置单个或多个节点
        if (CollectionUtils.isNotEmpty(phaseNodeList)) {
            for (int i = 0; i < phaseNodeList.size(); i++) {
                StringBuilder nodeStatusPath = new StringBuilder(Config.AUTOEXEC_HOME() + File.separator + JobUtil.getJobPath(jobId.toString(), new StringBuilder()) + File.separator + "status" + File.separator + phase + File.separator);
                //删除db对应的status记录
                JSONObject node = phaseNodeList.getJSONObject(i);
                String host = node.getString("host");
                Integer port = node.getInteger("port");
                Document document = new Document();
                document.put("jobId", jobId.toString());
                document.put("phase", phase);
                document.put("resourceId", node.getLong("resourceId"));
                try {
                    mongoTemplate.getCollection("_node_status").deleteMany(document);
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                    throw new MongodbException();
                }
                //删除对应status文件记录
                if (Arrays.asList("target", "runner_target").contains(execMode)) {
                    nodeStatusPath.append(host).append("-").append(port == null ? StringUtils.EMPTY : port).append("-").append(node.getString("resourceId")).append(".json");
                } else {
                    nodeStatusPath.append("local-0-0.json");
                }
                FileUtil.deleteDirectoryOrFile(nodeStatusPath.toString());
            }
        } else if (CollectionUtils.isNotEmpty(jobPhaseNodeSqlList)) {
            for (int i = 0; i < jobPhaseNodeSqlList.size(); i++) {
                StringBuilder nodeStatusPath = new StringBuilder(Config.AUTOEXEC_HOME() + File.separator + JobUtil.getJobPath(jobId.toString(), new StringBuilder()) + File.separator + "status" + File.separator + phase + File.separator);
                JSONObject node = jobPhaseNodeSqlList.getJSONObject(i);
                String host = node.getString("host");
                Integer port = node.getInteger("port");
                nodeStatusPath.append(host).append("-").append(port == null ? StringUtils.EMPTY : port).append("-").append(node.getString("resourceId")).append(File.separator).append(node.getString("sqlFile")).append(".txt");
                //删除对应status文件记录
                FileUtil.deleteDirectoryOrFile(nodeStatusPath.toString());
            }
        } else {
            //重置整个phase
            String nodeStatusPath = Config.AUTOEXEC_HOME() + File.separator + JobUtil.getJobPath(jobId.toString(), new StringBuilder()) + File.separator + "status" + File.separator + phase + File.separator;
            Document document = new Document();
            document.put("jobId", jobId.toString());
            document.put("phase", phase);
            try {
                mongoTemplate.getCollection("_node_status").deleteMany(document);
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                throw new MongodbException();
            }
            //删除对应status文件记录
            FileUtil.deleteDirectoryOrFile(nodeStatusPath);
        }
        return null;
    }

    @Override
    public String getToken() {
        return "/job/phase/node/status/reset";
    }
}
