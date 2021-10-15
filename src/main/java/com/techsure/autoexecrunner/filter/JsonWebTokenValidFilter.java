package com.techsure.autoexecrunner.filter;


import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.asynchronization.threadlocal.TenantContext;
import com.techsure.autoexecrunner.asynchronization.threadlocal.UserContext;
import com.techsure.autoexecrunner.common.config.Config;
import com.techsure.autoexecrunner.dto.UserVo;
import com.techsure.autoexecrunner.filter.core.ILoginAuthHandler;
import com.techsure.autoexecrunner.filter.core.LoginAuthFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter(filterName = "jsonWebTokenValidFilter",urlPatterns = {"/api/*"})
public class JsonWebTokenValidFilter extends OncePerRequestFilter {

    /**
     * Default constructor.
     */
    public JsonWebTokenValidFilter() {
    }

    @Override
    public void destroy() {
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String timezone = "+8:00";
        boolean isAuth = false;
        ILoginAuthHandler loginAuth = null;
        boolean isUnExpired = false;
        boolean hasTenant = false;
        UserVo userVo = null;
        JSONObject redirectObj = new JSONObject();
        String authType = null;

        //判断租户
        try {
            String tenant = request.getHeader("Tenant");
            if (StringUtils.isNotBlank(tenant)) {
                hasTenant = true;
                //先按 default 认证，不存在才根据具体 AuthType 认证用户
                loginAuth = LoginAuthFactory.getLoginAuth("token");
                userVo = loginAuth.auth(request, response);
                if (userVo == null || StringUtils.isBlank(userVo.getUuid())) {
                    authType = request.getHeader("AuthType");
                    if (StringUtils.isNotBlank(authType)) {
                        loginAuth = LoginAuthFactory.getLoginAuth(authType);
                        if (loginAuth != null) {
                            userVo = loginAuth.auth(request, response);
                        }
                    } else {
                        loginAuth = null;
                    }
                }
                if (userVo != null && StringUtils.isNotBlank(userVo.getUuid())) {
                    UserContext.init(userVo, timezone, request, response);
                    TenantContext.init();
                    TenantContext.get().switchTenant(tenant);
                    UserContext.get().setToken(userVo.getAuthorization());
                    //isUnExpired = userExpirationValid(); //没有校验用户登录有效性 可能存在漏洞
                    isUnExpired= true;
                    isAuth = true;
                }
            }

            if (hasTenant && isAuth && isUnExpired) {
                filterChain.doFilter(request, response);
            } else {
                if (!hasTenant) {
                    response.setStatus(521);
                    redirectObj.put("Status", "FAILED");
                    redirectObj.put("Message", "租户 '" + tenant + "' 不存在或已被禁用");
                } else if (loginAuth == null) {
                    response.setStatus(522);
                    redirectObj.put("Status", "FAILED");
                    redirectObj.put("Message", "找不到认证方式 '" + authType + "'");
                } else if (userVo != null && StringUtils.isBlank(userVo.getAuthorization())) {
                    response.setStatus(522);
                    redirectObj.put("Status", "FAILED");
                    redirectObj.put("Message", "没有找到认证信息，请登录");
                } else if (isAuth && !isUnExpired) {
                    response.setStatus(522);
                    redirectObj.put("Status", "FAILED");
                    redirectObj.put("Message", "会话已超时或已被终止，请重新登录");
                } else {
                    response.setStatus(522);
                    redirectObj.put("Status", "FAILED");
                    redirectObj.put("Message", "用户认证失败，请登录");
                }
                response.setContentType(Config.RESPONSE_TYPE_JSON);
                response.getWriter().print(redirectObj.toJSONString());
            }
        } catch (Exception ex) {
            logger.error("认证失败", ex);
            response.setStatus(522);
            redirectObj.put("Status", "FAILED");
            redirectObj.put("Message", "认证失败，具体异常请查看日志");
            response.setContentType(Config.RESPONSE_TYPE_JSON);
            response.getWriter().print(redirectObj.toJSONString());
        }

    }
}
