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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.ProtocolVersions;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.*;

/**
 * Informant MCP客户端。
 * 每次执行创建独立的MCP会话，并在执行结束后关闭会话，避免不同节点或请求之间共享会话状态。
 */
@Component
public class InformantMcpClient {
    private static final String MCP_ENDPOINT = "/api/mcp";
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(90);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Duration connectTimeout;
    private final Duration requestTimeout;

    /**
     * 使用生产环境约定的连接和请求超时创建客户端。
     */
    public InformantMcpClient() {
        this(DEFAULT_CONNECT_TIMEOUT, DEFAULT_REQUEST_TIMEOUT);
    }

    /**
     * 允许协议测试缩短超时，生产Bean始终使用默认构造器。
     */
    InformantMcpClient(Duration connectTimeout, Duration requestTimeout) {
        this.connectTimeout = connectTimeout;
        this.requestTimeout = requestTimeout;
    }

    /**
     * 查询Informant当前暴露的全部MCP工具。
     *
     * @param protocol Informant访问协议
     * @param ip       Informant地址
     * @param port     Informant端口
     * @param header   HMAC认证请求头
     * @return MCP服务信息及工具定义
     */
    public JSONObject listTools(String protocol, String ip, int port, Map<String, String> header) {
        McpSyncClient client = createClient(protocol, ip, port, header);
        try {
            McpSchema.InitializeResult initializeResult = client.initialize();
            List<Object> toolList = new ArrayList<>();
            String nextCursor = null;
            do {
                McpSchema.ListToolsResult listToolsResult;
                if (StringUtils.isBlank(nextCursor)) {
                    listToolsResult = client.listTools();
                } else {
                    listToolsResult = client.listTools(nextCursor);
                }
                if (listToolsResult != null && listToolsResult.tools() != null) {
                    toolList.addAll(listToolsResult.tools());
                }
                if (listToolsResult == null) {
                    nextCursor = null;
                } else {
                    nextCursor = listToolsResult.nextCursor();
                }
            } while (StringUtils.isNotBlank(nextCursor));

            JSONObject result = buildServerResult(initializeResult);
            result.put("tools", toJsonArray(toolList));
            return result;
        } catch (Exception ex) {
            throw buildMcpException(ex);
        } finally {
            closeClient(client);
        }
    }

    /**
     * 调用Informant指定的MCP工具。
     *
     * @param protocol Informant访问协议
     * @param ip       Informant地址
     * @param port     Informant端口
     * @param header   HMAC认证请求头
     * @param toolName MCP工具名
     * @param arguments MCP工具参数
     * @return MCP服务信息及工具调用结果
     */
    public JSONObject callTool(String protocol, String ip, int port, Map<String, String> header, String toolName, JSONObject arguments) {
        McpSyncClient client = createClient(protocol, ip, port, header);
        try {
            McpSchema.InitializeResult initializeResult = client.initialize();
            Map<String, Object> argumentMap = toMap(arguments);
            McpSchema.CallToolRequest request = McpSchema.CallToolRequest.builder(toolName)
                    .arguments(argumentMap)
                    .build();
            McpSchema.CallToolResult callToolResult = client.callTool(request);
            JSONObject result = buildServerResult(initializeResult);
            result.put("result", toJsonObject(callToolResult));
            return result;
        } catch (Exception ex) {
            throw buildMcpException(ex);
        } finally {
            closeClient(client);
        }
    }

    /**
     * 创建仅访问固定/api/mcp路径的官方SDK客户端。
     */
    private McpSyncClient createClient(String protocol, String ip, int port, Map<String, String> header) {
        String baseUri = buildBaseUri(protocol, ip, port);
        HttpClientStreamableHttpTransport.Builder transportBuilder = HttpClientStreamableHttpTransport.builder(baseUri)
                .endpoint(MCP_ENDPOINT)
                .connectTimeout(connectTimeout)
                .supportedProtocolVersions(Arrays.asList(
                        ProtocolVersions.MCP_2025_03_26,
                        ProtocolVersions.MCP_2025_06_18,
                        ProtocolVersions.MCP_2025_11_25
                ))
                .httpRequestCustomizer((builder, method, uri, body, context) -> applyHeaders(builder, header));
        if (StringUtils.equalsIgnoreCase("https", protocol)) {
            transportBuilder.clientBuilder(createInformantHttpsClientBuilder());
        }
        HttpClientStreamableHttpTransport transport = transportBuilder.build();
        return McpClient.sync(transport)
                .requestTimeout(requestTimeout)
                .initializationTimeout(requestTimeout)
                .clientInfo(McpSchema.Implementation.builder("neatlogic-runner", "1.0.0").build())
                .build();
    }

