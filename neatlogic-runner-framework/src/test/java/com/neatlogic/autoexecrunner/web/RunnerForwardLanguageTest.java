package com.neatlogic.autoexecrunner.web;

import com.neatlogic.autoexecrunner.asynchronization.threadlocal.RequestContext;
import com.neatlogic.autoexecrunner.exception.job.FifoWriteException;
import com.neatlogic.autoexecrunner.filter.DuplicateSlashPathFilter;
import com.neatlogic.autoexecrunner.filter.JsonWebTokenValidFilter;
import com.neatlogic.autoexecrunner.listener.ThreadlocalClearListener;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** 在真实容器上检查双斜杠转发复用工作线程时的请求语言和认证边界。 */
class RunnerForwardLanguageTest {
    /** 连续发送不同语言的请求，通过缺少租户的认证响应验证过滤器确实参与转发。 */
    @Test
    void initializesLanguageForEveryForward() throws Exception {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
        factory.setAddress(InetAddress.getLoopbackAddress());
        factory.setContextPath("/autoexecrunner");
        factory.addConnectorCustomizers(connector -> {
            connector.setProperty("minSpareThreads", "1");
            connector.setProperty("maxThreads", "1");
        });
        BlockingQueue<Boolean> freshContexts = new LinkedBlockingQueue<>();
        BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        WebServer server = factory.getWebServer(context -> {
            context.addListener(new ThreadlocalClearListener());
            // 从普通请求外层观察过滤链结束时的错误文案，避免测试自己设置语言。
            Filter observer = (request, response, chain) -> {
                freshContexts.add(RequestContext.get() == null);
                chain.doFilter(request, response);
                messages.add(new FifoWriteException("ENXIO", 6).getUserMessage());
            };
            context.addFilter("observer", observer).addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
            context.addFilter("normalize", new DuplicateSlashPathFilter())
                    .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
            WebFilter mapping = JsonWebTokenValidFilter.class.getAnnotation(WebFilter.class);
            context.addFilter(mapping.filterName(), new JsonWebTokenValidFilter())
                    .addMappingForUrlPatterns(EnumSet.copyOf(List.of(mapping.dispatcherTypes())), true, mapping.urlPatterns());
            context.addServlet("probe", new HttpServlet() {
                /** 未经认证就进入业务接口时，返回可与认证失败明确区分的状态。 */
                @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
                    resp.setStatus(204);
                }
            }).addMapping("/api/probe");
        });
        try {
            server.start();
            HttpClient client = HttpClient.newHttpClient();
            for (String language : List.of("en", "zh", "en", "zh")) {
                String path = language.equals("en") ? "/api/probe" : "//api/probe";
                URI uri = URI.create("http://localhost:" + server.getPort() + "/autoexecrunner" + path);
                HttpRequest request = HttpRequest.newBuilder(uri).header("Accept-Language", language)
                        .POST(HttpRequest.BodyPublishers.noBody()).build();
                HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                String message = messages.poll(2, TimeUnit.SECONDS);
                assertNotNull(message);
                assertTrue(message.contains(language.equals("en") ? "No process is reading" : "没有读取进程"), message);
                assertEquals(521, response.statusCode());
                assertEquals(Boolean.TRUE, freshContexts.poll(), "工作线程残留了上次请求的上下文");
            }
        } finally {
            server.stop();
        }
    }
}
