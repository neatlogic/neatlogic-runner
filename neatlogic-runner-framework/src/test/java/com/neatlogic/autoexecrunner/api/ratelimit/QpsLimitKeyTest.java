package com.neatlogic.autoexecrunner.api.ratelimit;

import com.neatlogic.autoexecrunner.api.autoexec.ExecRToolApi;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.dto.ApiVo;
import com.neatlogic.autoexecrunner.restful.annotation.QpsLimitKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class QpsLimitKeyTest {

    private static final String TEST_CONFIG_KEY = "qps.limit.test.default";

    @QpsLimitKey(TEST_CONFIG_KEY)
    private static class DefaultQpsApi {
    }

    @Test
    void shouldExposeAnnotationDefaults() {
        QpsLimitKey defaultAnnotation = DefaultQpsApi.class.getAnnotation(QpsLimitKey.class);
        QpsLimitKey execrtoolAnnotation = ExecRToolApi.class.getAnnotation(QpsLimitKey.class);

        Assertions.assertEquals(500, defaultAnnotation.defaultValue());
        Assertions.assertEquals(20, execrtoolAnnotation.defaultValue());
    }

    @Test
    void shouldPreferConfiguredQpsAndFallBackToAnnotationDefault() {
        ApiVo apiVo = new ApiVo();
        apiVo.setQpsLimitConfigKey(TEST_CONFIG_KEY);
        apiVo.setQpsLimitDefaultValue(20);
        String previousValue = Config.properties.getProperty(TEST_CONFIG_KEY);
        try {
            Config.properties.remove(TEST_CONFIG_KEY);
            Assertions.assertEquals(20, TokenRateLimiter.resolveQps(apiVo));

            Config.properties.setProperty(TEST_CONFIG_KEY, "37");
            Assertions.assertEquals(37, TokenRateLimiter.resolveQps(apiVo));
        } finally {
            if (previousValue == null) {
                Config.properties.remove(TEST_CONFIG_KEY);
            } else {
                Config.properties.setProperty(TEST_CONFIG_KEY, previousValue);
            }
        }
    }
}