    /**
     * 为Informant节点创建独立的HTTPS客户端，兼容节点自签名或主机名不匹配的存量证书。
     * 该信任策略只作用于当前MCP客户端，不修改Runner进程的全局TLS配置。
     */
    HttpClient.Builder createInformantHttpsClientBuilder() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null,
                    new X509ExtendedTrustManager[]{new InformantNodeTrustManager()},
                    new SecureRandom());
            return HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .sslContext(sslContext);
        } catch (NoSuchAlgorithmException | KeyManagementException ex) {
            throw new ApiRuntimeException("初始化Informant HTTPS连接失败");
        }
    }

    /**
     * 只追加NeatLogic计算好的三项HMAC请求头，避免透传任意请求头。
     */
    private void applyHeaders(HttpRequest.Builder builder, Map<String, String> header) {
        builder.header("X-Agent-Id", header.get("X-Agent-Id"));
        builder.header("X-Timestamp", header.get("X-Timestamp"));
        builder.header("X-Signature", header.get("X-Signature"));
    }

    /**
     * 使用URI构造器生成基础地址，确保目标路径始终由客户端固定为/api/mcp。
     */
    private String buildBaseUri(String protocol, String ip, int port) {
        try {
            URI uri = new URI(protocol, null, ip, port, null, null, null);
            return uri.toString();
        } catch (URISyntaxException ex) {
            throw new ApiRuntimeException("Informant地址不合法");
        }
    }

    /**
     * 组织初始化阶段返回的协议和服务端信息。
     */
    private JSONObject buildServerResult(McpSchema.InitializeResult initializeResult) {
        JSONObject result = new JSONObject(true);
        if (initializeResult == null) {
            return result;
        }
        result.put("protocolVersion", initializeResult.protocolVersion());
        result.put("serverInfo", toJsonObject(initializeResult.serverInfo()));
        result.put("instructions", initializeResult.instructions());
        return result;
    }

    /**
     * 将Fastjson对象转换为SDK需要的普通Map。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(JSONObject object) throws Exception {
        if (object == null || object.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return OBJECT_MAPPER.readValue(object.toJSONString(), LinkedHashMap.class);
    }

    /**
     * 将SDK对象转换为Runner统一使用的Fastjson对象。
     */
    private JSONObject toJsonObject(Object value) {
        if (value == null) {
            return new JSONObject(true);
        }
        try {
            return JSON.parseObject(OBJECT_MAPPER.writeValueAsString(value));
        } catch (Exception ex) {
            throw new ApiRuntimeException("MCP响应转换失败");
        }
    }

    /**
     * 将SDK工具列表转换为Fastjson数组。
     */
    private JSONArray toJsonArray(Object value) {
        if (value == null) {
            return new JSONArray();
        }
        try {
            return JSON.parseArray(OBJECT_MAPPER.writeValueAsString(value));
        } catch (Exception ex) {
            throw new ApiRuntimeException("MCP工具列表转换失败");
        }
    }

    /**
     * 关闭SDK客户端。关闭失败不能覆盖已经成功的工具结果。
     */
    private void closeClient(McpSyncClient client) {
        try {
            client.closeGracefully();
        } catch (Exception ignored) {
            // MCP会话清理失败不影响当前工具调用结果，同时不输出可能包含会话信息的异常。
        }
    }

    /**
     * 构造不包含认证头和会话标识的调用异常。
     */
    ApiRuntimeException buildMcpException(Exception ex) {
        if (exceptionContains(ex, "401", "unauthorized")) {
            return new ApiRuntimeException("调用Informant MCP失败：认证失败");
        } else if (exceptionContains(ex, "404", "not found")) {
            return new ApiRuntimeException("调用Informant MCP失败：服务或工具不存在");
        } else if (exceptionContains(ex, "timeout", "timed out")) {
            return new ApiRuntimeException("调用Informant MCP失败：请求超时");
        } else if (exceptionContains(ex,
                "sslhandshakeexception",
                "certificateexception",
                "pkix",
                "certification path",
                "subject alternative")) {
            return new ApiRuntimeException("调用Informant MCP失败：HTTPS证书校验失败");
        } else {
            // 不透传SDK原始异常，避免响应头、Session标识或请求上下文进入上层日志。
            return new ApiRuntimeException("调用Informant MCP失败：协议交互异常");
        }
    }

    /**
     * 沿异常链匹配错误类别，但不把异常原文写入返回值或日志。
     */
    private boolean exceptionContains(Throwable throwable, String... keywordList) {
        Throwable current = throwable;
        while (current != null) {
            String text = current.getClass().getName() + ":" + StringUtils.defaultString(current.getMessage());
            for (String keyword : keywordList) {
                if (StringUtils.containsIgnoreCase(text, keyword)) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * 兼容由Informant节点自行生成的证书，并避免JDK为普通X509TrustManager追加主机名校验。
     * Informant请求仍由数据库中的固定节点地址和HMAC签名共同约束。
     */
    static class InformantNodeTrustManager extends X509ExtendedTrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
