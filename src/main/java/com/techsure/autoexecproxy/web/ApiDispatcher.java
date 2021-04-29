package com.techsure.autoexecproxy.web;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONReader;
import com.techsure.autoexecproxy.common.config.Config;
import com.techsure.autoexecproxy.dto.ApiHandlerVo;
import com.techsure.autoexecproxy.dto.ApiVo;
import com.techsure.autoexecproxy.exception.ApiNotFoundException;
import com.techsure.autoexecproxy.exception.AuthenticateException;
import com.techsure.autoexecproxy.exception.ComponentNotFoundException;
import com.techsure.autoexecproxy.exception.core.ApiRuntimeException;
import com.techsure.autoexecproxy.restful.core.IApiComponent;
import com.techsure.autoexecproxy.restful.core.IBinaryStreamApiComponent;
import com.techsure.autoexecproxy.restful.core.IJsonStreamApiComponent;
import com.techsure.autoexecproxy.restful.core.privateapi.PrivateApiComponentFactory;
import com.techsure.autoexecproxy.web.core.ApiAuthFactory;
import com.techsure.autoexecproxy.web.core.IApiAuth;
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
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/api/")
public class ApiDispatcher {
    /**
     * 错误码定义（和codedriver一致）：
     * 520:已知接口错误，error.log不再打出堆栈信息，只会把错误信息返回给前端
     * 521:不明错误，会在error.log中打出
     * 522:用户认证问题
     **/
    Logger logger = LoggerFactory.getLogger(ApiDispatcher.class);
    private static final String RESPONSE_TYPE_JSON = "application/json;charset=UTF-8";
    private static final Map<Integer, String> errorMap = new HashMap<>();

    public ApiDispatcher() {
        errorMap.put(522, "访问认证失败");
    }


