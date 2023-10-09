package com.neatlogic.autoexecrunner.startup;

import org.apache.commons.collections4.MapUtils;
import org.reflections.Reflections;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class StartUpFactory implements ApplicationRunner {
    private static final Map<String, IStartUp> handlerMap = new HashMap<>();

    static {
        Reflections reflections = new Reflections("com.neatlogic.autoexecrunner.");
        Set<Class<? extends IStartUp>> startUps = reflections.getSubTypesOf(IStartUp.class);
        for (Class<? extends IStartUp> c : startUps) {
            IStartUp handler;
            try {
                handler = c.newInstance();
                handlerMap.put(handler.getName(), handler);
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (MapUtils.isNotEmpty(handlerMap)) {
            for (Map.Entry<String, IStartUp> entry : handlerMap.entrySet()) {
                entry.getValue().doService();
            }
        }
    }
}
