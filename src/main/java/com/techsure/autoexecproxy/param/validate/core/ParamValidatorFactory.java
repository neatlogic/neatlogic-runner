package com.techsure.autoexecproxy.param.validate.core;

import com.techsure.autoexecproxy.constvalue.ApiParamType;
import org.reflections.Reflections;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ParamValidatorFactory {
    private static final Map<ApiParamType, ApiParamValidatorBase> authParamMap = new HashMap<>();

    static {
        Reflections reflections = new Reflections("com.techsure");
        Set<Class<? extends ApiParamValidatorBase>> authClass = reflections.getSubTypesOf(ApiParamValidatorBase.class);
        for (Class<? extends ApiParamValidatorBase> c : authClass) {
            try {
                ApiParamValidatorBase authIns = c.newInstance();
                authParamMap.put(authIns.getType(), authIns);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public static ApiParamValidatorBase getAuthInstance(ApiParamType authParamType) {
        return authParamMap.get(authParamType);
    }
}
