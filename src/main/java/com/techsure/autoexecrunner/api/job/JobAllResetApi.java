/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package com.techsure.autoexecrunner.api.job;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.common.config.Config;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.util.JobUtil;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Output;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.techsure.autoexecrunner.util.FileUtil;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;


/**
 * @author lvzk
 * @since 2021/7/20 15:31
 **/
@Component
public class JobAllResetApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "重置作业";
    }

    @Resource
    MongoTemplate mongoTemplate;

    @Input({
            @Param(name = "jobId", type = ApiParamType.STRING, desc = "作业Id", isRequired = true)
    })
    @Output({
    })
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long jobId = jsonObj.getLong("jobId");
        Document document = new Document();
        document.put("jobId", jobId.toString());
        mongoTemplate.getCollection("_node_status").deleteMany(document);
        //删除对应status文件记录
        String nodeStatusPath = Config.AUTOEXEC_HOME() + File.separator + JobUtil.getJobPath(jobId.toString(), new StringBuilder()) + File.separator + "status";
        FileUtil.deleteDirectoryOrFile(nodeStatusPath);
        return null;
    }

    @Override
    public String getToken() {
        return "/job/all/reset";
    }
}