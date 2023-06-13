/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.neatlogic.autoexecrunner.web;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONReader;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.TenantContext;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.UserContext;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.constvalue.SystemUser;
import com.neatlogic.autoexecrunner.dto.ApiHandlerVo;
import com.neatlogic.autoexecrunner.dto.ApiVo;
import com.neatlogic.autoexecrunner.exception.AnonymousExceptionMessage;
import com.neatlogic.autoexecrunner.exception.ApiNotFoundException;
import com.neatlogic.autoexecrunner.exception.ComponentNotFoundException;
import com.neatlogic.autoexecrunner.exception.TenantNotFoundException;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import com.neatlogic.autoexecrunner.restful.core.IApiComponent;
import com.neatlogic.autoexecrunner.restful.core.IBinaryStreamApiComponent;
import com.neatlogic.autoexecrunner.restful.core.IJsonStreamApiComponent;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentFactory;
import com.neatlogic.autoexecrunner.util.RC4Util;
import com.neatlogic.autoexecrunner.util.TenantUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Objects;

@Controller
@RequestMapping("anonymous/api/")
public class AnonymousApiDispatcher {
    Logger logger = LoggerFactory.getLogger(AnonymousApiDispatcher.class);

    private void doIt(HttpServletRequest request, HttpServletResponse response, String token, boolean tokenHasEncrypted, ApiVo.Type apiType, JSONObject paramObj, JSONObject returnObj, String action) throws Exception {
        ApiVo interfaceVo = PrivateApiComponentFactory.getApiByToken(token);

        if (interfaceVo == null) {
            throw new ApiNotFoundException(token);
        } else if (interfaceVo.getPathVariableObj() != null) {
            // 融合路径参数
            paramObj.putAll(interfaceVo.getPathVariableObj());
        }

        ApiHandlerVo apiHandlerVo = PrivateApiComponentFactory.getApiHandlerByHandler(interfaceVo.getHandler());
        if (apiHandlerVo == null) {
            throw new ComponentNotFoundException("接口组件:" + interfaceVo.getHandler() + "不存在");
        }

        if (apiType.equals(ApiVo.Type.OBJECT)) {
            IApiComponent restComponent = PrivateApiComponentFactory.getInstance(interfaceVo.getHandler());
            if (restComponent != null) {
                if (!restComponent.supportAnonymousAccess().isSupportAnonymousAccess()
                        || !Objects.equals(restComponent.supportAnonymousAccess().isRequireTokenEncryption(), tokenHasEncrypted)) {
                    throw new AnonymousExceptionMessage();
                }
                if (action.equals("doservice")) {
                    /* 统计接口访问次数 */
                    Long starttime = System.currentTimeMillis();
                    Object returnV = restComponent.doService(interfaceVo, paramObj);
                    Long endtime = System.currentTimeMillis();
                    if (!restComponent.isRaw()) {
                        returnObj.put("TimeCost", endtime - starttime);
                        returnObj.put("Return", returnV);
                        returnObj.put("Status", "OK");
                    } else {
                        returnObj.putAll(JSONObject.parseObject(JSONObject.toJSONString(returnV)));
                    }
                } else {
                    returnObj.putAll(restComponent.help());
                }
            } else {
                throw new ComponentNotFoundException("接口组件:" + interfaceVo.getHandler() + "不存在");
            }
        } else if (apiType.equals(ApiVo.Type.STREAM)) {
            IJsonStreamApiComponent restComponent = PrivateApiComponentFactory.getStreamInstance(interfaceVo.getHandler());
            if (restComponent != null) {
                if (!restComponent.supportAnonymousAccess().isSupportAnonymousAccess()
                        || !Objects.equals(restComponent.supportAnonymousAccess().isRequireTokenEncryption(), tokenHasEncrypted)) {
                    throw new AnonymousExceptionMessage();
                }
                if (action.equals("doservice")) {
                    Long starttime = System.currentTimeMillis();
                    Object returnV = restComponent.doService(interfaceVo, paramObj, new JSONReader(new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8)));
                    Long endtime = System.currentTimeMillis();
                    if (!restComponent.isRaw()) {
                        returnObj.put("TimeCost", endtime - starttime);
                        returnObj.put("Return", returnV);
                        returnObj.put("Status", "OK");
                    } else {
                        returnObj.putAll(JSONObject.parseObject(JSONObject.toJSONString(returnV)));
                    }
                } else {
                    returnObj.putAll(restComponent.help());
                }
            } else {
                throw new ComponentNotFoundException("接口组件:" + interfaceVo.getHandler() + "不存在");
            }
        } else if (apiType.equals(ApiVo.Type.BINARY)) {
            IBinaryStreamApiComponent restComponent = PrivateApiComponentFactory.getBinaryInstance(interfaceVo.getHandler());
            if (restComponent != null) {
                if (!restComponent.supportAnonymousAccess().isSupportAnonymousAccess()
                        || !Objects.equals(restComponent.supportAnonymousAccess().isRequireTokenEncryption(), tokenHasEncrypted)) {
                    throw new AnonymousExceptionMessage();
                }
                if (action.equals("doservice")) {
                    Long starttime = System.currentTimeMillis();
                    Object returnV = restComponent.doService(interfaceVo, paramObj, request, response);
                    Long endtime = System.currentTimeMillis();
                    if (!restComponent.isRaw()) {
                        returnObj.put("TimeCost", endtime - starttime);
                        returnObj.put("Return", returnV);
                        returnObj.put("Status", "OK");
                    } else {
                        returnObj.putAll(JSONObject.parseObject(JSONObject.toJSONString(returnV)));
                    }
                } else {
                    returnObj.putAll(restComponent.help());
                }
            } else {
                throw new ComponentNotFoundException("接口组件:" + interfaceVo.getHandler() + "不存在");
            }
        }

    }

    @RequestMapping(value = "/rest/**", method = RequestMethod.GET)
    public void dispatcherForGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String token = new AntPathMatcher().extractPathWithinPattern(pattern, request.getServletPath());
        String tenant;
        boolean tokenHasEncrypted = true;
        JSONObject paramObj = new JSONObject();
        if (token.startsWith(RC4Util.PRE) || token.startsWith(RC4Util.PRE_OLD)) {
            String decryptData = RC4Util.decrypt(token);
            String[] split = decryptData.split("\\?", 2);
            token = split[0].substring(0, split[0].lastIndexOf("/"));
            tenant = split[0].substring(split[0].lastIndexOf("/") + 1);
            if (split.length == 2) {
                String[] params = split[1].split("&");
                for (String param : params) {
                    String[] array = param.split("=", 2);
                    if (array.length == 2) {
                        paramObj.put(array[0], array[1]);
                    }
                }
            }
        } else {
            tokenHasEncrypted = false;
            String originToken = token;
            token = token.substring(0, token.lastIndexOf("/"));
            tenant = originToken.substring(originToken.lastIndexOf("/") + 1);
            Enumeration<String> paraNames = request.getParameterNames();
            while (paraNames.hasMoreElements()) {
                String p = paraNames.nextElement();
                String[] vs = request.getParameterValues(p);
                if (vs.length > 1) {
                    paramObj.put(p, vs);
                } else {
                    paramObj.put(p, request.getParameter(p));
                }
            }
        }
        if (TenantUtil.hasTenant(tenant)) {
            TenantContext.init();
            TenantContext.get().switchTenant(tenant);
            UserContext.init(SystemUser.ANONYMOUS.getUserVo(), SystemUser.ANONYMOUS.getTimezone(), request, response);
        }
        JSONObject returnObj = new JSONObject();
        try {
            doIt(request, response, token, tokenHasEncrypted, ApiVo.Type.OBJECT, paramObj, returnObj, "doservice");
        } catch (ApiRuntimeException ex) {
            response.setStatus(520);
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
        }  catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            response.setStatus(520);
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ExceptionUtils.getStackFrames(ex));
        }
        if (!response.isCommitted()) {
            response.setContentType(Config.RESPONSE_TYPE_JSON);
            response.getWriter().print(returnObj);
        }
    }

    @RequestMapping(value = "/rest/**", method = RequestMethod.POST)
    public void dispatcherForPost(@RequestBody String jsonStr, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String token = new AntPathMatcher().extractPathWithinPattern(pattern, request.getServletPath());
        boolean tokenHasEncrypted = false;
        if (token.startsWith(RC4Util.PRE) || token.startsWith(RC4Util.PRE_OLD)) {
            tokenHasEncrypted = true;
            token = RC4Util.decrypt(token);
        }
        /* 为兼容gitlab webhook等场景下无法从header传入tenant的问题，
         先从header里获取tenant，如果没有，则从token中获取，token形如（明文或解密后的token）：deploy/ci/gitlab/event/callback/develop，develop即为tenant
        */
        String tenant = request.getHeader("Tenant");
        if (StringUtils.isBlank(tenant)) {
            tenant = token.substring(token.lastIndexOf("/") + 1);
            token = token.substring(0, token.lastIndexOf("/"));
        }
        JSONObject returnObj = new JSONObject();
        JSONObject paramObj;
        try {
            if (TenantUtil.hasTenant(tenant)) {
                TenantContext.init();
                TenantContext.get().switchTenant(tenant);
                UserContext.init(SystemUser.ANONYMOUS.getUserVo(), SystemUser.ANONYMOUS.getTimezone(), request, response);
            } else {
                throw new TenantNotFoundException(tenant);
            }
            if (StringUtils.isNotBlank(jsonStr)) {
                try {
                    paramObj = JSONObject.parseObject(jsonStr);
                } catch (Exception e) {
                    throw new ApiRuntimeException("请求参数需要符合JSON格式");
                }
            } else {
                paramObj = new JSONObject();
            }

            Enumeration<String> paraNames = request.getParameterNames();
            while (paraNames.hasMoreElements()) {
                String p = paraNames.nextElement();
                String[] vs = request.getParameterValues(p);
                if (vs.length > 1) {
                    paramObj.put(p, vs);
                } else {
                    paramObj.put(p, request.getParameter(p));
                }
            }

            doIt(request, response, token, tokenHasEncrypted, ApiVo.Type.OBJECT, paramObj, returnObj, "doservice");
        } catch (ApiRuntimeException ex) {
            response.setStatus(520);
            if (logger.isWarnEnabled()) {
                logger.warn(ex.getMessage(), ex);
            }
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
        } catch (Exception ex) {
            response.setStatus(500);
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ExceptionUtils.getStackTrace(ex));
            logger.error(ex.getMessage(), ex);
        }
        if (!response.isCommitted()) {
            response.setContentType(Config.RESPONSE_TYPE_JSON);
            if (returnObj.containsKey("_disableDetect")) {
                returnObj.remove("_disableDetect");
                response.getWriter().print(returnObj.toString(SerializerFeature.DisableCircularReferenceDetect));
            } else {
                response.getWriter().print(returnObj.toJSONString());
            }
        }

    }

    @RequestMapping(value = "/binary/**", method = RequestMethod.GET)
    public void dispatcherForPostBinary(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String token = new AntPathMatcher().extractPathWithinPattern(pattern, request.getServletPath());
        String tenant;
        boolean tokenHasEncrypted = true;
        JSONObject paramObj = new JSONObject();
        if (token.startsWith(RC4Util.PRE) || token.startsWith(RC4Util.PRE_OLD)) {
            String decryptData = RC4Util.decrypt(token);
            String[] split = decryptData.split("\\?", 2);
            token = split[0].substring(0, split[0].lastIndexOf("/"));
            tenant = split[0].substring(split[0].lastIndexOf("/") + 1);
            if (split.length == 2) {
                String[] params = split[1].split("&");
                for (String param : params) {
                    String[] array = param.split("=", 2);
                    if (array.length == 2) {
                        paramObj.put(array[0], array[1]);
                    }
                }
            }
        } else {
            tokenHasEncrypted = false;
            String originToken = token;
            token = token.substring(0, token.lastIndexOf("/"));
            tenant = originToken.substring(originToken.lastIndexOf("/") + 1);
            Enumeration<String> paraNames = request.getParameterNames();
            while (paraNames.hasMoreElements()) {
                String p = paraNames.nextElement();
                String[] vs = request.getParameterValues(p);
                if (vs.length > 1) {
                    paramObj.put(p, vs);
                } else {
                    paramObj.put(p, request.getParameter(p));
                }
            }
        }
        if (TenantUtil.hasTenant(tenant)) {
            TenantContext.init();
            TenantContext.get().switchTenant(tenant);
            UserContext.init(SystemUser.ANONYMOUS.getUserVo(), SystemUser.ANONYMOUS.getTimezone(), request, response);
        }
        JSONObject returnObj = new JSONObject();
        try {
            doIt(request, response, token, tokenHasEncrypted, ApiVo.Type.BINARY, paramObj, returnObj, "doservice");
        } catch (ApiRuntimeException ex) {
            response.setStatus(520);
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ex.getMessage());
        }catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            response.setStatus(520);
            returnObj.put("Status", "ERROR");
            returnObj.put("Message", ExceptionUtils.getStackFrames(ex));
        }
        if (!response.isCommitted()) {
            response.setContentType(Config.RESPONSE_TYPE_JSON);
            response.getWriter().print(returnObj.toJSONString());
        }
    }
}
