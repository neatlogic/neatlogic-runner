package com.techsure.autoexecrunner.common.config;

import com.techsure.autoexecrunner.util.RC4Util;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class LocalConfig implements BeanFactoryPostProcessor, EnvironmentAware, PriorityOrdered {
    static Logger logger = LoggerFactory.getLogger(LocalConfig.class);
    private static final String CONFIG_FILE = "application.properties";
    private Properties properties;
    private ConfigurableEnvironment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }

    private synchronized String getProperty(String keyName, String defaultValue) {
        if (properties == null) {
            properties = new Properties();
            try (InputStreamReader is = new InputStreamReader(Config.class.getClassLoader().getResourceAsStream(CONFIG_FILE), StandardCharsets.UTF_8)) {
                properties.load(is);
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error("load " + CONFIG_FILE + " error: " + ex.getMessage(), ex);
            }
        }
        String value = "";
        if (properties != null) {
            value = properties.getProperty(keyName, defaultValue);
            if (StringUtils.isNotBlank(value)) {
                value = value.trim();
            }
        }

        return value;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        MutablePropertySources propertySources = environment.getPropertySources();
        Map<String, Object> paramMap = new HashMap<>();
        //如果有需要在xml中加载的配置，可以在下面设置
        String mongoHost = this.getProperty("mongo.host", "localhost:27017");
        String mongoUser = this.getProperty("mongo.username", "root");
        String mongoPwd = this.getProperty("mongo.password", "root");
        String mongoDb = this.getProperty("mongo.database", "admin");
        if (mongoPwd.startsWith("RC4:")) {
            mongoPwd = RC4Util.decrypt(mongoPwd.substring(4));
        }
        paramMap.put("mongo.url", "mongodb://" + mongoUser + ":" + mongoPwd + "@" + mongoHost + "/" + mongoDb);
        propertySources.addLast(new MapPropertySource("localconfig", paramMap));
    }

    @Override
    public int getOrder() {
        return PriorityOrdered.HIGHEST_PRECEDENCE;
    }

}
