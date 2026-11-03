package com.neatlogic.autoexecrunner.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Locale;

/** 从 Runner 自身的中英文语言包读取可公开的异常提示。 */
public final class RunnerMessages {
    private static final Logger logger = LoggerFactory.getLogger(RunnerMessages.class);
    private static final JSONObject ZH = load("zh");
    private static final JSONObject EN = load("en");

    private RunnerMessages() {
    }

    /** 语言资源随模块打包；读取失败时仅保留原有诊断消息。 */
    private static JSONObject load(String language) {
        String resource = "/neatlogic/resources/runner-framework/i18n/language_" + language + ".json";
        try (InputStream stream = RunnerMessages.class.getResourceAsStream(resource)) {
            if (stream == null) {
                logger.error("Runner 语言资源缺失：{}", resource);
                return null;
            }
            return JSON.parseObject(stream.readAllBytes(), JSONObject.class);
        } catch (IOException | RuntimeException ex) {
            logger.error("读取 Runner 语言资源失败：{}", resource, ex);
            return null;
        }
    }

    /** 仅格式化模块提供的模板和安全参数；缺少译文时不产生可展示消息。 */
    public static String t(String key, Object... args) {
        RequestContext context = RequestContext.get();
        Locale requested = context == null ? null : context.getLocale();
        boolean english = requested != null && "en".equalsIgnoreCase(requested.getLanguage());
        Object value = english ? EN : ZH;
        for (String part : key.split("\\.")) {
            if (!(value instanceof JSONObject)) {
                return null;
            }
            value = ((JSONObject) value).get(part);
        }
        return value instanceof String ? new MessageFormat((String) value, english ? Locale.ENGLISH : Locale.CHINESE).format(args) : null;
    }
}
