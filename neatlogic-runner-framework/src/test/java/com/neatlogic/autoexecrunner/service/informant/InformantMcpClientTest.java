/*
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

package com.neatlogic.autoexecrunner.service.informant;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class InformantMcpClientTest {
    private static final String SESSION_ID = "informant-session-1";
    private HttpServer server;
    private int port;

    @BeforeEach
    public void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
    }

    @AfterEach
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void listToolsUsesHmacHeadersAndCompletesMcpLifecycle() {
        List<String> methodList = new ArrayList<>();
        AtomicBoolean deleteReceived = new AtomicBoolean(false);
        AtomicInteger listCount = new AtomicInteger();
        server.createContext("/api/mcp", exchange -> {
            assertHmacHeader(exchange);
            if ("DELETE".equals(exchange.getRequestMethod())) {
                Assertions.assertEquals(SESSION_ID, exchange.getRequestHeaders().getFirst("MCP-Session-Id"));
                deleteReceived.set(true);
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            JSONObject request = readJson(exchange);
            String method = request.getString("method");
            methodList.add(method);
            if ("initialize".equals(method)) {
                writeJson(exchange, response(request, initializeResult()), SESSION_ID);
            } else if ("notifications/initialized".equals(method)) {
                Assertions.assertEquals(SESSION_ID, exchange.getRequestHeaders().getFirst("MCP-Session-Id"));
                exchange.sendResponseHeaders(202, -1);
            } else if ("tools/list".equals(method)) {
                Assertions.assertEquals(SESSION_ID, exchange.getRequestHeaders().getFirst("MCP-Session-Id"));
                JSONObject result = new JSONObject(true);
                JSONArray tools = new JSONArray();
                JSONObject tool = new JSONObject(true);
                int currentListCount = listCount.incrementAndGet();
                if (currentListCount == 1) {
                    tool.put("name", "get_health_summary");
                    result.put("nextCursor", "page-2");
                } else {
                    Assertions.assertEquals("page-2",
                            request.getJSONObject("params").getString("cursor"));
                    tool.put("name", "get_service_objects");
                }
                tool.put("description", "查询健康摘要");
                JSONObject inputSchema = new JSONObject(true);
                inputSchema.put("type", "object");
                inputSchema.put("properties", new JSONObject(true));
                tool.put("inputSchema", inputSchema);
                JSONObject annotations = new JSONObject(true);
                annotations.put("readOnlyHint", true);
                annotations.put("destructiveHint", false);
                tool.put("annotations", annotations);
                tools.add(tool);
                result.put("tools", tools);
                writeJson(exchange, response(request, result), null);
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
        });
        server.start();

        JSONObject result = new InformantMcpClient().listTools("http", "127.0.0.1", port, hmacHeader());

        Assertions.assertEquals(List.of(
                "initialize", "notifications/initialized", "tools/list", "tools/list"), methodList);
        Assertions.assertTrue(deleteReceived.get());
        Assertions.assertEquals("2025-06-18", result.getString("protocolVersion"));
        Assertions.assertEquals("informant", result.getJSONObject("serverInfo").getString("name"));
        Assertions.assertEquals("按证据逐步诊断", result.getString("instructions"));
        Assertions.assertEquals("get_health_summary", result.getJSONArray("tools").getJSONObject(0).getString("name"));
        Assertions.assertEquals("get_service_objects", result.getJSONArray("tools").getJSONObject(1).getString("name"));
    }

    @Test
    public void callToolReadsSseStructuredContent() {
        server.createContext("/api/mcp", exchange -> {
            assertHmacHeader(exchange);
            if ("DELETE".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            JSONObject request = readJson(exchange);
            String method = request.getString("method");
            if ("initialize".equals(method)) {
                writeJson(exchange, response(request, initializeResult()), SESSION_ID);
            } else if ("notifications/initialized".equals(method)) {
                exchange.sendResponseHeaders(202, -1);
            } else if ("tools/call".equals(method)) {
                Assertions.assertEquals("get_health_summary", request.getJSONObject("params").getString("name"));
                Assertions.assertEquals("java", request.getJSONObject("params").getJSONObject("arguments").getString("service"));
                JSONObject result = new JSONObject(true);
                JSONArray content = new JSONArray();
                JSONObject text = new JSONObject(true);
                text.put("type", "text");
                text.put("text", "ok");
                content.add(text);
                result.put("content", content);
                JSONObject structuredContent = new JSONObject(true);
                structuredContent.put("status", "healthy");
                result.put("structuredContent", structuredContent);
                result.put("isError", false);
                writeSse(exchange, response(request, result));
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
        });
        server.start();

        JSONObject arguments = new JSONObject(true);
        arguments.put("service", "java");
        JSONObject response = new InformantMcpClient().callTool(
                "http",
                "127.0.0.1",
                port,
                hmacHeader(),
                "get_health_summary",
                arguments
        );

        JSONObject result = response.getJSONObject("result");
        Assertions.assertFalse(result.getBooleanValue("isError"));
        Assertions.assertEquals("healthy", result.getJSONObject("structuredContent").getString("status"));
        Assertions.assertEquals("ok", result.getJSONArray("content").getJSONObject(0).getString("text"));
    }

    @Test
    public void concurrentOperationsUseIndependentSessions() {
        AtomicInteger sessionSequence = new AtomicInteger();
        Map<String, Set<String>> sessionStageMap = new ConcurrentHashMap<>();
        server.createContext("/api/mcp", exchange -> {
            assertHmacHeader(exchange);
            if ("DELETE".equals(exchange.getRequestMethod())) {
                String sessionId = exchange.getRequestHeaders().getFirst("MCP-Session-Id");
                recordSessionStage(sessionStageMap, sessionId, "delete");
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            JSONObject request = readJson(exchange);
            String method = request.getString("method");
            if ("initialize".equals(method)) {
                String sessionId = "concurrent-session-" + sessionSequence.incrementAndGet();
                sessionStageMap.put(sessionId, ConcurrentHashMap.newKeySet());
                writeJson(exchange, response(request, initializeResult()), sessionId);
            } else if ("notifications/initialized".equals(method)) {
                String sessionId = exchange.getRequestHeaders().getFirst("MCP-Session-Id");
                recordSessionStage(sessionStageMap, sessionId, "initialized");
                exchange.sendResponseHeaders(202, -1);
            } else if ("tools/list".equals(method)) {
                String sessionId = exchange.getRequestHeaders().getFirst("MCP-Session-Id");
                recordSessionStage(sessionStageMap, sessionId, "list");
                JSONObject result = new JSONObject(true);
                result.put("tools", new JSONArray());
                writeJson(exchange, response(request, result), null);
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
        });
        server.start();

        CompletableFuture<JSONObject> first = CompletableFuture.supplyAsync(
                () -> new InformantMcpClient().listTools("http", "127.0.0.1", port, hmacHeader()));
        CompletableFuture<JSONObject> second = CompletableFuture.supplyAsync(
                () -> new InformantMcpClient().listTools("http", "127.0.0.1", port, hmacHeader()));
        CompletableFuture.allOf(first, second).join();

        Assertions.assertEquals(2, sessionStageMap.size());
        for (Set<String> stageSet : sessionStageMap.values()) {
            Assertions.assertEquals(Set.of("initialized", "list", "delete"), stageSet);
        }
    }

    @Test
    public void protocolErrorsAreHandledWithoutLeakingRequestContext() {
        AtomicInteger requestCount = new AtomicInteger();
        server.createContext("/api/mcp", exchange -> {
            int currentRequestCount = requestCount.incrementAndGet();
            if (currentRequestCount == 1) {
                exchange.sendResponseHeaders(401, -1);
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
        });
        server.start();

        InformantMcpClient client = new InformantMcpClient();
        ApiRuntimeException unauthorized = Assertions.assertThrows(
                ApiRuntimeException.class,
                () -> client.listTools("http", "127.0.0.1", port, hmacHeader()));
        ApiRuntimeException notFound = Assertions.assertThrows(
                ApiRuntimeException.class,
                () -> client.listTools("http", "127.0.0.1", port, hmacHeader()));

        Assertions.assertTrue(unauthorized.getMessage().contains("调用Informant MCP失败"));
        Assertions.assertTrue(notFound.getMessage().contains("调用Informant MCP失败"));
        Assertions.assertFalse(unauthorized.getMessage().contains("signature-1"));
        Assertions.assertFalse(notFound.getMessage().contains("MCP-Session-Id"));
    }

    @Test
    public void requestTimeoutIsClassifiedWithoutRawSdkException() {
        server.createContext("/api/mcp", exchange -> {
            try {
                Thread.sleep(500);
                exchange.sendResponseHeaders(504, -1);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        server.start();

        InformantMcpClient client = new InformantMcpClient(
                Duration.ofMillis(100), Duration.ofMillis(100));
        ApiRuntimeException timeout = Assertions.assertThrows(
                ApiRuntimeException.class,
                () -> client.listTools("http", "127.0.0.1", port, hmacHeader()));

        Assertions.assertTrue(timeout.getMessage().contains("请求超时"));
        Assertions.assertFalse(timeout.getMessage().contains("signature-1"));
    }

    @Test
    public void httpsClientUsesScopedCompatibilitySslContext() throws Exception {
        HttpClient httpClient = new InformantMcpClient()
                .createInformantHttpsClientBuilder()
                .build();

        Assertions.assertNotSame(SSLContext.getDefault(), httpClient.sslContext());
        Assertions.assertEquals(HttpClient.Version.HTTP_1_1, httpClient.version());
    }

    @Test
    public void certificateFailureIsClassifiedWithoutRawCertificateDetails() {
        SSLHandshakeException cause = new SSLHandshakeException(
                "No subject alternative names matching IP address 192.0.2.25 found");

        ApiRuntimeException exception = new InformantMcpClient().buildMcpException(cause);

        Assertions.assertTrue(exception.getMessage().contains("HTTPS证书校验失败"));
        Assertions.assertFalse(exception.getMessage().contains("192.0.2.25"));
    }

    /**
     * 校验每个MCP协议请求均携带Informant HMAC认证头。
     */
    private void assertHmacHeader(HttpExchange exchange) {
        Assertions.assertEquals("agent-1", exchange.getRequestHeaders().getFirst("X-Agent-Id"));
        Assertions.assertEquals("10001", exchange.getRequestHeaders().getFirst("X-Timestamp"));
        Assertions.assertEquals("signature-1", exchange.getRequestHeaders().getFirst("X-Signature"));
    }

    /**
     * 记录指定Session完成的协议阶段，缺少Session时立即使测试失败。
     */
    private void recordSessionStage(Map<String, Set<String>> sessionStageMap,
                                    String sessionId, String stage) {
        Assertions.assertNotNull(sessionId);
        Set<String> stageSet = sessionStageMap.get(sessionId);
        Assertions.assertNotNull(stageSet);
        stageSet.add(stage);
    }

    private Map<String, String> hmacHeader() {
        Map<String, String> header = new LinkedHashMap<>();
        header.put("X-Agent-Id", "agent-1");
        header.put("X-Timestamp", "10001");
        header.put("X-Signature", "signature-1");
        return header;
    }

    private JSONObject initializeResult() {
        JSONObject result = new JSONObject(true);
        result.put("protocolVersion", "2025-06-18");
        JSONObject capabilities = new JSONObject(true);
        capabilities.put("tools", new JSONObject(true));
        result.put("capabilities", capabilities);
        JSONObject serverInfo = new JSONObject(true);
        serverInfo.put("name", "informant");
        serverInfo.put("version", "1.2.5");
        result.put("serverInfo", serverInfo);
        result.put("instructions", "按证据逐步诊断");
        return result;
    }

    private JSONObject response(JSONObject request, JSONObject result) {
        JSONObject response = new JSONObject(true);
        response.put("jsonrpc", "2.0");
        response.put("id", request.get("id"));
        response.put("result", result);
        return response;
    }

    private JSONObject readJson(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        return JSON.parseObject(new String(body, StandardCharsets.UTF_8));
    }

    private void writeJson(HttpExchange exchange, JSONObject response, String sessionId) throws IOException {
        byte[] body = response.toJSONString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        if (sessionId != null) {
            exchange.getResponseHeaders().set("MCP-Session-Id", sessionId);
        }
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private void writeSse(HttpExchange exchange, JSONObject response) throws IOException {
        String event = "event: message\n" + "data: " + response.toJSONString() + "\n\n";
        byte[] body = event.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
