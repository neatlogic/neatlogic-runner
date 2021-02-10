package com.techsure.autoexecproxy.web.core;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecproxy.dto.ApiVo;
import org.apache.commons.net.util.Base64;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class BasicApiAuth extends ApiAuthBase {

    @Override
    public String getType() {
        return ApiVo.AuthenticateType.BASIC.getValue();
    }

    @Override
    public int myAuth(ApiVo interfaceVo, JSONObject jsonParam, HttpServletRequest request) throws IOException {
        String authorization = request.getHeader("Authorization");
        authorization = authorization.replace("Basic ", "");
        byte[] bytes = Base64.decodeBase64(authorization);
        authorization = new String(bytes, StandardCharsets.UTF_8);
        String[] as = authorization.split(":");
        if (as.length == 2) {
            String username = as[0];
            String password = as[1];
            if (interfaceVo.getUsername().equalsIgnoreCase(username) && interfaceVo.getPassword().equals(password)) {
            } else {
                return 522;//用户验证失败
            }
        } else {
            return 522;//用户验证失败
        }
        return 1;
    }


    @Override
    public JSONObject help() {
        JSONObject helpJson = new JSONObject();
        helpJson.put("title", "Basic认证");
        List<String> detailList = new ArrayList<String>();
        helpJson.put("detailList", detailList);
        detailList.add("request header需要包含键值对Authorization:Basic xxx");
        detailList.add("（xxx是 '用户名:密码' 的base64编码）。");
        return helpJson;
    }

}
