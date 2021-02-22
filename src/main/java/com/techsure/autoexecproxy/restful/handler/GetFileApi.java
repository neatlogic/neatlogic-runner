package com.techsure.autoexecproxy.restful.handler;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecproxy.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * @Title: GetFileApi
 * @Package: com.techsure.autoexecproxy.restful.handler
 * @Description: TODO
 * @author: chenqiwei
 * @date: 2021/2/222:57 下午
 * Copyright(c) 2021 TechSure Co.,Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 **/
@Component
public class GetFileApi extends PrivateBinaryStreamApiComponentBase {
    @Override
    public String getName() {
        return "获取附件";
    }

    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ServletOutputStream os;
        InputStream in;
        File file = new File("/Users/chenqiwei/idea_project/autoexec/test/fetchfile");
        in = new FileInputStream(file);
        String fileNameEncode;
        boolean flag = request.getHeader("User-Agent").indexOf("Gecko") > 0;
        if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0 || flag) {
            fileNameEncode = URLEncoder.encode(file.getName(), "UTF-8");// IE浏览器
        } else {
            fileNameEncode = new String(file.getName().replace(" ", "").getBytes(StandardCharsets.UTF_8), "ISO8859-1");
        }

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment;filename=\"" + fileNameEncode + "\"");
        os = response.getOutputStream();
        IOUtils.copyLarge(in, os);
        if (os != null) {
            os.flush();
            os.close();
        }
        in.close();
        return null;
    }

    @Override
    public String getToken() {
        return "getfile";
    }
}
