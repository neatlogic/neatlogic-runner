package com.techsure.autoexecrunner.util;


import com.techsure.autoexecrunner.dto.RestVo;
import com.techsure.autoexecrunner.util.authtication.core.AuthenticateHandlerFactory;
import com.techsure.autoexecrunner.util.authtication.core.IAuthenticateHandler;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class RestUtil {

    protected static Logger logger = LoggerFactory.getLogger(RestUtil.class);
    static TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            return;
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            return;
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }};

    private static class NullHostNameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String paramString, SSLSession paramSSLSession) {
            return true;
        }
    }

    public static String sendRequest(RestVo restVo) {
        HttpURLConnection connection = null;
        String result = "";
        try {
            HttpsURLConnection.setDefaultHostnameVerifier(new NullHostNameVerifier());
            SSLContext sc = SSLContext.getInstance("TLSv1.2");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            URL getUrl = new URL(restVo.getUrl());
            connection = (HttpURLConnection) getUrl.openConnection();
            // 设置http method
            connection.setRequestMethod("POST");
            connection.setUseCaches(false);
            connection.setDoOutput(true);

            // 设置验证
            if (StringUtils.isNotBlank(restVo.getAuthType())) {
                IAuthenticateHandler handler = AuthenticateHandlerFactory.getHandler(restVo.getAuthType());
                if (handler != null) {
                    handler.authenticate(connection, restVo.getAuthConfig());
                }
            }

            // 设置超时时间
            connection.setConnectTimeout(0);
            connection.setReadTimeout(restVo.getTimeout());

            // 设置默认header
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            if(StringUtils.isNotBlank(restVo.getTenant())){
                connection.setRequestProperty("Tenant", restVo.getTenant());
            }

            connection.connect();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            result = e.getMessage();
        }
        if (connection != null) {
            try (DataOutputStream out = new DataOutputStream(connection.getOutputStream());) {
                if (restVo.getPayload() != null) {
                    out.write(restVo.getPayload().toJSONString().getBytes(StandardCharsets.UTF_8));
                    out.flush();
                }
                out.close();
                // 处理返回值 异常则需要获取具体异常信息
                InputStreamReader reader = null;
                if (100 <= connection.getResponseCode() && connection.getResponseCode() <= 399) {
                    reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
                } else {
                    reader = new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8);
                }
                StringWriter writer = new StringWriter();
                IOUtils.copy(reader, writer);
                result = writer.toString();
                if (100 > connection.getResponseCode() || connection.getResponseCode() > 399) {
                    logger.error(connection.getResponseCode()+":"+result);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                result = e.getMessage();
            }finally {
                connection.disconnect();
            }
        }
        return result;
    }
}
