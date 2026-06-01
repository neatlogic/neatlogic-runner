package com.neatlogic.autoexecrunner.filter.core;


import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.dto.AuthenticationInfoVo;
import com.neatlogic.autoexecrunner.dto.JwtVo;
import com.neatlogic.autoexecrunner.dto.UserVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

@DependsOn("loginService")
public abstract class LoginAuthHandlerBase implements ILoginAuthHandler {
    Logger logger = LoggerFactory.getLogger(LoginAuthHandlerBase.class);

    public abstract String getType();

    @Override
    public UserVo auth(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String type = this.getType();
        String tenant = request.getHeader("tenant");
        UserVo userVo = myAuth(request);
        //logger.info("loginAuth type: " + type);
        if (userVo != null) {
            //logger.info("get userUuId: " + userVo.getUuid());
            //logger.info("get userId: " + userVo.getUserId());
        }
        return userVo;
    }

    public abstract UserVo myAuth(HttpServletRequest request) throws ServletException, IOException;

    /**
     * 生成jwt对象
     *
     * @param checkUserVo 用户
     * @return jwt对象
     * @throws Exception 异常
     */
    public static JwtVo buildJwt(UserVo checkUserVo, AuthenticationInfoVo authenticationInfoVo) throws Exception {
        Long tokenCreateTime = System.currentTimeMillis();
        JwtVo jwtVo = new JwtVo(checkUserVo, tokenCreateTime, authenticationInfoVo);
        SecretKeySpec signingKey = new SecretKeySpec(Config.JWT_SECRET().getBytes(), "HmacSHA1");
        Mac mac;
        mac = Mac.getInstance("HmacSHA1");
        mac.init(signingKey);
        byte[] rawHmac = mac.doFinal((jwtVo.getJwthead() + "." + jwtVo.getJwtbody()).getBytes());
        String jwtsign = Base64.getUrlEncoder().encodeToString(rawHmac);
        // 压缩cookie内容
        String c = "Bearer_" + jwtVo.getJwthead() + "." + jwtVo.getJwtbody() + "." + jwtsign;
        checkUserVo.setAuthorization(c);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(bos);
        gzipOutputStream.write(c.getBytes());
        gzipOutputStream.close();
        String cc = Base64.getEncoder().encodeToString(bos.toByteArray());
        bos.close();
        jwtVo.setCc(cc);
        jwtVo.setJwtsign(jwtsign);
        checkUserVo.setJwtVo(jwtVo);
        return jwtVo;
    }

    /**
     * 生成jwt对象
     *
     * @param checkUserVo 用户
     * @return jwt对象
     * @throws Exception 异常
     */
    public static JwtVo buildJwt(UserVo checkUserVo) throws Exception {
        return buildJwt(checkUserVo, new AuthenticationInfoVo());
    }
}
