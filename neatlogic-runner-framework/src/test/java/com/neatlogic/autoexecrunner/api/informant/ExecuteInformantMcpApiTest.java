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
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import com.neatlogic.autoexecrunner.service.informant.InformantMcpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class ExecuteInformantMcpApiTest {

    @Test
    public void rejectsUnsupportedOperationAndProtocol() {
        ExecuteInformantMcpApi api = new ExecuteInformantMcpApi(new RecordingMcpClient());
        JSONObject unsupportedOperationRequest = baseRequest();
        unsupportedOperationRequest.put("operation", "deleteTool");
        Assertions.assertThrows(ApiRuntimeException.class, () -> api.myDoService(unsupportedOperationRequest));

        JSONObject unsupportedProtocolRequest = baseRequest();
        unsupportedProtocolRequest.put("protocol", "file");
        Assertions.assertThrows(ApiRuntimeException.class, () -> api.myDoService(unsupportedProtocolRequest));

        JSONObject hostnameRequest = baseRequest();
        hostnameRequest.put("ip", "internal.example");
        Assertions.assertThrows(ApiRuntimeException.class, () -> api.myDoService(hostnameRequest));
    }

    @Test
    public void onlyForwardsRequiredHmacHeaders() throws Exception {
        RecordingMcpClient client = new RecordingMcpClient();
        ExecuteInformantMcpApi api = new ExecuteInformantMcpApi(client);
        JSONObject request = baseRequest();
        JSONObject header = request.getJSONObject("header");
        header.put("Host", "malicious.example");
        header.put("Authorization", "secret");

        api.myDoService(request);

        Assertions.assertEquals(3, client.header.size());
        Assertions.assertEquals("agent-1", client.header.get("X-Agent-Id"));
        Assertions.assertFalse(client.header.containsKey("Host"));
        Assertions.assertFalse(client.header.containsKey("Authorization"));
    }

    private JSONObject baseRequest() {
        JSONObject request = new JSONObject(true);
        request.put("operation", "listTools");
        request.put("protocol", "http");
        request.put("ip", "127.0.0.1");
        request.put("httpPort", 8080);
        JSONObject header = new JSONObject(true);
        header.put("x-agent-id", "agent-1");
        header.put("x-timestamp", "10001");
        header.put("x-signature", "signature-1");
        request.put("header", header);
        return request;
    }

    private static class RecordingMcpClient extends InformantMcpClient {
        private Map<String, String> header;

        @Override
        public JSONObject listTools(String protocol, String ip, int port, Map<String, String> header) {
            this.header = header;
            return new JSONObject(true);
        }
    }
}
