/*
 * Copyright (C) 2026 TechSure Co., Ltd. All Rights Reserved.
 */
package com.neatlogic.autoexecrunner.api.autoexec;

import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

class ExecRToolApiTest {

    @Test
    void shouldKeepIpOptionalForRunnerExecution() throws Exception {
        Method method = ExecRToolApi.class.getMethod("myDoService", com.alibaba.fastjson.JSONObject.class);
        Input input = method.getAnnotation(Input.class);
        Param ipParam = Arrays.stream(input.value())
                .filter(param -> "ip".equals(param.name()))
                .findFirst()
                .orElseThrow();

        Assertions.assertFalse(ipParam.isRequired());
    }
}
