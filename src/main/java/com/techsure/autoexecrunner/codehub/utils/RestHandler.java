package com.techsure.autoexecrunner.codehub.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.common.config.Config;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 注意点：
 * 1、HTTP请求的返回值类型是不确定的，因接口的不同而不同，所以抽象出来的公共方法返回值一律为Object
 * 2、包含哪些请求头以参数方式传入
 * 3、请求方法以参数方式传入
 * @author zouye
 *
 */
public class RestHandler {
	private static Logger logger = LoggerFactory.getLogger(RestHandler.class);

	private static TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
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
	} };

	public static Object get(String url) {
		return get(url, null);
	}

	public static Object get(String url, Map<String, String> headers) {
		HttpURLConnection conn = null;
		try {
			conn = getURLConnectoin(url, "GET", headers);
			return getResponse(conn);
		} catch (IOException e) {
			if (e instanceof UnknownHostException){
				throw new RuntimeException("URL地址访问错误: "+e.getMessage());
			}
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e.getMessage());
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	private static Object getResponse(HttpURLConnection conn/*, int responseType*/) {
		StringBuilder result = new StringBuilder();
		int code = 200;
		String line;
		BufferedReader reader;
		try {
			code = conn.getResponseCode();
			if (code >= 200 && code < 400) {
				reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), String.valueOf(StandardCharsets.UTF_8)));
				while ((line = reader.readLine()) != null) {
					result.append(line);
				}
			} else {
				reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), String.valueOf(StandardCharsets.UTF_8)));
				while ((line = reader.readLine()) != null) {
					result.append(line);
				}
				String errMsg = result.toString();
				try {
					JSONObject resObj = JSONObject.parseObject(result.toString());
					if (resObj.containsKey("message")){
						errMsg = resObj.getString("message") + " ";
					}
					if (resObj.containsKey("error")){
						errMsg = resObj.getString("error") + " ";
					}
					throw new RuntimeException(errMsg);
				} catch (Exception e) {
					// 非json类型的返回, 为了避免返回一大堆错误信息, 比如html 404页面的代码, 此处进行长度缩减
					logger.error(errMsg);
					throw new RuntimeException("Server response " + StringUtils.substring(errMsg, 0 , 1000));
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Server error " + e.getMessage());
		}
		
		return result.toString();
	}

	public static Object post(String url, JSONObject jsonObj, Map<String, String> headers) {
		HttpURLConnection conn = null;

		try {
			conn = getURLConnectoin(url, "POST", headers);
			DataOutputStream out = new DataOutputStream(conn.getOutputStream());
			out.write(jsonObj.toString().getBytes(String.valueOf(StandardCharsets.UTF_8))); //解决中文乱码
			out.flush();
			
			return getResponse(conn);
		} catch (IOException e) {
			if (e instanceof UnknownHostException){
				throw new RuntimeException("URL地址访问错误: "+e.getMessage());
			}
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e.getMessage());
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	public static Object put(String url, JSONObject jsonObj, Map<String, String> headers) {
		HttpURLConnection conn = null;

		try {
			conn = getURLConnectoin(url, "PUT", headers);
			DataOutputStream out = new DataOutputStream(conn.getOutputStream());
			out.write(jsonObj.toString().getBytes(String.valueOf(StandardCharsets.UTF_8))); //解决中文乱码
			out.flush();
			
			return getResponse(conn);
		} catch (IOException e) {
			if (e instanceof UnknownHostException){
				throw new RuntimeException("URL地址访问错误: "+e.getMessage());
			}
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e.getMessage());
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}
	

	public static Object delete(String url, JSONObject jsonObj, Map<String, String> headers) {
		HttpURLConnection conn = null;

		try {
			conn = getURLConnectoin(url, "DELETE", headers);
			DataOutputStream out = new DataOutputStream(conn.getOutputStream());
            out.write(jsonObj.toString().getBytes(String.valueOf(StandardCharsets.UTF_8))); //解决中文乱码
			out.flush();
			
			return getResponse(conn);
		} catch (IOException e) {
			if (e instanceof UnknownHostException){
				throw new RuntimeException("URL地址访问错误: "+e.getMessage());
			}
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e.getMessage());
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	private static HttpURLConnection getURLConnectoin(String Url, String method, Map<String, String> headers) throws IOException {
		HttpsURLConnection.setDefaultHostnameVerifier(new NullHostNameVerifier());
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (NoSuchAlgorithmException | KeyManagementException ex) {
			ex.printStackTrace();
		}

		URL getUrl = new URL(Url);
		HttpURLConnection connection = (HttpURLConnection) getUrl.openConnection();

		connection.setUseCaches(false);

		if (!"GET".equals(method)) {
			connection.setDoOutput(true);
		}

		connection.setDoInput(true);
		connection.setRequestMethod(method);

		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Accept-Charset", String.valueOf(StandardCharsets.UTF_8));
		connection.setRequestProperty("Accept", "text/plain, application/json, */*");
		connection.setRequestProperty("Charset", String.valueOf(StandardCharsets.UTF_8));
		connection.setConnectTimeout(Config.CONNECTION_CONNECT_TIMEOUT*1000);// 设置连接超时
		connection.setReadTimeout(Config.CONNECTION_READ_TIMEOUT*1000);//设置读超时
		if (headers != null && !headers.isEmpty()) {
			for (String key: headers.keySet()) {
				connection.setRequestProperty(key, headers.get(key));
			}
		}
		connection.connect();

		return connection;
	}

	public static class NullHostNameVerifier implements HostnameVerifier {
		@Override
		public boolean verify(String paramString, SSLSession paramSSLSession) {
			return true;
		}
	}

	public static String encodeHtml(String str) {
		if (StringUtils.isNotBlank(str)) {
			str = str.replace("<", "&lt;");
			str = str.replace(">", "&gt;");
			return str;
		}
		return "";
	}
	
	public static void savePaginationHeader(HttpURLConnection conn, String headers, JSONObject retObj, String name) {
		int x = conn.getHeaderFieldInt(headers, -1);
		if (x != -1) {
			retObj.put(name, x);
		}
	}
	
	/**
	 * 对 gitlab 接口调用优化, 需要记录目标返回的headers字段, 读取分页数据, 而且不返回Object类型, 封装成JSONObject
	 * @param method
	 * @param url 
	 * @param headers
	 * @param jsonObj
	 * @return JSONObject
	 */
	public static JSONObject httpRest(String method, String url, Map<String, String> headers, JSONObject jsonObj) {
		HttpURLConnection conn = null;
		JSONObject retObj;

		try {
			conn = getURLConnectoin(url, method, headers);
			if (!method.equalsIgnoreCase("GET") && jsonObj != null) {
				DataOutputStream out = new DataOutputStream(conn.getOutputStream());
				out.write(jsonObj.toString().getBytes(StandardCharsets.UTF_8)); //解决中文乱码
				out.flush();
			}
			String data = (String) getResponse(conn);
			data = data.trim();
			if (StringUtils.isEmpty(data)) {
				retObj = new JSONObject();
			} else {
				
				Object object = JSON.parse(data);
				if (object instanceof JSONObject) {
					retObj = (JSONObject) object;
				} else if (object instanceof JSONArray) {

					retObj = new JSONObject();
					retObj.put("list", object);

					// 如果是带分页的接口, 需要把总页数等返回, gitlab是存在header里面的, 在这里直接也用header不太方便
					savePaginationHeader(conn, "X-Total", retObj, "total");
					savePaginationHeader(conn, "X-Total-Pages", retObj, "totalPages");
					savePaginationHeader(conn, "X-Per-Page", retObj, "perPage");
					savePaginationHeader(conn, "X-Page", retObj, "page");
					savePaginationHeader(conn, "X-Next-Page", retObj, "nextPage");
					savePaginationHeader(conn, "X-Prev-Page", retObj, "prevPage");

				} else {
					logger.error(data);
					throw new JSONException(data);
				}
			}

			return retObj;
		} catch (IOException e) {
			if (e instanceof UnknownHostException){
				throw new RuntimeException("URL地址访问错误: "+e.getMessage());
			}
			logger.error(e.getMessage(), e);
			throw new RuntimeException(e.getMessage());
		}  catch (Exception e) {
			String message = encodeHtml(e.getMessage());
			logger.error(e.getMessage(), e);
			if (message.startsWith("Server response ")) {
				message = StringUtils.removeStart(message, "Server response ");
				try {
					retObj = JSONObject.parseObject(message);
				} catch (Exception e2) {
					throw new RuntimeException(message);
				}
				if (retObj.containsKey("message")) {
					throw new RuntimeException(retObj.getString("message") + " ");
				}
				if (retObj.containsKey("error")) {
					throw new RuntimeException(retObj.getString("error") + " ");
				}
			}
			throw new RuntimeException(message);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}
	
	
}
