package com.neatlogic.autoexecrunner.filter;


import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.RequestContext;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.TenantContext;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.UserContext;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.dto.UserVo;
import com.neatlogic.autoexecrunner.filter.core.ILoginAuthHandler;
import com.neatlogic.autoexecrunner.filter.core.LoginAuthFactory;
import org.apache.commons.lang3.StringUtils;
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
        boolean hasTenant = false;
        UserVo userVo = null;
        JSONObject redirectObj = new JSONObject();
        String authType = null;
        //初始化request上下文
        RequestContext.init(request, request.getRequestURI(), response);
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
                    isAuth = true;
                }
            }

            if (hasTenant && isAuth) {
                //兼容“处理response,对象toString可能会异常”的场景，过了filter，应该是520异常
                try {
                    filterChain.doFilter(request, response);
                }catch (Exception ex){
                    logger.error(ex.getMessage(), ex);
                    response.setStatus(520);
                    redirectObj.put("Status", "ERROR");
                    redirectObj.put("Message", ex.getMessage());
                    response.setContentType(Config.RESPONSE_TYPE_JSON);
                    response.getWriter().print(redirectObj.toJSONString());
                }
            } else {
                if (!hasTenant) {
                    response.setStatus(521);
                    redirectObj.put("Status", "FAILED");
                    redirectObj.put("Message", "租户 '" + tenant + "' 不存在或已被禁用");
                } else {
                    response.setStatus(522);
                    redirectObj.put("Status", "FAILED");
                    redirectObj.put("Message", "用户认证失败，请核对neatlogic和runner的jwt.secret是否一致");
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
