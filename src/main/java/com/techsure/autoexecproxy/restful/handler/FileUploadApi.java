package com.techsure.autoexecproxy.restful.handler;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecproxy.constvalue.ApiParamType;
import com.techsure.autoexecproxy.restful.annotation.Description;
import com.techsure.autoexecproxy.restful.annotation.Input;
import com.techsure.autoexecproxy.restful.annotation.Param;
import com.techsure.autoexecproxy.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Title: FileUploadApi
 * @Package: com.techsure.autoexecproxy.restful.handler
 * @Description: 附件上传接口
 * @author: chenqiwei
 * @date: 2021/2/1011:48 上午
 * Copyright(c) 2021 TechSure Co.,Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 **/
@Component
public class FileUploadApi extends PrivateBinaryStreamApiComponentBase {

    @Override
    public String getName() {
        return "附件上传";
    }

    @Override
    public String getToken() {
        return "fileupload";
    }

    @Input({@Param(name = "param", type = ApiParamType.STRING, desc = "附件参数名称", isRequired = true)})
    @Description(desc = "附件上传接口")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        String paramName = paramObj.getString("param");
        String type = paramObj.getString("type");
        MultipartFile multipartFile = multipartRequest.getFile(paramName);
        if (multipartFile != null) {
            return multipartFile.getOriginalFilename();
        }
        return null;
    }
}
