package com.neatlogic.autoexecrunner.util.authtication.handler;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.neatlogic.autoexecrunner.constvalue.AuthenticateType;
import com.neatlogic.autoexecrunner.dto.RestVo;
import com.neatlogic.autoexecrunner.util.authtication.core.IAuthenticateHandler;
import org.springframework.util.Base64Utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HmacAuthenticateHandler implements IAuthenticateHandler {
    @Override
    public String getType() {
        return AuthenticateType.HMAC.getValue();
    }

    @Override
    public void authenticate(HttpURLConnection connection, RestVo rest) throws MalformedURLException {
        //将postdata加密 进而获取authorization
        String postDataBase64 = Base64Utils.encodeToString(JSON.toJSONString(rest.getPayload(), SerializerFeature.WriteMapNullValue).getBytes());
        String sign = rest.getUsername() + "#" + new URL(rest.getUrl()).getPath() + "#" + postDataBase64;
        String authorization = encrypt(rest.getToken(), sign);//认证
        //设置请求头
        connection.addRequestProperty("Authorization", "Hmac " + authorization);
        connection.addRequestProperty("AuthType", "hmac");
        connection.addRequestProperty("x-access-key", rest.getUsername());
    }


    public static String encrypt(String secret, String sign) {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(sign.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : rawHmac) {
                String shaHex = Integer.toHexString(b & 0xFF);
                if (shaHex.length() < 2) {
                    hexString.append(0);
                }
                hexString.append(shaHex);
            }
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "0000000000000000000000000000000000000000000000000000000000000000";
    }
}
