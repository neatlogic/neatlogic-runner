package com.techsure.autoexecproxy.web.handler;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.StringUtils;
import com.techsure.autoexecproxy.common.config.Config;
import com.techsure.autoexecproxy.constvalue.AuthenticateType;
import com.techsure.autoexecproxy.dto.ApiVo;
import com.techsure.autoexecproxy.web.core.ApiAuthBase;
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
    public AuthenticateType getType() {
        return AuthenticateType.BASIC;
    }

    @Override
    public int myAuth(ApiVo interfaceVo, JSONObject jsonParam, HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.isNotBlank(authorization)) {
            authorization = authorization.replace("Basic ", "");
            byte[] bytes = Base64.decodeBase64(authorization);
            authorization = new String(bytes, StandardCharsets.UTF_8);
            String[] as = authorization.split(":");
            if (as.length == 2) {
                String username = as[0];
                String password = as[1];
                if (Config.ACCESS_KEY().equalsIgnoreCase(username) && Config.ACCESS_SECRET().equals(password)) {
                    return 1;
                } else {
                    return 522;//用户验证失败
                }
            } else {
                return 522;//用户验证失败
            }
        }
        return 522;
    }


    @Override
    public JSONObject help() {
        JSONObject helpJson = new JSONObject();
        helpJson.put("title", "Basic认证");
        List<String> detailList = new ArrayList<>();
        helpJson.put("detailList", detailList);
        detailList.add("request header需要包含键值对Authorization:Basic xxx");
        detailList.add("（xxx是 '用户名:密码' 的base64编码）。");
        return helpJson;
    }

}
