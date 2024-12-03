package com.neatlogic.autoexecrunner.startup;

import org.apache.commons.collections4.MapUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class StartUpFactory implements ApplicationRunner {
    private static final Map<String, IStartUp> handlerMap = new HashMap<>();
    static Logger logger = LoggerFactory.getLogger(StartUpFactory.class);

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
    public void run(ApplicationArguments args) {
        if (MapUtils.isNotEmpty(handlerMap)) {
            for (Map.Entry<String, IStartUp> entry : handlerMap.entrySet()) {
                try {
                    entry.getValue().doService();
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex); //异常不影响runner服务启动
                }
            }
        }
    }
}
