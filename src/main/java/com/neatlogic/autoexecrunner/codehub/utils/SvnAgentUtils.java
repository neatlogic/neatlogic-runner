package com.neatlogic.autoexecrunner.codehub.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import com.neatlogic.autoexecrunner.util.HttpRequestUtil;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author fengt
 * 
 *         SVN代理工具类
 *
 */
public class SvnAgentUtils {
    private static Logger logger = LoggerFactory.getLogger(SvnAgentUtils.class);

    private static final Base64.Encoder base64Encoder = Base64.getEncoder();

    /**
     * 创建仓库(并将仓库根目录rw权限授予创建用户)
     * @param jsonObj
     * @return
     */
    public static void createRepo(JSONObject jsonObj) {
        String createRepoUserId = jsonObj.getString("userId");
        String agentUrl = jsonObj.getString("agentUrl");
        String username = jsonObj.getString("agentUsername");
        String password = jsonObj.getString("agentPassword");
        String repoPath = jsonObj.getString("repoPath");

        if(StringUtils.isEmpty(createRepoUserId)){
            throw new ApiRuntimeException("参数userId不能为空");
        }
        if(StringUtils.isEmpty(agentUrl)){
            throw new ApiRuntimeException("参数agentUrl不能为空");
        }
        if(StringUtils.isEmpty(username)){
            throw new ApiRuntimeException("参数agentUsername不能为空");
        }
        if(StringUtils.isEmpty(password)){
            throw new ApiRuntimeException("参数agentPassword不能为空");
        }
        if(StringUtils.isEmpty(repoPath)){
            throw new ApiRuntimeException("参数repoPath不能为空");
        }
        String url = "";
        if(StringUtils.endsWith(agentUrl,"/")){
            url = agentUrl + "createrepo.py";
        }
        else{
            url = agentUrl + "/createrepo.py";
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("path",repoPath);

        JSONObject createRepoReturnJsonObj = JSONObject.parseObject(post(url, jsonObject, getAuthHeader(username, password)));
        String status = createRepoReturnJsonObj.getString("status");
        if(StringUtils.equals(status,"OK")){
            //设置repo的owner为userId,将仓库下的根目录的rw权限授予用户userId
            JSONObject putAuthParamJsonObj = new JSONObject();
            putAuthParamJsonObj.put("action","write");
            putAuthParamJsonObj.put("path",repoPath);

            JSONArray authlistJsonArray = new JSONArray();
            JSONObject authJsonObj = new JSONObject();
            authJsonObj.put("path","/");

            JSONArray accJsonArray = new JSONArray();
            JSONObject accJsonObj = new JSONObject();
            accJsonObj.put("userid",createRepoUserId);
            accJsonObj.put("acc","rw");
            accJsonArray.add(accJsonObj);

            authJsonObj.put("acclist",accJsonArray);

            authlistJsonArray.add(authJsonObj);

            putAuthParamJsonObj.put("authlist",authlistJsonArray);

            JSONObject putAuthReturnJsonObject = putAuth(agentUrl,username,password,putAuthParamJsonObj);
            String putAuthStatus = putAuthReturnJsonObject.getString("status");
            if(StringUtils.equals(putAuthStatus,"OK")){
                logger.info("仓库:"+repoPath+" 给用户:" + createRepoUserId + " 授权成功");
            }
            else{
                logger.error("仓库授权失败，错误信息：" + putAuthReturnJsonObject);
                throw new ApiRuntimeException("仓库授权失败，错误信息：" + putAuthReturnJsonObject);
            }
        }
        else{
            logger.error("创建仓库失败，错误信息：" + createRepoReturnJsonObj);
            throw new ApiRuntimeException("创建仓库失败，错误信息：" + createRepoReturnJsonObj);
        }
    }

    /**
     * 根据groupid查询组列表(区分大小写)
     * @param agentUrl
     * @param username
     * @param password
     * @param groupid
     * @return
     */
    public static JSONObject getGroup(String agentUrl, String username, String password, String groupid) {
        String url = "";
        if(StringUtils.endsWith(agentUrl,"/")){
            url = agentUrl + "getgroup.py";
        }
        else{
            url = agentUrl + "/getgroup.py";
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("groupid",groupid);

        Map<String,String> headers = getAuthHeader(username, password);
        HttpRequestUtil httpRequestUtil = HttpRequestUtil.post(url);
        if (MapUtils.isNotEmpty(headers)) {
            for (String key : headers.keySet()) {
                httpRequestUtil.addHeader(key, headers.get(key));
            }
        }
        httpRequestUtil.setPayload(jsonObject.toJSONString());
        HttpRequestUtil requestPost = httpRequestUtil.sendRequest();
        return requestPost.getResultJson();
        //return (JSONObject) JSONObject.toJSON(RestHandler.post(url, jsonObject, getAuthHeader(username, password)));
    }

    /**
     * 取组成员接口
     * @param agentUrl
     * @param username
     * @param password
     * @param groupid 组名称
     * @return
     */
    public static JSONObject getMember(String agentUrl, String username, String password, String groupid, String keyword) {
        String url = "";
        if(StringUtils.endsWith(agentUrl,"/")){
            url = agentUrl + "getmember.py";
        }
        else{
            url = agentUrl + "/getmember.py";
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("groupid", groupid);
        jsonObject.put("keyword", keyword);

        Map<String,String> headers = getAuthHeader(username, password);
        HttpRequestUtil httpRequestUtil = HttpRequestUtil.post(url);
        if (MapUtils.isNotEmpty(headers)) {
            for (String key : headers.keySet()) {
                httpRequestUtil.addHeader(key, headers.get(key));
            }
        }
        httpRequestUtil.setPayload(jsonObject.toJSONString());
        HttpRequestUtil requestPost = httpRequestUtil.sendRequest();
        return requestPost.getResultJson();
        //return (JSONObject) JSONObject.toJSON(RestHandler.post(url, jsonObject, getAuthHeader(username, password)));
    }


     /**
     * 读权限接口
     * @param agentUrl
     * @param username
     * @param password
     * @param path 文件或者目录子路径，必须包含repository， /svn/可以没有，支持多层/
     * @return
     */
    public static JSONObject getAuth(String agentUrl, String username, String password, String path) throws UnsupportedEncodingException {
        String url = "";
        if(StringUtils.endsWith(agentUrl,"/")){
            url = agentUrl + "getauth.py";
        }
        else{
            url = agentUrl + "/getauth.py";
        }
        url = url + "?path=" + URLEncoder.encode(path,"utf-8");


        return JSONObject.parseObject(get(url, getAuthHeader(username, password)));
    }

    
    public static boolean getDelegation(JSONObject jsonObj) throws UnsupportedEncodingException {
        String userId = jsonObj.getString("userId");
        String agentUrl = jsonObj.getString("agentUrl");
        String username = jsonObj.getString("agentUsername");
        String password = jsonObj.getString("agentPassword");
        String repo = jsonObj.getString("repo");
        
        if(StringUtils.isEmpty(userId)){
            throw new ApiRuntimeException("参数userId不能为空");
        }
        if(StringUtils.isEmpty(agentUrl)){
            throw new ApiRuntimeException("参数agentUrl不能为空");
        }
        if(StringUtils.isEmpty(username)){
            throw new ApiRuntimeException("参数agentUsername不能为空");
        }
        if(StringUtils.isEmpty(password)){
            throw new ApiRuntimeException("参数agentPassword不能为空");
        }
        String url = "";
        if(StringUtils.endsWith(agentUrl,"/")){
            url = agentUrl + "getdelegation.py";
        }
        else{
            url = agentUrl + "/getdelegation.py";
        }
        url = url + "?repo=" + URLEncoder.encode(repo,"utf-8") + "&userid=" + URLEncoder.encode(userId,"utf-8") ;


        JSONObject jsonObject =  JSONObject.parseObject(get(url, getAuthHeader(username, password)));
        
        if (StringUtils.equals(jsonObject.getString("status"), "OK")) {
            if (jsonObject.containsKey("content")) {
                return jsonObject.getBoolean("content");
            }
        } else {
            throw new ApiRuntimeException("调用svn代理出错：" + jsonObject.toString());
        }
        return false;
    }
    
    
    /**
     * 读仓库权限接口
     * @param agentUrl
     * @param username
     * @param password
     * @param repo 仓库名称
     * @return
     */
    public static JSONObject getRepoAuth(String agentUrl, String username, String password, String repo) throws UnsupportedEncodingException {
        String url = "";
        if(StringUtils.endsWith(agentUrl,"/")){
            url = agentUrl + "getrepoauth.py";
        }
        else{
            url = agentUrl + "/getrepoauth.py";
        }
        url = url + "?repo=" + URLEncoder.encode(repo,"utf-8");


        return JSONObject.parseObject(get(url, getAuthHeader(username, password)));
    }

    /**
     * 写权限接口
     * @param agentUrl
     * @param username
     * @param password
     * @param jsonObject 格式参考：
     * {
     * 	"action": "write",
     * 	"path": "/svn/SVNRepository36/财险核心系统v5/",
     * 	"authlist": [{
     * 			"path": "/实施/",
     * 			"acclist": [{
     * 					"userid": "WB20200507011",
     * 					"acc": "r"
     *                                },
     *                {
     * 					"userid": "DBA组",
     * 					"acc": "rw"
     *                }
     * 			]
     * 		},
     * 		{
     * 			"path": "/财险核心系统v5/实施",
     * 			"acclist": [{
     * 					"userid": "WB20200507011",
     * 					"acc": "r"
     *                },
     *                {
     * 					"userid": "DBA组",
     * 					"acc": "rw"
     *                }
     * 			]
     * 		}
     * 	]
     * }
     * 说明：
     * 写权限接口以authlist.path为主键，以authlist.acclist的内容完全覆盖该路径的权限配置。
     * @return
     */
    public static JSONObject putAuth(String agentUrl, String username, String password, JSONObject jsonObject) {
        String url = "";
        if(StringUtils.endsWith(agentUrl,"/")){
            url = agentUrl + "putauth.py";
        }
        else{
            url = agentUrl + "/putauth.py";
        }

        return JSONObject.parseObject(post(url, jsonObject, getAuthHeader(username, password)));
    }

    private static Map<String,String> getAuthHeader(String username, String password){
        Map<String,String> headers = new HashMap<String,String>();
        String authStr = username + ":" + password;
        String b64AuthStr = "Basic " + base64Encoder.encodeToString(authStr.getBytes());
        headers.put("authorization",b64AuthStr);
        headers.put("Accept", "application/json");
        return headers;
    }

    public static String get(String url, Map<String, String> headers) {
        JSONObject retObj;
        try {
            HttpRequestUtil httpRequestUtil = HttpRequestUtil.get(url);
            if (MapUtils.isNotEmpty(headers)) {
                for (String key : headers.keySet()) {
                    httpRequestUtil.addHeader(key, headers.get(key));
                }
            }
            HttpRequestUtil requestGet = httpRequestUtil.sendRequest();
            return requestGet.getResult();
            //return (String)RestHandler.get(url, headers);
        } catch (Exception e) {
            String message = encodeHtml(e.getMessage());

            if (message.startsWith("Server response ")) {
                try {
                    retObj = JSONObject.parseObject(StringUtils.removeStart(message, "Server response "));
                } catch (Exception e2) {
                    throw new ApiRuntimeException(message);
                }
                if (retObj.containsKey("message")) {
                    throw new ApiRuntimeException(retObj.getString("message") + " ");
                }
            }
            throw new ApiRuntimeException(message);
        }
    }

    public static String post(String url, JSONObject jsonObject, Map<String, String> headers) {
        JSONObject retObj;
        try {
            HttpRequestUtil httpRequestUtil = HttpRequestUtil.post(url);
            if (MapUtils.isNotEmpty(headers)) {
                for (String key : headers.keySet()) {
                    httpRequestUtil.addHeader(key, headers.get(key));
                }
            }
            if(jsonObject != null){
                httpRequestUtil.setPayload(jsonObject.toJSONString());
            }
            HttpRequestUtil requestPost = httpRequestUtil.sendRequest();
            return requestPost.getResult();
            //return (String)RestHandler.post(url, jsonObject, headers);
        } catch (Exception e) {
            String message = encodeHtml(e.getMessage());

            if (message.startsWith("Server response ")) {
                try {
                    retObj = JSONObject.parseObject(StringUtils.removeStart(message, "Server response "));
                } catch (Exception e2) {
                    throw new ApiRuntimeException(message);
                }
                if (retObj.containsKey("message")) {
                    throw new ApiRuntimeException(retObj.getString("message") + " ");
                }
            }
            throw new ApiRuntimeException(message);
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

}
