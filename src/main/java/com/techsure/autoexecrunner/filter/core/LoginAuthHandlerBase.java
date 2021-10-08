package com.techsure.autoexecrunner.filter.core;


import com.techsure.autoexecrunner.dto.UserVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@DependsOn("loginService")
public abstract class LoginAuthHandlerBase implements ILoginAuthHandler {
    Logger logger = LoggerFactory.getLogger(LoginAuthHandlerBase.class);

    public abstract String getType();

    @Override
    public UserVo auth(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String type = this.getType();
        String tenant = request.getHeader("tenant");
        UserVo userVo = myAuth(request);
        logger.info("loginAuth type: " + type);
        if (userVo != null) {
            logger.info("get userUuId: " + userVo.getUuid());
            logger.info("get userId: " + userVo.getUserId());
        }
        return userVo;
    }

    public abstract UserVo myAuth(HttpServletRequest request) throws ServletException, IOException;
}
