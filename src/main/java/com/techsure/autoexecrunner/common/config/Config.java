package com.techsure.autoexecrunner.common.config;

import com.alibaba.nacos.api.annotation.NacosInjected;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.techsure.autoexecrunner.common.RootConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
    private static String JWT_SECRET = "techsure#neatlogic$secret";
    private static String AUTOEXEC_HOME;//脚本目录
    private static String AUTH_TYPE;//autoexecrunner的认证方式
    private static String ACCESS_KEY;//访问用户
    private static String ACCESS_SECRET;//访问密码
    private static Long LOGTAIL_BUFLEN;//日志tail buff长度
    private static String WARN_PATTERN;//告警提示关键字
    private static String DATA_HOME;//文件根目录
    private static String DEPLOY_HOME;//发布目录
    private static String GITLAB_PASSWORD;// gitlab private_token

    //neatlogic
    private static String NEATLOGIC_ROOT;

    public static String JWT_SECRET() {
        return JWT_SECRET;
    }

    public static String AUTOEXEC_HOME() {
        return AUTOEXEC_HOME;
    }

    public static String AUTH_TYPE() {
        return AUTH_TYPE;
    }

    public static String NEATLOGIC_ROOT() {
        return NEATLOGIC_ROOT;
    }

    public static String ACCESS_KEY() {
        return ACCESS_KEY;
    }

    public static String ACCESS_SECRET() {
        return ACCESS_SECRET;
    }

    public static Long LOGTAIL_BUFLEN() {
        return LOGTAIL_BUFLEN;
    }

    public static String WARN_PATTERN() {
        return WARN_PATTERN;
    }

    public static String DATA_HOME() {
        return DATA_HOME;
    }

    public static String DEPLOY_HOME() {
        return DEPLOY_HOME;
    }

    public static String GITLAB_PASSWORD() {
        return GITLAB_PASSWORD;
    }

    @PostConstruct
    public void init() {
        try {
            String propertiesString = configService.getConfig("config", "com.techsure", 3000);
            loadNacosProperties(propertiesString);
            configService.addListener("config", "com.techsure", new Listener() {
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
            AUTOEXEC_HOME = prop.getProperty("autoexec.home");
            if (StringUtils.isBlank(AUTOEXEC_HOME)) {
                logger.error("请在配置文件中定义autoexec.home参数");
            }
            DEPLOY_HOME = prop.getProperty("deploy.home");
            JWT_SECRET = prop.getProperty("jwt.secret", "neatlogic#neatlogic$secret");
            NEATLOGIC_ROOT = prop.getProperty("neatlogic.root", "http://localhost:8083/neatlogic");
            AUTH_TYPE = prop.getProperty("auth.type", "");
            ACCESS_KEY = prop.getProperty("access.key", "admin");
            ACCESS_SECRET = prop.getProperty("access.secret", "password");
            WARN_PATTERN = prop.getProperty("warn.pattern", "warn:");
            LOGTAIL_BUFLEN = Long.valueOf(prop.getProperty("logtail.buflen", String.valueOf(64 * 1024)));
            DATA_HOME = prop.getProperty("data.home", "/home/autoexec-runner/data");
            GITLAB_PASSWORD = prop.getProperty("gitlab.password");
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
