/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package com.techsure.autoexecrunner.api.job.node;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import com.techsure.autoexecrunner.service.AutoexecJobService;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@Component
public class DownloadJobPhaseNodeInputApi extends PrivateBinaryStreamApiComponentBase {
    private static final Logger logger = LoggerFactory.getLogger(DownloadJobPhaseNodeInputApi.class);
    @Resource
    AutoexecJobService autoexecJobService;

    @Override
    public String getToken() {
        return "/job/phase/node/input/download";
    }

    @Override
    public String getName() {
        return "下载剧本节点工具入参";
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业Id", isRequired = true),
            @Param(name = "ip", type = ApiParamType.STRING, desc = "ip"),
            @Param(name = "port", type = ApiParamType.INTEGER, desc = "端口"),
            @Param(name = "execMode", type = ApiParamType.STRING, desc = "执行方式", isRequired = true),
    })
    @Override
    public Object myDoService(JSONObject jsonObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        JSONObject inputParam = autoexecJobService.getJobOperationInputParam(jsonObj);
        try (InputStream in = IOUtils.toInputStream(inputParam.toJSONString(), StandardCharsets.UTF_8.toString()); OutputStream os = response.getOutputStream()) {
            IOUtils.copyLarge(in, os);
            if (os != null) {
                os.flush();
                os.close();
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }

        return null;
    }

}
