package com.techsure.autoexecrunner.common.config;

import com.alibaba.nacos.api.annotation.NacosInjected;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.techsure.autoexecrunner.common.RootConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Executor;

@RootConfiguration
public class Config {
    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    @NacosInjected
    private ConfigService configService;
    private static final String CONFIG_FILE = "application.properties";
    public static final String RESPONSE_TYPE_JSON = "application/json;charset=UTF-8";

    public static final String RC4KEY = "codedriver.key.20200101";

    private static String JWT_SECRET = "techsure#codedriver$secret";
    private static String AUTOEXEC_HOME;//脚本目录
    private static String DATA_HOME;// 存储文件路径
    private static String CALLBACK_URL;//octopus的链接地址，callback时需要使用
    private static String AUTH_TYPE;//autoexecrunner的认证方式
    private static String ACCESS_KEY;//访问用户
    private static String ACCESS_SECRET;//访问密码
    private static String PARAM_PATH;//参数保存路径
    private static String LOG_PATH;//执行日志路径
    private static Long LOGTAIL_BUFLEN;//日志tail buff长度
    private static String WARN_PATTERN;//告警提示关键字
    //mongodb
    private static String MONGODB_HOST;
    private static Integer MONGODB_PORT;
    private static String MONGODB_DEFAULT_DATABASE;


    public static String CALLBACK_URL() {
        return CALLBACK_URL;
    }

    public static String JWT_SECRET() {
        return JWT_SECRET;
    }

    public static String AUTOEXEC_HOME() {
        return AUTOEXEC_HOME;
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
    public static String PARAM_PATH() {
        return PARAM_PATH;
    }
    public static String LOG_PATH() {
        return LOG_PATH;
    }
    public static Long LOGTAIL_BUFLEN() {
        return LOGTAIL_BUFLEN;
    }
    public static String WARN_PATTERN() {
        return WARN_PATTERN;
    }

    public static String DATA_HOME() {
        if (!DATA_HOME.endsWith(File.separator)) {
            DATA_HOME += File.separator;
        }
        return DATA_HOME;
    }
    public static String MONGODB_HOST() {
        return MONGODB_HOST;
    }
    public static Integer MONGODB_PORT() {
        return MONGODB_PORT;
    }
    public static String MONGODB_DEFAULT_DATABASE() {
        return MONGODB_DEFAULT_DATABASE;
    }

    @PostConstruct
    public void init() {
        try {
            String propertiesString = configService.getConfig("config", "codedriver.autoexecrunner", 3000);
            loadNacosProperties(propertiesString);
            configService.addListener("config", "codedriver.autoexecrunner", new Listener() {
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
            AUTOEXEC_HOME = prop.getProperty("autoexec.home");
            if (StringUtils.isBlank(AUTOEXEC_HOME)) {
                logger.error("请在配置文件中定义autoexec.home参数");
            }
            CALLBACK_URL = prop.getProperty("callback.url");
            JWT_SECRET = prop.getProperty("jwt.secret", "techsure#codedriver$secret");
            AUTH_TYPE = prop.getProperty("auth.type", "");
            ACCESS_KEY = prop.getProperty("access.key", "admin");
            ACCESS_SECRET = prop.getProperty("access.secret", "password");
            PARAM_PATH = prop.getProperty("param.path", "/data/job");
            LOG_PATH = prop.getProperty("log.path", "/data/job");
            WARN_PATTERN = prop.getProperty("warn.pattern", "warn:");
            LOGTAIL_BUFLEN = Long.valueOf(prop.getProperty("logtail.buflen", String.valueOf(32 * 1024)));
            MONGODB_HOST = prop.getProperty("spring.data.mongodb.host", "localhost");
            MONGODB_PORT = Integer.valueOf(prop.getProperty("spring.data.mongodb.port", "8080"));
            MONGODB_DEFAULT_DATABASE = prop.getProperty("spring.data.mongodb.database", "autoexec");
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
