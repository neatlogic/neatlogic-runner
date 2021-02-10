package com.techsure.autoexecproxy.restful.core;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecproxy.dto.ApiVo;
import com.techsure.autoexecproxy.exception.core.ApiRuntimeException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.aop.framework.AopContext;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class ApiComponentBase extends ApiValidateAndHelpBase implements MyApiComponent {


    public final Object doService(ApiVo apiVo, JSONObject paramObj) throws Exception {
        String error = "";
        Object result = null;
        long startTime = System.currentTimeMillis();
        try {
            try {
                Object proxy = AopContext.currentProxy();
                Class<?> targetClass = AopUtils.getTargetClass(proxy);
                validApi(targetClass, paramObj, JSONObject.class);
                Method method = proxy.getClass().getMethod("myDoService", JSONObject.class);
                result = method.invoke(proxy, paramObj);
            } catch (IllegalStateException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | SecurityException ex) {
                validApi(this.getClass(), paramObj, JSONObject.class);
                result = myDoService(paramObj);
            } catch (Exception ex) {
                if (ex.getCause() != null && ex.getCause() instanceof ApiRuntimeException) {
                    throw new ApiRuntimeException(ex.getCause().getMessage());
                } else {
                    throw ex;
                }
            }
        } catch (Exception e) {
            Throwable target = e;
            //如果是反射抛得异常，则需要拆包，把真实得异常类找出来
            while (target instanceof InvocationTargetException) {
                target = ((InvocationTargetException) target).getTargetException();
            }
            error = e.getMessage() == null ? ExceptionUtils.getStackTrace(e) : e.getMessage();
            throw (Exception) target;
        }
        return result;
    }

    @Override
    public final JSONObject help() {
        return getApiComponentHelp(JSONObject.class);
    }

}
