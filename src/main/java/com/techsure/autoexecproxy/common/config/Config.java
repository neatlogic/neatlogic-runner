package com.techsure.autoexecproxy.common.config;

import com.alibaba.nacos.api.annotation.NacosInjected;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.techsure.autoexecproxy.common.RootConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Executor;

@RootConfiguration
public class Config {
    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    @NacosInjected
    private ConfigService configService;
    private static final String CONFIG_FILE = "config.properties";

    public static final String RC4KEY = "codedriver.key.20200101";

    private static String JWT_SECRET = "techsure#codedriver$secret";
    private static String DATA_HOME;// 存储文件路径
    private static String OCTOPUS_HOME_URL;//octopus的链接地址，callback时需要使用
    private static String AUTH_TYPE;//autoexecproxy的认证方式
    private static String ACCESS_KEY;//访问用户
    private static String ACCESS_SECRET;//访问密码


    static {

    }

    public static String OCTOPUS_HOME_URL() {
        return OCTOPUS_HOME_URL;
    }

    public static String JWT_SECRET() {
        return JWT_SECRET;
    }


    public static String AUTH_TYPE() {
        return AUTH_TYPE;
    }

    public static String ACCESS_KEY() {
        return ACCESS_KEY;
    }

    public static String ACCESS_SECRET() {
        return ACCESS_SECRET;
    }


    public static String DATA_HOME() {
        if (!DATA_HOME.endsWith(File.separator)) {
            DATA_HOME += File.separator;
        }
        return DATA_HOME;
    }


    @PostConstruct
    public void init() {
        try {
            String propertiesString = configService.getConfig("config", "codedriver.autoexecproxy", 3000);
            loadNacosProperties(propertiesString);
            configService.addListener("config", "codedriver.autoexecproxy", new Listener() {
                @Override
                public void receiveConfigInfo(String configInfo) {
                    loadNacosProperties(configInfo);
                }

                @Override
                public Executor getExecutor() {
                    return null;
                }
            });
        } catch (NacosException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static void loadNacosProperties(String configInfo) {
        try {
            Properties prop = new Properties();
            if (StringUtils.isNotBlank(configInfo)) {
                prop.load(new ByteArrayInputStream(configInfo.getBytes()));
            } else {
                // 如果从nacos中读不出配置，则使用本地配置文件配置
                prop.load(new InputStreamReader(Objects.requireNonNull(Config.class.getClassLoader().getResourceAsStream(CONFIG_FILE)), StandardCharsets.UTF_8));
            }
            DATA_HOME = prop.getProperty("data.home", "/app/data");
            OCTOPUS_HOME_URL = prop.getProperty("octopus.home.url");
            JWT_SECRET = prop.getProperty("jwt.secret", "techsure#codedriver$secret");
            AUTH_TYPE = prop.getProperty("auth.type", "basic");
            ACCESS_KEY = prop.getProperty("access.key", "admin");
            ACCESS_SECRET = prop.getProperty("access.secret", "password");
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static String getProperty(String configFile, String keyName, String defaultValue, boolean isRequired) {
        Properties pro = new Properties();
        try (InputStream is = Config.class.getClassLoader().getResourceAsStream(configFile)) {
            pro.load(is);
            String value = pro.getProperty(keyName, defaultValue);
            if (value != null) {
                value = value.trim();
            }
            return value;
        } catch (Exception e) {
            if (isRequired) {
                logger.error(e.getMessage(), e);
            }
        }
        return "";
    }

    public static String getProperty(String configFile, String keyName) {
        return getProperty(configFile, keyName, "", false);
    }

    public static String getProperty(String configFile, String keyName, boolean isRequired) {
        return getProperty(configFile, keyName, "", isRequired);
    }


}
