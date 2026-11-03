package com.neatlogic.autoexecrunner.web;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.api.job.node.JobPhaseNodeSubmitWaitInputApi;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.RequestContext;
import com.neatlogic.autoexecrunner.dto.ApiHandlerVo;
import com.neatlogic.autoexecrunner.dto.ApiVo;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import com.neatlogic.autoexecrunner.exception.job.FifoWriteException;
import com.neatlogic.autoexecrunner.restful.core.MyApiComponent;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentFactory;
import com.neatlogic.autoexecrunner.util.FileSafeUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/** 验证反射调用保留安全提示及兼容诊断消息。 */
class RunnerUserMessageTest {
    @TempDir Path root;

    /** 使用真实私有 API 分发和代理，覆盖请求语言、原始异常以及 520 序列化。 */
    @Test void propagatesLocalizedMessageThroughPostDispatcher() throws Exception {
        String token = "test/user-message";
        ApiVo api = new ApiVo();
        api.setToken(token);
        api.setHandler(token);
        ProxyFactory proxy = new ProxyFactory(new JobPhaseNodeSubmitWaitInputApi() {
            /** 使用固定诊断码覆盖反射路径，无需发送真实交互输入。 */
            @Override public Object myDoService(JSONObject params) {
                throw new FifoWriteException("ENXIO", 6);
            }
        });
        proxy.setInterfaces(MyApiComponent.class);
        proxy.setExposeProxy(true);
        PrivateApiComponentFactory.getApiMap().put(token, api);
        PrivateApiComponentFactory.getApiHandlerMap().put(token, new ApiHandlerVo());
        PrivateApiComponentFactory.getComponentMap().put(token, (MyApiComponent) proxy.getProxy());
        try {
            for (Locale locale : new Locale[]{Locale.ENGLISH, Locale.CHINESE}) {
                MockHttpServletRequest request = new MockHttpServletRequest();
                request.setServletPath("/api/rest/" + token);
                request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/rest/**");
                request.addHeader("Accept-Language", "en");
                request.addPreferredLocale(Locale.ENGLISH);
                if (locale.equals(Locale.CHINESE)) {
                    request.setCookies(new Cookie("neatlogic_language", "zh_CN"));
                }
                MockHttpServletResponse response = new MockHttpServletResponse();
                RequestContext.init(request, request.getServletPath(), response);
                new ApiDispatcher().dispatcherForPost(new JSONObject(), request, response);
                assertEquals(520, response.getStatus());
                JSONObject body = JSONObject.parseObject(response.getContentAsString());
                assertEquals("ERROR", body.get("Status"));
                assertEquals("FIFO ENXIO (errno=6)", body.get("Message"));
                String text = body.getString("UserMessage");
                assertTrue(text.contains(locale.equals(Locale.ENGLISH) ? "No process is reading" : "没有读取进程"), text);
                assertTrue(text.contains("errno=6"));
            }
        } finally {
            if (RequestContext.get() != null) RequestContext.get().release();
            PrivateApiComponentFactory.getApiMap().remove(token);
            PrivateApiComponentFactory.getApiHandlerMap().remove(token);
            PrivateApiComponentFactory.getComponentMap().remove(token);
        }
    }

    /** 仅由已知异常生成提示，禁止路径中的用户数据进入可展示字段。 */
    @Test void separatesSafePathReasonsFromDiagnosticMessages() throws Exception {
        Path allowed = Files.createDirectory(root.resolve("allowed"));
        ApiRuntimeException ex = assertThrows(ApiRuntimeException.class,
                () -> FileSafeUtil.getChildPath(allowed.toString(), "../sensitive-fixture"));
        JSONObject error = ApiDispatcher.businessError(ex);
        assertEquals("文件路径不合法", error.get("Message"));
        assertTrue(error.getString("UserMessage").contains("超出允许的工作目录"));
        assertFalse(error.getString("UserMessage").contains("sensitive-fixture"));
        assertFalse(ApiDispatcher.businessError(new ApiRuntimeException("sensitive-fixture")).containsKey("UserMessage"));
        assertFalse(ApiDispatcher.businessError(new FifoWriteException("UNKNOWN")).containsKey("UserMessage"));
    }
}
