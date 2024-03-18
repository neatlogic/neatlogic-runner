/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
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
        StringBuilder nodeStatusPath = new StringBuilder(Config.AUTOEXEC_HOME() + File.separator + JobUtil.getJobPath(jobId.toString(), new StringBuilder()) + File.separator + "status" + File.separator + phase + File.separator);
        //重置单个或多个节点
        if (CollectionUtils.isNotEmpty(phaseNodeList)) {
            for (int i = 0; i < phaseNodeList.size(); i++) {
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
        }
        if (CollectionUtils.isNotEmpty(jobPhaseNodeSqlList)) {
            for (int i = 0; i < jobPhaseNodeSqlList.size(); i++) {
                JSONObject node = jobPhaseNodeSqlList.getJSONObject(i);
                String host = node.getString("host");
                Integer port = node.getInteger("port");
                nodeStatusPath.append(host).append("-").append(port == null ? StringUtils.EMPTY : port).append("-").append(node.getString("resourceId")).append(File.separator).append(node.getString("sqlFile")).append(".txt");
            }
        } else {
            //重置整个phase
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
            FileUtil.deleteDirectoryOrFile(nodeStatusPath.toString());
        }
        return null;
    }

    @Override
    public String getToken() {
        return "/job/phase/node/status/reset";
    }
}
