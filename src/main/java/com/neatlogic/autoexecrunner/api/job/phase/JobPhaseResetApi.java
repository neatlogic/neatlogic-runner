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
package com.neatlogic.autoexecrunner.api.job.phase;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.api.job.node.UpdateJobPhaseNodeStatusApi;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.exception.MongodbException;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Output;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.neatlogic.autoexecrunner.util.FileUtil;
import com.neatlogic.autoexecrunner.util.JobUtil;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;


/**
 * @author lvzk
 * @since 2022/5/11 15:31
 **/
@Component
public class JobPhaseResetApi extends PrivateApiComponentBase {

    private static final Logger logger = LoggerFactory.getLogger(UpdateJobPhaseNodeStatusApi.class);

    @Override
    public String getName() {
        return "重置作业阶段";
    }

    @Resource
    MongoTemplate mongoTemplate;

    @Input({
            @Param(name = "jobId", type = ApiParamType.STRING, desc = "作业Id", isRequired = true),
            @Param(name = "phaseName", type = ApiParamType.STRING, desc = "作业阶段名", isRequired = true)
    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        String phaseName = jsonObj.getString("phaseName");
        Document document = new Document();
        document.put("jobId", jobId.toString());
        document.put("phase", phaseName);
        try {
            mongoTemplate.getCollection("_node_status").deleteMany(document);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new MongodbException();
        }
        //删除对应status文件记录
        String nodeStatusPath = Config.AUTOEXEC_HOME() + File.separator + JobUtil.getJobPath(jobId.toString(), new StringBuilder()) + File.separator + "status" + File.separator + phaseName;
        FileUtil.deleteDirectoryOrFile(nodeStatusPath);
        return null;
    }

    @Override
    public String getToken() {
        return "/job/phase/reset";
    }
}
