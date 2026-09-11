package com.neatlogic.autoexecrunner.api.informant;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.util.HttpRequestUtil;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class InformantForwardResponseTest {

    @Test
    public void forwardEnvelopePreservesStatusContentTypeAndBody() {
        String body = "{\"error\":{\"code\":\"source_not_found\",\"message\":\"missing\"}}";

        JSONObject response = SendRequestToInformantApi.buildForwardResponse(404, "application/json", body);

        Assertions.assertTrue(response.getBooleanValue(SendRequestToInformantApi.FORWARD_RESPONSE_MARKER));
        Assertions.assertEquals(404, response.getIntValue("statusCode"));
        Assertions.assertEquals("application/json", response.getString("contentType"));
        Assertions.assertEquals(body, response.getString("body"));
    }

    @Test
    public void httpRequestKeepsRawDownstreamErrorBody() throws Exception {
        String body = "{\"error\":{\"code\":\"source_not_found\",\"message\":\"missing\"}}";
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/error", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(404, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            HttpRequestUtil request = HttpRequestUtil
                    .get("http://127.0.0.1:" + server.getAddress().getPort() + "/error")
                    .sendRequest();

            Assertions.assertEquals(404, request.getResponseCode());
            Assertions.assertEquals(body, request.getErrorResponseBody());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void downloadErrorContentTypeIsRestricted() {
        Assertions.assertEquals(
                "application/problem+json; charset=UTF-8",
                DownloadRequestFromInformantApi.safeErrorContentType("application/problem+json; charset=UTF-8"));
        Assertions.assertEquals(
                "application/json",
                DownloadRequestFromInformantApi.safeErrorContentType("application/octet-stream"));
    }
}
