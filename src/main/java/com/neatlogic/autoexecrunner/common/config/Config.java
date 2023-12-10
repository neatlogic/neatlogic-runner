package com.neatlogic.autoexecrunner.common.config;

import com.alibaba.nacos.api.annotation.NacosInjected;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.neatlogic.autoexecrunner.common.RootConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimetypesFileTypeMap;
import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.FileNameMap;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

@RootConfiguration
public class Config {
    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    @NacosInjected
    private ConfigService configService;
    private static final String CONFIG_FILE = "application.properties";
    public static final String RESPONSE_TYPE_JSON = "application/json;charset=UTF-8";
    private static String JWT_SECRET = "neatlogic#neatlogic$secret";
    private static String AUTOEXEC_HOME;//脚本目录

    private static Integer SERVER_PORT;//服务端口
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


    //codehub
    public static String NEATLOGIC_HOME;

    public static FileNameMap FILE_NAME_MAP;

    /**
     * working copy 所在目录
     */
    public static String WORKING_COPY_PATH;

    /**
     * 从提交 message 中识别需求的模式
     */
    public static Pattern ISSUE_PATTERN;

    /**
     * 识别多个需求的模式
     */
    public static Pattern MULTIPLE_ISSUE_PATTERN;

    /**
     * 最大扫描提交数量
     */
    public static int MAX_GET_COMMIT_LOG;

    /**
     * 是否启用缓存，默认启用，用于缓存 commit 和 diff
     */
    public static boolean CACHE_ENABLE;

    /**
     * 单个缓存文件的大小，超出此限制则创建新的缓存文件，默认 512 * 1024 byte
     */
    public static int CACHE_MAX_SIZE;

    /**
     * 决定文本文件的后缀，多个用空格分割。文本文件可在线查看内容
     */
    public static String FILE_MIMETYPE_TEXT_PLAIN;

    /**
     * 获取字符串格式的文件内容时，如果文件大小超过该值，则报错。单位为KB，默认 2048
     */
    public static Integer FILE_CONTENT_SHOW_MAX_SIZE = 2048;

    public static MimetypesFileTypeMap FILE_MIME_TYPE_MAP;

    /**
     * 连接超时时间（用来建立连接的时间。如果到了指定的时间，还没建立连接，则报异常。单位：秒，0代表不限制）
     */
    public static Integer CONNECTION_CONNECT_TIMEOUT = 0;
    /**
     * 读超时时间（已经建立连接，并开始读取服务端资源。如果到了指定的时间，没有可能的数据被客户端读取，则报异常。单位：秒，0代表不限制）
     */
    public static Integer CONNECTION_READ_TIMEOUT = 0;

    /**
     * 控制合并操作的并发量，merge.concurrent.size <= 实际并发量 <= merge.concurrent.size * Server数量。超出限制则抛出异常，无法合并。默认配置为 4 ，小于等于0则取默认值
     */
    public static Integer MERGE_CONCURRENT_SIZE = 4;

    public static Boolean IS_SSL ;

    //启动服务需要注册runner的租户，以逗号隔开
    public static String REGISTER_TENANTS;

    public static Integer SERVER_ID;


    public static final List<String> RES_POSSIBLY_CHARSETS = new ArrayList<String>();


    public static String JWT_SECRET() {
        return JWT_SECRET;
    }

    public static String AUTOEXEC_HOME() {
        return AUTOEXEC_HOME;
    }

    public static Integer SERVER_PORT() {
        return SERVER_PORT;
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

    public static Boolean IS_SSL(){
        return IS_SSL;
    }

    public static String REGISTER_TENANTS(){
        return REGISTER_TENANTS;
    }

    public static Integer SERVER_ID(){
        return SERVER_ID;
    }


    @PostConstruct
    public void init() {
        try {
            String propertiesString = configService.getConfig("config", "com.neatlogic", 3000);
            loadNacosProperties(propertiesString);
            configService.addListener("config", "com.neatlogic", new Listener() {
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
            SERVER_PORT = Integer.parseInt(prop.getProperty("server.port","8084"));
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
            DATA_HOME = prop.getProperty("data.home", "/app/autoexec/");
            GITLAB_PASSWORD = prop.getProperty("gitlab.password");

            //codehub
            NEATLOGIC_HOME = System.getenv("NEATLOGIC_HOME");
            if (NEATLOGIC_HOME == null || "".equals(NEATLOGIC_HOME)) {
                NEATLOGIC_HOME = "/app";
            }

            WORKING_COPY_PATH = NEATLOGIC_HOME + prop.getProperty("repo.workingcopy.path", "/data/workingcopy");
            ISSUE_PATTERN = Pattern.compile(prop.getProperty("issue.pattern", "^\\s*([a-zA-Z]+-\\d+)"), 0);

            String issueSeparator = prop.getProperty("issue.separator", ",，\\s");
            if (!issueSeparator.contains(",")) {
                issueSeparator = issueSeparator + ",";
            }
            MULTIPLE_ISSUE_PATTERN = Pattern.compile(String.format("%s([%s]|)+", ISSUE_PATTERN.toString(), issueSeparator));
            // 搜索commit的时候最大能获取到commit数量
            MAX_GET_COMMIT_LOG = Integer.parseInt(prop.getProperty("max.get.commit.log", "300"));
            CACHE_ENABLE = Boolean.parseBoolean(prop.getProperty("cache.enable", "true"));
            CACHE_MAX_SIZE = Integer.parseInt(prop.getProperty("cache.max.size", "512")) * 1024;
            // 转为字节

            FILE_MIMETYPE_TEXT_PLAIN = prop.getProperty("file.mimetype.text.plain", "sql text c cc c++ cpp h pl py txt java el gitignore js css properties jsp yml json md vue sh config htm html xml classpath project pm less scss");
            FILE_MIME_TYPE_MAP = new MimetypesFileTypeMap();
            FILE_MIME_TYPE_MAP.addMimeTypes("text/plain " + FILE_MIMETYPE_TEXT_PLAIN);
            FILE_CONTENT_SHOW_MAX_SIZE = Integer.parseInt(prop.getProperty("file.content.show.max.size", "2048"));
            CONNECTION_CONNECT_TIMEOUT = Integer.parseInt(prop.getProperty("net.connection.connecttimeout", "0"));
            CONNECTION_READ_TIMEOUT = Integer.parseInt(prop.getProperty("net.connection.readtimeout", "0"));
            MERGE_CONCURRENT_SIZE = Integer.parseInt(prop.getProperty("merge.concurrent.size", "4"));
            if (MERGE_CONCURRENT_SIZE <= 0) {
                MERGE_CONCURRENT_SIZE = 4;
            }
            String possiblyCharsets = prop.getProperty("res.possibly.charsets", "UTF-8,GBK,ISO-8859-1");
            for (String charset : possiblyCharsets.split(",")) {
                if (!charset.trim().equals("")) {
                    RES_POSSIBLY_CHARSETS.add(charset);
                }
            }

            IS_SSL = Boolean.valueOf(prop.getProperty("server.ssl.enabled", "false"));

            REGISTER_TENANTS = prop.getProperty("register.tenants");

            SERVER_ID = Integer.parseInt(prop.getProperty("server.id","1"));




        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
