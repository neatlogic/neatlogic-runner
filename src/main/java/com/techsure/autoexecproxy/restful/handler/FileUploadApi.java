package com.techsure.autoexecproxy.restful.handler;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecproxy.dto.ApiVo;
import com.techsure.autoexecproxy.restful.core.privateapi.IPrivateBinaryStreamApiComponent;
import com.techsure.autoexecproxy.restful.core.privateapi.PrivateApiComponentBase;
import com.techsure.autoexecproxy.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Title: FileUploadApi
 * @Package: com.techsure.autoexecproxy.restful.handler
 * @Description: TODO
 * @author: chenqiwei
 * @date: 2021/2/1011:48 上午
 * Copyright(c) 2021 TechSure Co.,Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 **/
public class FileUploadApi extends PrivateBinaryStreamApiComponentBase {

    @Override
    public String getName() {
        return "附件上传";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public int needAudit() {
        return 0;
    }


    @Override
    public String getToken() {
        return "fileupload";
    }

    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        return null;
    }
}
