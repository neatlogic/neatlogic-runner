package com.neatlogic.autoexecrunner.common.config;

import com.neatlogic.autoexecrunner.common.RootConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

@RootConfiguration
public class Config {
    @Resource
    private ConfigurableApplicationContext configurableApplicationContext;

    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    private static final String CONFIG_FILE = "application.properties";
    public static final String RESPONSE_TYPE_JSON = "application/json;charset=UTF-8";
    private static String JWT_SECRET = "neatlogic#neatlogic$secret";
    private static String AUTOEXEC_HOME;//脚本目录

    private static Integer SERVER_PORT;//服务端口
    private static Long LOGTAIL_BUFLEN;//日志tail buff长度
    private static String WARN_PATTERN;//告警提示关键字
    private static String DATA_HOME;//文件根目录
    private static String DEPLOY_HOME;//发布目录
    private static String GITLAB_PASSWORD;// gitlab private_token
    private static Integer SUBPROCESS_COMMAND_QUEUE_MAX_SIZE;//自动化作业命令等待队列的最大容量，超过此数量的新命令将被拒绝或丢弃
    private static Integer SUBPROCESS_EXECUTION_MAX_CONCURRENT;//自动化作业最大并发子进程数，超过此数量的命令将进入等待队列
    //neatlogic
    private static String NEATLOGIC_ROOT;

    private static Integer INFORMANT_HEARTBEAT_INTERVAL;//单位是秒，超过心跳周期没收到心跳则会发送下线请求


    /**
     * 连接超时时间（用来建立连接的时间。如果到了指定的时间，还没建立连接，则报异常。单位：秒，0代表不限制）
     */
    public static Integer CONNECTION_CONNECT_TIMEOUT = 0;
    /**
     * 读超时时间（已经建立连接，并开始读取服务端资源。如果到了指定的时间，没有可能的数据被客户端读取，则报异常。单位：秒，0代表不限制）
     */
    public static Integer CONNECTION_READ_TIMEOUT = 0;

    public static Boolean IS_SSL;

    //启动服务需要注册runner的租户，以逗号隔开
    public static String REGISTER_TENANTS;

    public static Integer SERVER_ID;

    private static String AUTOEXEC_TOKEN;// autoexec用户的token

    private static Integer UPDATE_RUNNER_STATUS_PERIOD;// runner推送到neatlogic的间隔时间,半个小时执行一次

    public static Properties properties = new Properties();

    public static String JWT_SECRET() {
        return JWT_SECRET;
    }

    public static String AUTOEXEC_HOME() {
        return AUTOEXEC_HOME;
    }

    public static Integer SERVER_PORT() {
        return SERVER_PORT;
    }

    public static Integer UPDATE_RUNNER_STATUS_PERIOD() {
        return UPDATE_RUNNER_STATUS_PERIOD;
    }

    public static String NEATLOGIC_ROOT() {
        return NEATLOGIC_ROOT;
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

    public static Boolean IS_SSL() {
        return IS_SSL;
    }

    public static String REGISTER_TENANTS() {
        return REGISTER_TENANTS;
    }

    public static String AUTOEXEC_TOKEN() {
        return AUTOEXEC_TOKEN;
    }

    public static Integer INFORMANT_HEARTBEAT_INTERVAL() {
        return INFORMANT_HEARTBEAT_INTERVAL;
    }

    public static Integer SUBPROCESS_COMMAND_QUEUE_MAX_SIZE() {
        return SUBPROCESS_COMMAND_QUEUE_MAX_SIZE;
    }

    public static Integer SUBPROCESS_EXECUTION_MAX_CONCURRENT() {
        return SUBPROCESS_EXECUTION_MAX_CONCURRENT;
    }


    public Config() {
        try {
            Properties prop = new Properties();
            prop.load(new InputStreamReader(Objects.requireNonNull(Config.class.getClassLoader().getResourceAsStream(CONFIG_FILE)), StandardCharsets.UTF_8));
            SERVER_PORT = Integer.parseInt(prop.getProperty("server.port", "8084"));
            AUTOEXEC_HOME = prop.getProperty("autoexec.home");
            if (StringUtils.isBlank(AUTOEXEC_HOME)) {
                logger.error("请在配置文件中定义autoexec.home参数");
            }
            DEPLOY_HOME = prop.getProperty("deploy.home");
            JWT_SECRET = prop.getProperty("jwt.secret");
            if (StringUtils.isBlank(JWT_SECRET)) {
                System.out.println("  ✖application.properties请定义jwt密钥:jwt.secret,注意需和neatlogic配置的保持一致");
                logger.error("  ✖application.properties请定义jwt密钥:jwt.secret,注意需和neatlogic配置的保持一致");
                int exitCode = SpringApplication.exit(configurableApplicationContext);
                System.exit(exitCode);
            }
            NEATLOGIC_ROOT = prop.getProperty("neatlogic.root", "http://localhost:8083/neatlogic");
            WARN_PATTERN = prop.getProperty("warn.pattern", "warn:");
            LOGTAIL_BUFLEN = Long.valueOf(prop.getProperty("logtail.buflen", String.valueOf(64 * 1024)));
            DATA_HOME = prop.getProperty("data.home", "/app/autoexec/");
            GITLAB_PASSWORD = prop.getProperty("gitlab.password");

            CONNECTION_CONNECT_TIMEOUT = Integer.parseInt(prop.getProperty("net.connection.connecttimeout", "0"));
            CONNECTION_READ_TIMEOUT = Integer.parseInt(prop.getProperty("net.connection.readtimeout", "0"));

            IS_SSL = Boolean.valueOf(prop.getProperty("server.ssl.enabled", "false"));

            REGISTER_TENANTS = prop.getProperty("register.tenants");

            SERVER_ID = Integer.parseInt(prop.getProperty("server.id", "1"));

            AUTOEXEC_TOKEN = prop.getProperty("autoexec.token", "499922b4317c251c2ce525f7b83e3d94");

            UPDATE_RUNNER_STATUS_PERIOD = Integer.parseInt(prop.getProperty("update.runner.status.period", "1800000"));

            SUBPROCESS_COMMAND_QUEUE_MAX_SIZE = Integer.parseInt(prop.getProperty("subprocess.command.queue.max-size", "1000"));

            SUBPROCESS_EXECUTION_MAX_CONCURRENT = Integer.parseInt(prop.getProperty("subprocess.execution.max-concurrent", "20"));

            INFORMANT_HEARTBEAT_INTERVAL = Integer.parseInt(prop.getProperty("informant.heartbeat.interval", "300"));

            properties = prop;
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

    }

    public static String getConfigProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

}
