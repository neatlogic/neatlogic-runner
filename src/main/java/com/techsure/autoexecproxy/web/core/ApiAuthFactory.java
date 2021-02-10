package com.techsure.autoexecproxy.web.core;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ApiAuthFactory implements ApplicationListener<ContextRefreshedEvent> {
    private static final Map<String, IApiAuth> apiAuthMap = new HashMap<>();


    public static IApiAuth getApiAuth(String type) {
        return apiAuthMap.get(type);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ApplicationContext context = event.getApplicationContext();
        Map<String, IApiAuth> myMap = context.getBeansOfType(IApiAuth.class);
        for (Map.Entry<String, IApiAuth> entry : myMap.entrySet()) {
            IApiAuth apiAuth = entry.getValue();
            apiAuthMap.put(apiAuth.getType().getValue(), apiAuth);
        }

    }


}
