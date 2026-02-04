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
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Output;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.neatlogic.autoexecrunner.util.FileUtil;
import com.neatlogic.autoexecrunner.util.JobUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;


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
            if (Objects.equals(execMode, "runner")) {
                JSONObject localNode = new JSONObject();
                localNode.put("resourceId", 0);
                localNode.put("host", "local");
                localNode.put("port", 0);
                phaseNodeList.add(localNode);
            }

            int batchSize = 100;
            List<WriteModel<Document>> batch = new ArrayList<>(batchSize);

            for (int i = 0; i < phaseNodeList.size(); i++) {
                JSONObject phaseNode = phaseNodeList.getJSONObject(i);

                Bson filter = combine(
                        eq("phase", phase),
                        eq("resourceId", phaseNode.getLong("resourceId"))
                );

                Document update = new Document()
                        .append("$set", new Document()
                                .append("data.status", nodeStatus)
                        )
                        .append("$setOnInsert", new Document()
                                .append("createDate", new Date())
                                .append("jobId", jobId.toString())
                                .append("host", phaseNode.getString("host"))
                                .append("port", phaseNode.getString("port"))
                        );
                batch.add(new UpdateOneModel<>(filter, update, new UpdateOptions().upsert(true)));

                if (batch.size() >= batchSize || i == (phaseNodeList.size() - 1)) {
                    BulkWriteResult result = mongoTemplate.getCollection("_node_status").bulkWrite(batch, new BulkWriteOptions().ordered(false));
                    logger.info("Batch upserted: {} docs", result.getUpserts().size());
                    batch.clear();
                }
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
                    if (StringUtils.isNotBlank(nodeStatusJsonStr)) {
                        JSONObject nodeStatusJson = JSONObject.parseObject(nodeStatusJsonStr);
                        if (MapUtils.isNotEmpty(nodeStatusJson)) {
                            nodeStatusJson.put("status", nodeStatus);
                            FileUtil.saveFile(nodeStatusJson.toJSONString(), nodeStatusPath);
                        }
                    }
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
