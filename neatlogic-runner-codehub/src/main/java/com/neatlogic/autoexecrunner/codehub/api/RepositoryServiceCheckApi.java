package com.neatlogic.autoexecrunner.codehub.api;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.codehub.utils.JSONUtils;
import com.neatlogic.autoexecrunner.restful.annotation.Description;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.stereotype.Service;


@Service
public class RepositoryServiceCheckApi extends PrivateApiComponentBase {

	@Override
	public String getToken() {
		return "codehub/repository/servicecheck";
	}

	@Override
	public String getName() {
		return "仓库服务检验接口";
	}

	/* 检测服务是否可用 */
	@Input({
			@Param(name = "type", type = ApiParamType.STRING, desc = "仓库类型"),
			@Param(name = "address", type = ApiParamType.STRING, desc = "地址")
	})
	@Description(desc = "仓库服务校验接口")
	@Override
	public Object myDoService(JSONObject config) {
		String type = JSONUtils.optString(config,"type", "").trim();
		String address = JSONUtils.optString(config,"address", "").trim();

		if (StringUtils.isBlank(type) || (!"svn".equalsIgnoreCase(type) && !"gitlab".equalsIgnoreCase(type))) {
			throw new ApiRuntimeException("请指定仓库服务类型, 支持的类型为 svn、gitlab");
		}

		if (StringUtils.isBlank(address)) {
			throw new ApiRuntimeException("请指定仓库服务地址");
		}

		String errMsg = "";
		JSONObject jsonObj = new JSONObject();
		if (!address.startsWith("http://") && !address.startsWith("https://")) {
			errMsg = "only support http/https protocol.\n";
			jsonObj.put("checkStatus", "failed");
			jsonObj.put("Message", errMsg);
			return jsonObj;
		}

		CloseableHttpClient httpClient = httpSslClient();
		try {
			// HTTP请求
			HttpGet httpGet = new HttpGet(address);
			RequestConfig requestConfig = RequestConfig.custom()
					.setConnectTimeout(30 * 1000)// 设置连接超时时间，单位毫秒
					.setConnectionRequestTimeout(30 * 1000)// 设置从connect Manager获取Connection 超时时间，单位毫秒
					.setSocketTimeout(30 * 1000).build();// 请求获取数据的超时时间，单位毫秒
			httpGet.setConfig(requestConfig);
			// 发送请求，返回响应
			CloseableHttpResponse response = httpClient.execute(httpGet);
			// 获取响应信息
			int resCode = response.getStatusLine().getStatusCode();
			if (resCode == 500 || resCode == 404) {
				errMsg = "server response code: " + resCode + ".\n";
				jsonObj.put("checkStatus", "failed");
				jsonObj.put("Message", errMsg);
			} else {
				jsonObj.put("checkStatus", "succeed");
				jsonObj.put("Message", errMsg);
			}
		} catch (ClientProtocolException e) {
			// 协议错误
			jsonObj.put("checkStatus", "failed");
			jsonObj.put("Message", e.getMessage());
			return jsonObj;
		} catch (UnknownHostException e) {
			// 未知主机
			jsonObj.put("checkStatus", "failed");
			jsonObj.put("Message", "未知主机: " + e.getMessage());
			return jsonObj;
		} catch (IOException e) {
			// 网络异常
			jsonObj.put("checkStatus", "failed");
			jsonObj.put("Message", e.getMessage());
			return jsonObj;
		}

		return jsonObj;
	}

	/**
	 * 能正常访问https的client
	 * @return
	 */
	private static CloseableHttpClient httpSslClient() {
		try {
			// @Yujh add nossl valid
			SSLContext ctx = SSLContext.getInstance(SSLConnectionSocketFactory.TLS);
			ctx.init(null, new TrustManager[] { new X509TrustManager() {
				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				@Override
				public void checkClientTrusted(X509Certificate[] xcs, String str) {
				}

				@Override
				public void checkServerTrusted(X509Certificate[] xcs, String str) {
				}
			} }, null);
			SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(ctx, NoopHostnameVerifier.INSTANCE);
			RequestConfig requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD_STRICT).setExpectContinueEnabled(Boolean.TRUE)
					.setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM, AuthSchemes.DIGEST)).setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC)).build();
			// 设置允许认证
			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create().register("http", PlainConnectionSocketFactory.INSTANCE)
					.register("https", socketFactory).build();
			PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
			CloseableHttpClient closeableHttpClient = HttpClients.custom().setConnectionManager(connectionManager).setDefaultRequestConfig(requestConfig).build();
			return closeableHttpClient;
		} catch (Exception ex) {
			throw new ApiRuntimeException(ex);
		} 
	}



/*
	@Override
	public JSONArray help() {
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObj;

		jsonObj = new JSONObject();
		jsonObj.put("name", "type");
		jsonObj.put("type", "String");
		jsonObj.put("desc", "仓库服务类型, gitlab or svn");
		jsonArray.add(jsonObj);

		jsonObj = new JSONObject();
		jsonObj.put("name", "address");
		jsonObj.put("type", "String");
		jsonObj.put("desc", "仓库服务类型地址");
		jsonArray.add(jsonObj);

		return jsonArray;
	}
*/
}
