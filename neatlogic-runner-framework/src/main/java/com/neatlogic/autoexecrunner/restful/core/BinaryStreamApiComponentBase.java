package com.neatlogic.autoexecrunner.restful.core;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import com.neatlogic.autoexecrunner.dto.ApiVo;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.aop.framework.AopContext;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.ClassUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

public abstract class BinaryStreamApiComponentBase extends ApiValidateAndHelpBase implements MyBinaryStreamApiComponent {
    // private static Logger logger =
    // LoggerFactory.getLogger(BinaryStreamApiComponentBase.class);


    @Override
    public final Object doService(ApiVo apiVo, JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String error = "";
        Object result = null;
        long startTime = System.currentTimeMillis();
        try {
            try {
                Object proxy = AopContext.currentProxy();
                Class<?> targetClass = AopUtils.getTargetClass(proxy);
                validApi(targetClass, paramObj, JSONObject.class, HttpServletRequest.class, HttpServletResponse.class);
                Method method = proxy.getClass().getMethod("myDoService", JSONObject.class, HttpServletRequest.class, HttpServletResponse.class);
                result = method.invoke(proxy, paramObj, request, response);
            } catch (IllegalStateException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException ex) {
                validApi(this.getClass(), paramObj, JSONObject.class, HttpServletRequest.class, HttpServletResponse.class);
                result = myDoService(paramObj, request, response);
            } catch (Exception ex) {
                if (ex.getCause() != null && ex.getCause() instanceof ApiRuntimeException) {
                    throw new ApiRuntimeException(ex.getCause().getMessage());
                } else {
                    throw ex;
                }
            }
        } catch (Exception e) {
            error = e.getMessage() == null ? ExceptionUtils.getStackTrace(e) : e.getMessage();
            throw e;
        } finally {
        }
        return result;
    }

    public final String getId() {
        return ClassUtils.getUserClass(this.getClass()).getName();
    }

    @Override
    public final JSONObject help() {
        return getApiComponentHelp(JSONObject.class, HttpServletRequest.class, HttpServletResponse.class);
    }

}
