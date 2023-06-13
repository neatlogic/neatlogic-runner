package com.neatlogic.autoexecrunner.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class TagentConfig {
    private static final Logger logger = LoggerFactory.getLogger(Config.class);

    private static final String CONFIG_FILE = "application.properties";
    public final static String AUTH_TYPE;
    public final static String ACCESS_KEY;
    public final static String ACCESS_SECRET;
    public static final Long AUTOEXEC_NETTY_READ_TIMEOUT;
    public static final Long AUTOEXEC_NETTY_WRITE_TIMEOUT;
    public static final Long AUTOEXEC_NETTY_ALL_TIMEOUT;
    public static final int AUTOEXEC_NETTY_PORT;

    static {
        AUTH_TYPE = getProperty(CONFIG_FILE, "auth.type", "basic");
        ACCESS_KEY = getProperty(CONFIG_FILE, "access.key", "neatlogic");
        ACCESS_SECRET = getProperty(CONFIG_FILE, "access.secret", "x15wDEzSbBL6tV1W");
        AUTOEXEC_NETTY_READ_TIMEOUT = Long.valueOf(getProperty(CONFIG_FILE, "autoexec.netty.readtimeout", "300"));
        AUTOEXEC_NETTY_WRITE_TIMEOUT = Long.valueOf(getProperty(CONFIG_FILE, "autoexec.netty.writetimeout", "900"));
        AUTOEXEC_NETTY_ALL_TIMEOUT = Long.valueOf(getProperty(CONFIG_FILE, "autoexec.netty.alltimeout", "900"));
        AUTOEXEC_NETTY_PORT = Integer.parseInt(getProperty(CONFIG_FILE, "autoexec.netty.port", "8888"));
    }

    public static String getProperty(String configFile, String keyName, String defaultValue) {
        Properties pro = new Properties();
        InputStream is = Config.class.getClassLoader().getResourceAsStream(configFile);
        try {
            pro.load(is);
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("load " + configFile + " error: " + ex.getMessage(), ex);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.error("InputStream error: " + e.getMessage(), e);
                }
            }
        }
        return pro.getProperty(keyName, defaultValue);
    }
}