    private JSON doIt(HttpServletRequest request, HttpServletResponse response, String token, ApiVo.Type apiType, JSONObject paramObj, String action) throws Exception {
        ApiVo interfaceVo = PrivateApiComponentFactory.getApiByToken(token);

        if (interfaceVo == null) {
            throw new ApiNotFoundException("token为“" + token + "”的接口不存在或已被禁用");
        } else if (interfaceVo.getPathVariableObj() != null) {
            // 融合路径参数
            paramObj.putAll(interfaceVo.getPathVariableObj());
        }

        ApiHandlerVo apiHandlerVo = PrivateApiComponentFactory.getApiHandlerByHandler(interfaceVo.getHandler());
        if (apiHandlerVo == null) {
            throw new ComponentNotFoundException("接口组件:" + interfaceVo.getHandler() + "不存在");
        }

        /*认证，如果是查看帮助接口，则不需要认证*/
        String uri = request.getRequestURI();
        if (!(uri.contains("/api/help/") && !token.contains("/api/help/"))) {
            IApiAuth apiAuth = ApiAuthFactory.getApiAuth(Config.AUTH_TYPE());
            if (apiAuth != null) {
                int result = apiAuth.auth(interfaceVo, paramObj, request);
                if (result != 1) {
                    throw new AuthenticateException(errorMap.get(result));
                }
            }
        }

        //param补充 tenant 租户信息
        if(StringUtils.isNotBlank(request.getHeader("Tenant"))){
            paramObj.put("tenant",request.getHeader("Tenant"));
        }

        if (apiType.equals(ApiVo.Type.OBJECT)) {
            IApiComponent restComponent = PrivateApiComponentFactory.getInstance(interfaceVo.getHandler());
            if (restComponent != null) {
                if (action.equals("doservice")) {
                    Long starttime = System.currentTimeMillis();
                    Object returnV = restComponent.doService(interfaceVo, paramObj);
                    Long endtime = System.currentTimeMillis();
                    if (!restComponent.isRaw()) {
                        JSONObject returnObj = new JSONObject();
                        returnObj.put("TimeCost", endtime - starttime);
                        returnObj.put("Return", returnV);
                        returnObj.put("Status", "OK");
                        return returnObj;
                    } else {
                        Object o = JSON.parse(JSON.toJSONString(returnV));
                        if (o instanceof JSONObject) {
                            return (JSONObject) o;
                        } else {
                            return (JSONArray) o;
                        }
                    }
                } else {
                    return restComponent.help();
                }
            } else {
                throw new ComponentNotFoundException("接口组件:" + interfaceVo.getHandler() + "不存在");
            }
        } else if (apiType.equals(ApiVo.Type.STREAM)) {
            IJsonStreamApiComponent restComponent = PrivateApiComponentFactory.getStreamInstance(interfaceVo.getHandler());
            if (restComponent != null) {
                if (action.equals("doservice")) {
                    Long starttime = System.currentTimeMillis();
                    Object returnV = restComponent.doService(interfaceVo, paramObj, new JSONReader(new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8)));
                    Long endtime = System.currentTimeMillis();
                    if (!restComponent.isRaw()) {
                        JSONObject returnObj = new JSONObject();
                        returnObj.put("TimeCost", endtime - starttime);
                        returnObj.put("Return", returnV);
                        returnObj.put("Status", "OK");
                        return returnObj;
                    } else {
                        Object o = JSON.parse(JSON.toJSONString(returnV));
                        if (o instanceof JSONObject) {
                            return (JSONObject) o;
                        } else {
                            return (JSONArray) o;
                        }
                    }
                } else {
                    return restComponent.help();
                }
            } else {
                throw new ComponentNotFoundException("接口组件:" + interfaceVo.getHandler() + "不存在");
            }
        } else if (apiType.equals(ApiVo.Type.BINARY)) {
            IBinaryStreamApiComponent restComponent = PrivateApiComponentFactory.getBinaryInstance(interfaceVo.getHandler());
            if (restComponent != null) {
                if (action.equals("doservice")) {
                    Long starttime = System.currentTimeMillis();
                    Object returnV = restComponent.doService(interfaceVo, paramObj, request, response);
                    Long endtime = System.currentTimeMillis();
                    if (!restComponent.isRaw()) {
                        JSONObject returnObj = new JSONObject();
                        returnObj.put("TimeCost", endtime - starttime);
                        returnObj.put("Return", returnV);
                        returnObj.put("Status", "OK");
                        return returnObj;
                    } else {
                        Object o = JSON.parse(JSON.toJSONString(returnV));
                        if (o instanceof JSONObject) {
                            return (JSONObject) o;
                        } else {
                            return (JSONArray) o;
                        }
                    }
                } else {
                    return restComponent.help();
                }
            } else {
                throw new ComponentNotFoundException("接口组件:" + interfaceVo.getHandler() + "不存在");
            }
        }
        return new JSONObject();
    }

    @RequestMapping(value = "/rest/**", method = RequestMethod.GET)
    public void dispatcherForGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String token = new AntPathMatcher().extractPathWithinPattern(pattern, request.getServletPath());

        JSONObject paramObj = new JSONObject();
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
        JSON returnObj;
        try {
            returnObj = doIt(request, response, token, ApiVo.Type.OBJECT, paramObj, "doservice");
        } catch (ApiRuntimeException ex) {
            response.setStatus(520);
            JSONObject rObj = new JSONObject();
            rObj.put("Status", "ERROR");
            rObj.put("Message", ex.getMessage());
            returnObj = rObj;
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            response.setStatus(521);
            JSONObject rObj = new JSONObject();
            rObj.put("Status", "ERROR");
            rObj.put("Message", ExceptionUtils.getStackFrames(ex));
            returnObj = rObj;
        }
        if (!response.isCommitted()) {
            response.setContentType(RESPONSE_TYPE_JSON);
            response.getWriter().print(returnObj);
        }
    }

    @RequestMapping(value = "/rest/**", method = RequestMethod.POST)
    public void dispatcherForPost(@RequestBody String jsonStr, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String token = new AntPathMatcher().extractPathWithinPattern(pattern, request.getServletPath());
        JSON returnObj;
        try {
            JSONObject paramObj;
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

            returnObj = doIt(request, response, token, ApiVo.Type.OBJECT, paramObj, "doservice");
        } catch (ApiRuntimeException ex) {
            response.setStatus(520);
            JSONObject rObj = new JSONObject();
            rObj.put("Status", "ERROR");
            rObj.put("Message", ex.getMessage());
            returnObj = rObj;
        } catch (Exception ex) {
            response.setStatus(521);
            JSONObject rObj = new JSONObject();
            rObj.put("Status", "ERROR");
            rObj.put("Message", ExceptionUtils.getStackTrace(ex));
            returnObj = rObj;
            logger.error(ex.getMessage(), ex);
        }
        if (!response.isCommitted()) {
            response.setContentType(RESPONSE_TYPE_JSON);
            response.getWriter().print(returnObj);
        }
    }

    @RequestMapping(value = "/stream/**", method = RequestMethod.POST)
    public void dispatcherForPostStream(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String token = new AntPathMatcher().extractPathWithinPattern(pattern, request.getServletPath());

        JSONObject paramObj = new JSONObject();
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
        JSON returnObj;
        try {
            returnObj = doIt(request, response, token, ApiVo.Type.STREAM, paramObj, "doservice");
        } catch (ApiRuntimeException ex) {
            response.setStatus(520);
            JSONObject rObj = new JSONObject();
            rObj.put("Status", "ERROR");
            rObj.put("Message", ex.getMessage());
            returnObj = rObj;
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            response.setStatus(521);
            JSONObject rObj = new JSONObject();
            rObj.put("Status", "ERROR");
            rObj.put("Message", ExceptionUtils.getStackFrames(ex));
            returnObj = rObj;
        }
        if (!response.isCommitted()) {
            response.setContentType(RESPONSE_TYPE_JSON);
            response.getWriter().print(returnObj);
        }
    }

    @RequestMapping(value = "/binary/**", method = RequestMethod.GET)
    public void dispatcherForPostBinary(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String token = new AntPathMatcher().extractPathWithinPattern(pattern, request.getServletPath());

        JSONObject paramObj = new JSONObject();

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
        JSON returnObj;
        try {
            returnObj = doIt(request, response, token, ApiVo.Type.BINARY, paramObj, "doservice");
        } catch (ApiRuntimeException ex) {
            response.setStatus(520);
            JSONObject rObj = new JSONObject();
            rObj.put("Status", "ERROR");
            rObj.put("Message", ex.getMessage());
            returnObj = rObj;
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            response.setStatus(521);
            JSONObject rObj = new JSONObject();
            rObj.put("Status", "ERROR");
            rObj.put("Message", ExceptionUtils.getStackFrames(ex));
            returnObj = rObj;
        }
        if (!response.isCommitted()) {
            response.setContentType(RESPONSE_TYPE_JSON);
            response.getWriter().print(returnObj);
        }
    }

    @RequestMapping(value = "/binary/**", method = RequestMethod.POST, consumes = "application/json")
    public void dispatcherForPostBinaryJson(@RequestBody String jsonStr, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String token = new AntPathMatcher().extractPathWithinPattern(pattern, request.getServletPath());

        JSONObject paramObj;
        if (StringUtils.isNotBlank(jsonStr)) {
            try {
                paramObj = JSONObject.parseObject(jsonStr);
            } catch (Exception e) {
                throw new ApiRuntimeException("请求参数需要符合JSON格式");
            }
        } else {
            paramObj = new JSONObject();
        }

        JSON returnObj;
        try {
            returnObj = doIt(request, response, token, ApiVo.Type.BINARY, paramObj, "doservice");
        } catch (ApiRuntimeException ex) {
            response.setStatus(520);
            JSONObject rObj = new JSONObject();
            rObj.put("Status", "ERROR");
            rObj.put("Message", ex.getMessage());
            returnObj = rObj;
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            response.setStatus(521);
            JSONObject rObj = new JSONObject();
            rObj.put("Status", "ERROR");
            rObj.put("Message", ExceptionUtils.getStackFrames(ex));
            returnObj = rObj;
        }
        if (!response.isCommitted()) {
            response.setContentType(RESPONSE_TYPE_JSON);
            response.getWriter().print(returnObj);
        }
    }

    @RequestMapping(value = "/binary/**", method = RequestMethod.POST, consumes = "multipart/form-data")
    public void dispatcherForPostBinaryMultipart(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String token = new AntPathMatcher().extractPathWithinPattern(pattern, request.getServletPath());
        JSONObject paramObj = new JSONObject();

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
        JSON returnObj;
        try {
            returnObj = doIt(request, response, token, ApiVo.Type.BINARY, paramObj, "doservice");
        } catch (ApiRuntimeException ex) {
            response.setStatus(520);
            JSONObject rObj = new JSONObject();
            rObj.put("Status", "ERROR");
            rObj.put("Message", ex.getMessage());
            returnObj = rObj;
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            response.setStatus(521);
            JSONObject rObj = new JSONObject();
            rObj.put("Status", "ERROR");
            rObj.put("Message", ExceptionUtils.getStackFrames(ex));
            returnObj = rObj;
        }
        if (!response.isCommitted()) {
            response.setContentType(RESPONSE_TYPE_JSON);
            response.getWriter().print(returnObj);
        }
    }

    @RequestMapping(value = "/help/rest/**", method = RequestMethod.GET)
    public void resthelp(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String token = new AntPathMatcher().extractPathWithinPattern(pattern, request.getServletPath());

        JSON returnObj;
        try {
            returnObj = doIt(request, response, token, ApiVo.Type.OBJECT, null, "help");
        } catch (ApiRuntimeException ex) {
            response.setStatus(520);
            JSONObject rObj = new JSONObject();
            rObj.put("Status", "ERROR");
            rObj.put("Message", ex.getMessage());
            returnObj = rObj;
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            response.setStatus(521);
            JSONObject rObj = new JSONObject();
            rObj.put("Status", "ERROR");
            rObj.put("Message", ExceptionUtils.getStackFrames(ex));
            returnObj = rObj;
        }
        response.setContentType(RESPONSE_TYPE_JSON);
        response.getWriter().print(returnObj);
    }

    @RequestMapping(value = "/help/stream/**", method = RequestMethod.GET)
    public void streamhelp(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String token = new AntPathMatcher().extractPathWithinPattern(pattern, request.getServletPath());

        JSON returnObj;
        try {
            returnObj = doIt(request, response, token, ApiVo.Type.STREAM, null, "help");
        } catch (ApiRuntimeException ex) {
            response.setStatus(520);
            JSONObject rObj = new JSONObject();
            rObj.put("Status", "ERROR");
            rObj.put("Message", ex.getMessage());
            returnObj = rObj;
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            response.setStatus(521);
            JSONObject rObj = new JSONObject();
            rObj.put("Status", "ERROR");
            rObj.put("Message", ExceptionUtils.getStackFrames(ex));
            returnObj = rObj;
        }
        response.setContentType(RESPONSE_TYPE_JSON);
        response.getWriter().print(returnObj);
    }

    @RequestMapping(value = "/help/binary/**", method = RequestMethod.GET)
    public void binaryhelp(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String token = new AntPathMatcher().extractPathWithinPattern(pattern, request.getServletPath());

        JSON returnObj;
        try {
            returnObj = doIt(request, response, token, ApiVo.Type.BINARY, null, "help");
        } catch (ApiRuntimeException ex) {
            response.setStatus(520);
            JSONObject rObj = new JSONObject();
            rObj.put("Status", "ERROR");
            rObj.put("Message", ex.getMessage());
            returnObj = rObj;
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            response.setStatus(521);
            JSONObject rObj = new JSONObject();
            rObj.put("Status", "ERROR");
            rObj.put("Message", ExceptionUtils.getStackFrames(ex));
            returnObj = rObj;
        }
        response.setContentType(RESPONSE_TYPE_JSON);
        response.getWriter().print(returnObj);
    }

}
