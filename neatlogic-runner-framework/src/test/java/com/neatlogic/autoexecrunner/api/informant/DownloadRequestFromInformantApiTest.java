/*
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

package com.neatlogic.autoexecrunner.api.informant;

import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.util.HttpRequestUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.mock.web.MockHttpServletResponse;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;

public class DownloadRequestFromInformantApiTest {

    @Test
    public void allowsOnlyKnownDownloadUrls() {
        Assertions.assertDoesNotThrow(() -> DownloadRequestFromInformantApi.validateDownloadUrl(
                "/api/threaddump/dumptxt/download"));
        Assertions.assertDoesNotThrow(() -> DownloadRequestFromInformantApi.validateDownloadUrl(
                "/api/query/v1/logs/download"));
        Assertions.assertThrows(ApiRuntimeException.class,
                () -> DownloadRequestFromInformantApi.validateDownloadUrl("/api/query/v1/logs"));
        Assertions.assertThrows(ApiRuntimeException.class,
                () -> DownloadRequestFromInformantApi.validateDownloadUrl("/api/query/v1/logs/download?name=runner.log"));
    }
    /** 两层代理遵循相同的路由契约，不放行查询串、编码路径或路径穿越。 */
    @Test public void evidenceDownloadUrlIsStrictlyScoped() {
        DownloadRequestFromInformantApi.validateDownloadUrl("/api/query/v1/incidents/1/log-evidence/download");
        DownloadRequestFromInformantApi.validateDownloadUrl("/api/query/v1/incidents/13CFbe34e08c0c9/log-evidence/download");
        for (String url : new String[]{
            null,
            "/api/query/v1/incidents//log-evidence/download",
            "/api/query/v1/incidents/xyz/log-evidence/download",
            "/api/query/v1/incidents/../log-evidence/download",
            "/api/query/v1/incidents/%2e%2e/log-evidence/download",
            "/api/query/v1/incidents/1%2f2/log-evidence/download",
            "/api/query/v1/incidents/1/log-evidence/download?entry=0",
            "/api/query/v1/incidents/1/log-evidence/download#x",
            "/api/query/v1/incidents/1/log-evidence/download/extra",
            "/api/query/v1/incidents/1/log-evidence/download/",
            "https://example.invalid/api/query/v1/incidents/1/log-evidence/download"
        }) {
            Assertions.assertThrows(ApiRuntimeException.class, () -> DownloadRequestFromInformantApi.validateDownloadUrl(url));
        }
    }
    /** 使用真实请求构造器验证查询参数，不建立连接或扩展生产方法的可见性。 */
    @Test public void evidenceParamsUseExistingQueryForwarding() throws Exception {
        JSONObject param = new JSONObject();
        param.put("entry", 0);
        param.put("dump_type", "java");
        Method build = DownloadRequestFromInformantApi.class.getDeclaredMethod("buildHttpRequest",
                String.class, String.class, JSONObject.class, JSONObject.class, int.class, int.class, HttpServletResponse.class);
        build.setAccessible(true);
        HttpRequestUtil request = (HttpRequestUtil) build.invoke(new DownloadRequestFromInformantApi(),
                "http://unused/api/query/v1/incidents/1/log-evidence/download", "get", param, new JSONObject(),
                1000, 1000, new MockHttpServletResponse());
        Field url = HttpRequestUtil.class.getDeclaredField("url");
        url.setAccessible(true);
        String query = URI.create((String) url.get(request)).getRawQuery();
        Assertions.assertTrue(query.contains("entry=0"));
        Assertions.assertTrue(query.contains("dump_type=java"));
    }
}
