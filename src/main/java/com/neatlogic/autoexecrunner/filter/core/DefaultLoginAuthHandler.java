package com.neatlogic.autoexecrunner.filter.core;


import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.UserContext;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.dto.UserVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

@Service
public class DefaultLoginAuthHandler extends LoginAuthHandlerBase {

    @Override
    public String getType() {
        return "token";
    }

    @Override
    public UserVo myAuth(HttpServletRequest request) throws ServletException, IOException {
        //获取 authorization，优先获取header的authorization
        UserVo userVo = new UserVo();
        String authorization = request.getHeader("Authorization");

        if (StringUtils.isNotBlank(authorization)) {
            // 解压内容
            if (authorization.startsWith("GZIP_")) {
                authorization = authorization.substring(5);
                try {
                    byte[] compressDatas = Base64.getDecoder().decode(authorization);
                    ByteArrayInputStream bis = new ByteArrayInputStream(compressDatas);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    GZIPInputStream gzipInputStream = new GZIPInputStream(bis);
                    byte[] buffer = new byte[2048];
                    int n;
                    while ((n = gzipInputStream.read(buffer)) >= 0) {
                        bos.write(buffer, 0, n);
                    }
                    bis.close();
                    gzipInputStream.close();
                    authorization = bos.toString();
                    bos.close();
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
            userVo.setAuthorization(authorization);
        }
        //如果 authorization 存在，则解包获取用户信息
        if (StringUtils.isNotBlank(authorization)) {
            if (authorization.startsWith("Bearer") && authorization.length() > 7) {
                String jwt = authorization.substring(7);
                String[] jwtParts = jwt.split("\\.");
                if (jwtParts.length == 3) {
                    SecretKeySpec signingKey = new SecretKeySpec(Config.JWT_SECRET().getBytes(), "HmacSHA1");
                    Mac mac;
                    try {
                        mac = Mac.getInstance("HmacSHA1");
                        mac.init(signingKey);
                        byte[] rawHmac = mac.doFinal((jwtParts[0] + "." + jwtParts[1]).getBytes());
                        String result = Base64.getUrlEncoder().encodeToString(rawHmac);
                        if (result.equals(jwtParts[2])) {
                            String jwtBody = new String(Base64.getUrlDecoder().decode(jwtParts[1]), StandardCharsets.UTF_8);
                            JSONObject jwtBodyObj = JSONObject.parseObject(jwtBody);
                            userVo.setUuid(jwtBodyObj.getString("useruuid"));
                            userVo.setUserId(jwtBodyObj.getString("userid"));
                            userVo.setUserName(jwtBodyObj.getString("username"));
                            userVo.setAuthorization(authorization);
                            return userVo;
                        }
                    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                        e.printStackTrace();
                    }

                }
            }
            UserContext.init(userVo,"");
        }
        return userVo;
    }
}
