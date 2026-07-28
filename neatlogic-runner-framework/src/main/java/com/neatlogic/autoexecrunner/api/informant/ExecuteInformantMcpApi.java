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

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import com.neatlogic.autoexecrunner.restful.annotation.Description;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import com.neatlogic.autoexecrunner.service.informant.InformantMcpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 执行Informant MCP操作的Runner私有接口。
 */
@Component
public class ExecuteInformantMcpApi extends PrivateApiComponentBase {
    private static final String OPERATION_LIST_TOOLS = "listTools";
    private static final String OPERATION_CALL_TOOL = "callTool";

    private final InformantMcpClient informantMcpClient;

    public ExecuteInformantMcpApi(InformantMcpClient informantMcpClient) {
        this.informantMcpClient = informantMcpClient;
    }

    @Override
    public String getName() {
        return "执行Informant MCP操作";
    }

    @Override
    public String getToken() {
        return "informant/mcp/execute";
    }

    @Input({
            @Param(name = "operation", type = ApiParamType.STRING, desc = "操作类型：listTools或callTool", isRequired = true),
            @Param(name = "ip", type = ApiParamType.STRING, desc = "Informant IP", isRequired = true),
            @Param(name = "httpPort", type = ApiParamType.INTEGER, desc = "Informant HTTP端口"),
            @Param(name = "httpsPort", type = ApiParamType.INTEGER, desc = "Informant HTTPS端口"),
            @Param(name = "protocol", type = ApiParamType.STRING, desc = "访问协议：http或https", isRequired = true),
            @Param(name = "header", type = ApiParamType.JSONOBJECT, desc = "Informant HMAC认证请求头", isRequired = true),
            @Param(name = "toolName", type = ApiParamType.STRING, desc = "MCP工具名"),
            @Param(name = "arguments", type = ApiParamType.JSONOBJECT, desc = "MCP工具参数")
    })
    @Description(desc = "通过Runner访问Informant的MCP服务")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String operation = jsonObj.getString("operation");
        String protocol = normalizeProtocol(jsonObj.getString("protocol"));
        String ip = jsonObj.getString("ip");
        validateIp(ip);
        int port = resolvePort(protocol, jsonObj.getInteger("httpPort"), jsonObj.getInteger("httpsPort"));
        Map<String, String> header = resolveHmacHeader(jsonObj.getJSONObject("header"));

        if (OPERATION_LIST_TOOLS.equals(operation)) {
            return informantMcpClient.listTools(protocol, ip, port, header);
        } else if (OPERATION_CALL_TOOL.equals(operation)) {
            String toolName = jsonObj.getString("toolName");
            if (StringUtils.isBlank(toolName)) {
                throw new ApiRuntimeException("callTool操作的toolName不能为空");
            }
            JSONObject arguments = jsonObj.getJSONObject("arguments");
            if (arguments == null) {
                arguments = new JSONObject(true);
            }
            return informantMcpClient.callTool(protocol, ip, port, header, toolName, arguments);
        } else {
            throw new ApiRuntimeException("operation只允许listTools或callTool");
        }
    }

    /**
     * 只接受IP字面量，禁止把Runner中转接口作为域名解析或通用主机代理。
     */
    private void validateIp(String ip) {
        if (StringUtils.isBlank(ip) || !ip.matches("[0-9a-fA-F:.]+")) {
            throw new ApiRuntimeException("ip必须是合法的IP地址");
        }
        try {
            InetAddress.getByName(ip);
        } catch (Exception ex) {
            throw new ApiRuntimeException("ip必须是合法的IP地址");
        }
    }

    /**
     * 校验并标准化Informant访问协议。
     */
    private String normalizeProtocol(String protocol) {
        if ("http".equalsIgnoreCase(protocol)) {
            return "http";
        } else if ("https".equalsIgnoreCase(protocol)) {
            return "https";
        } else {
            throw new ApiRuntimeException("protocol只允许http或https");
        }
    }

    /**
     * 根据协议选择对应端口，并拒绝无效端口。
     */
    private int resolvePort(String protocol, Integer httpPort, Integer httpsPort) {
        Integer port;
        if ("https".equals(protocol)) {
            port = httpsPort;
        } else {
            port = httpPort;
        }
        if (port == null || port <= 0 || port > 65535) {
            throw new ApiRuntimeException(protocol + "Port不合法");
        }
        return port;
    }

    /**
     * 只提取Informant现有认证要求的三项HMAC请求头。
     */
    private Map<String, String> resolveHmacHeader(JSONObject headerObject) {
        if (headerObject == null) {
            throw new ApiRuntimeException("header不能为空");
        }
        Map<String, String> header = new LinkedHashMap<>();
        header.put("X-Agent-Id", getHeaderIgnoreCase(headerObject, "X-Agent-Id"));
        header.put("X-Timestamp", getHeaderIgnoreCase(headerObject, "X-Timestamp"));
        header.put("X-Signature", getHeaderIgnoreCase(headerObject, "X-Signature"));
        for (Map.Entry<String, String> entry : header.entrySet()) {
            if (StringUtils.isBlank(entry.getValue())) {
                throw new ApiRuntimeException("header缺少" + entry.getKey());
            }
        }
        return header;
    }

    /**
     * 请求头名称按HTTP规范进行大小写不敏感匹配。
     */
    private String getHeaderIgnoreCase(JSONObject headerObject, String name) {
        for (String key : headerObject.keySet()) {
            if (name.equalsIgnoreCase(key)) {
                return headerObject.getString(key);
            }
        }
        return null;
    }
}
