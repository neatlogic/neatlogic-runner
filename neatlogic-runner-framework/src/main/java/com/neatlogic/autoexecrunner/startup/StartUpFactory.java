package com.neatlogic.autoexecrunner.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StartUpFactory implements ApplicationRunner {
    static Logger logger = LoggerFactory.getLogger(StartUpFactory.class);

    private final List<IStartUp> startUps;

    public StartUpFactory(List<IStartUp> startUps) {
        this.startUps = startUps;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (IStartUp handler : startUps) {
            try {
                handler.doService();
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex); //异常不影响runner服务启动
            }
        }
    }
}


