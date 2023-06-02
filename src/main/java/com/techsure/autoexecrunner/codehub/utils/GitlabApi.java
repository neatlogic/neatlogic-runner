package com.techsure.autoexecrunner.codehub.utils;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;
import com.techsure.autoexecrunner.util.HttpRequestUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author yujh
 *
 * Gitlab调用api对象
 *
 *
 * 封装取token,自动拼接url,继承分页条件
 * 获取和创建project
 * 获取group等功能
 */
public class GitlabApi {

    private static Logger logger = LoggerFactory.getLogger( GitlabApi.class);
    private String gitLabServerUrl;
    private String token;
    private String tokenType;
    private String repoPath;
    private Integer per_page;
    private Integer page;
    private Map<String, String> headers;
    /**
     * @param jsonObj
     * username
     * password
     * repositoryServiceAddress
     * credentialType
     */
    public GitlabApi(JSONObject jsonObj) {

        this.repoPath = jsonObj.getString("repoPath");
        String username = jsonObj.getString("username");
        String password = jsonObj.getString("password");
        String gitlabUrl = jsonObj.getString("repositoryServiceAddress");
        String credentialType = jsonObj.getString("credentialType");

        this.gitLabServerUrl = gitlabUrl;
        if ("password".equalsIgnoreCase(credentialType)) {
            if (username.isEmpty()) {
                throw new ApiRuntimeException("gitlab用户名不能为空");
            }
            if (password.isEmpty()) {
                throw new ApiRuntimeException("gitlab密码不能为空");
            }
            JSONObject resObj = getGitLabAccessToken(username, password);
            this.token = resObj.getString("access_token");
            this.tokenType = "access_token";
        } else {
            if (password.isEmpty()) {
                throw new ApiRuntimeException("gitlab token不能为空");
            }
            this.token = password;
            this.tokenType = "private_token";
        }

        if (jsonObj.containsKey("per_page")) {
            // gitlab api的分页条数
            per_page = jsonObj.getIntValue("per_page");
        }else if (jsonObj.containsKey("perPage")) {
            // gitlab api的分页条数
            per_page = jsonObj.getIntValue("perPage");
        }
        if (jsonObj.containsKey("page")) {
            // gitlab api的分页条数
            page = jsonObj.getIntValue("page");
        }
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String encodeURL(String url) {
        try {
            return URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException();
        }
    }
    
    public JSONObject httpGet(String api, Map<String, Object> params) {
        JSONObject param = new JSONObject();
        HttpRequestUtil httpRequestUtil = HttpRequestUtil.get(formatQueryParams(this.gitLabServerUrl + api, params));
        if (MapUtils.isNotEmpty(this.headers)) {
            for (String key : this.headers.keySet()) {
                httpRequestUtil.addHeader(key, this.headers.get(key));
            }
        }
        HttpRequestUtil requestGet = httpRequestUtil.sendRequest();
        //return RestHandler.httpRest("GET", formatQueryParams(this.gitLabServerUrl + api, params), headers, null);
        return handleGitLabResult(requestGet.getResponseHeadersMap(),requestGet.getResult());
    }

    /**
     * 有些接口是会分页的, 为了实现某些需求需要保证全部返回
     * 这个方法是用来保证取得所有不分页的数据
     * @return
     */
    public JSONArray httpGetNotPagination(String api, Map<String, Object> params){
        if (params == null) {
            params = new HashMap<>();
        }
        params.put("per_page", 50);
        JSONArray retObj = new JSONArray();
        for (int i = 1;; i++) {
            params.put("page", i);
            JSONObject data = httpGet(api, params);
            JSONArray array = data.getJSONArray("list");
            if (array.size() == 0 || !data.containsKey("totalPages")) {
                break;
            }
            retObj.addAll(array);
            if (data.getIntValue("totalPages") <= i) {
                break;
            }
        }
        return retObj;
    }
    
    public JSONObject httpPost(String api, Map<String, Object> params, JSONObject json) {
        HttpRequestUtil httpRequestUtil = HttpRequestUtil.post(formatQueryParams(this.gitLabServerUrl + api, params));
        if (MapUtils.isNotEmpty(this.headers)) {
            for (String key : this.headers.keySet()) {
                httpRequestUtil.addHeader(key, this.headers.get(key));
            }
        }
        if(json != null){
            httpRequestUtil.setPayload(json.toJSONString());
        }
        HttpRequestUtil requestPost = httpRequestUtil.sendRequest();
        //return RestHandler.httpRest("POST", formatQueryParams(this.gitLabServerUrl + api, params), headers, json);
        return handleGitLabResult(requestPost.getResponseHeadersMap(),requestPost.getResult());
    }

    public JSONObject httpPut(String api, Map<String, Object> params, JSONObject json) {
        HttpRequestUtil httpRequestUtil = HttpRequestUtil.put(formatQueryParams(this.gitLabServerUrl + api, params));
        if (MapUtils.isNotEmpty(this.headers)) {
            for (String key : this.headers.keySet()) {
                httpRequestUtil.addHeader(key, this.headers.get(key));
            }
        }
        if(json != null){
            httpRequestUtil.setPayload(json.toJSONString());
        }
        HttpRequestUtil requestPut = httpRequestUtil.sendRequest();
        //return RestHandler.httpRest("PUT", formatQueryParams(this.gitLabServerUrl + api, params), headers, json);
        return handleGitLabResult(requestPut.getResponseHeadersMap(),requestPut.getResult());
    }

    public void httpDelete(String api, Map<String, Object> params, JSONObject json) {
        HttpRequestUtil httpRequestUtil = HttpRequestUtil.delete(formatQueryParams(this.gitLabServerUrl + api, params));
        if (MapUtils.isNotEmpty(this.headers)) {
            for (String key : this.headers.keySet()) {
                httpRequestUtil.addHeader(key, this.headers.get(key));
            }
        }
        if(json != null){
            httpRequestUtil.setPayload(json.toJSONString());
        }
        HttpRequestUtil requestDelete = httpRequestUtil.sendRequest();
        //RestHandler.httpRest("DELETE", formatQueryParams(this.gitLabServerUrl + api, params), headers, json);
        handleGitLabResult(requestDelete.getResponseHeadersMap(),requestDelete.getResult());
    }

    public String formatQueryParams(String url, Map<String, Object> map) {
        if (map == null) {
            map = new HashMap<>();
        }
        map.put(tokenType, token);
        if (per_page != null && !map.containsKey("per_page")) {
            map.put("per_page", per_page);
        }
        if (page != null && !map.containsKey("page")) {
            map.put("page", page);
        }
        final Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
        final StringBuilder sb = new StringBuilder(map.size() * 8);
        while (it.hasNext()) {
            final Map.Entry<String, Object> entry = it.next();
            final String key = entry.getKey();
            if (key != null) {
                sb.append(encodeURL(key));
                sb.append('=');
                final Object value = entry.getValue();
                final String valueAsString = value != null ? encodeURL(value.toString()) : "";
                sb.append(valueAsString);
                if (it.hasNext()) {
                    sb.append('&');
                }
            }
        }
        return url.contains("?") ? url + "&" + sb.toString() : url + "?" + sb.toString();
    }

    /**
     * 根据gitlab用户名&密码获取access_token
     *
     * @param userName
     * @param password
     * @return
     */
    public JSONObject getGitLabAccessToken(String userName, String password) {
        JSONObject userObj = new JSONObject();
        userObj.put("grant_type", "password");
        userObj.put("username", userName);
        userObj.put("password", password);

        return httpPost("/oauth/token", null, userObj);
    }

    /**
     * 列出所有项目组名称, 包含namespace内的
     * 
     * @return
     */
    public JSONArray listAllGroups() {
        JSONArray retObj = httpGetNotPagination("/api/v4/groups", null);
        // namespace可能会包含一部分group
        JSONArray groups = httpGetNotPagination("/api/v4/namespaces", null);
        retObj.addAll(groups);
        return retObj;
    }


    /**
     * 列出单个用户的信息
     * <pre>
     * {
     *   "id": 1,
     *   "username": "john_smith",
     *   "name": "John Smith",
     *   "state": "active",
     *   "avatar_url": "http://localhost:3000/uploads/user/avatar/1/cd8.jpeg",
     *   "web_url": "http://localhost:3000/john_smith",
     *   "created_at": "2012-05-23T08:00:58Z",
     *   "bio": null,
     *   "location": null,
     *   "public_email": "john@example.com",
     *   "skype": "",
     *   "linkedin": "",
     *   "twitter": "",
     *   "website_url": "",
     *   "organization": ""
     * }
     * </pre>
     * 
     * @param id 
     * @return
     */
    public JSONObject getUser(int id) {
        return httpGet("/api/v4/users/" + id, null);
    }
    
    public JSONObject getCurrentUser() {
        return httpGet("/api/v4/user", null);
    }
    
    
    
    public JSONObject getGroup(int id) {
        return httpGet("/api/v4/groups/" + id + "?with_projects=false", null);
    }
    
    public JSONObject getGroup(String groupName) {
        return httpGet("/api/v4/groups/" + encodeURL(groupName) + "?with_projects=false", null);
    }
    
    /**
     * 列出所有用户信息, 不带分页
     *
     * @return
     */
    public JSONArray listAllUsers() {
        return httpGetNotPagination("/api/v4/users?active=true", null);
    }

    /**
     * 搜索用户列表, 支持分页
     * 分页数采用gitlab默认条数, 可用于下拉框选择用户的场景
     * @return 
     */
    public JSONObject searchUsers(String search) {
        return httpGet("/api/v4/users?active=true&search=" + encodeURL(search), null);
    }

    /**
     * 搜索项目组列表, 支持分页
     * 分页数采用gitlab默认条数, 可用于下拉框选择项目组的场景
     * @return
     */
    public JSONObject searchGroups(String search) {
        return httpGet("/api/v4/groups?search=" + encodeURL(search), null);
    }

    public boolean existsProject(String path) {
        String[] paths = path.split("/");
        Map<String, Object> params = new HashMap<>();
        params.put("search", paths[paths.length - 1]);
        JSONArray projects = httpGetNotPagination("/api/v4/projects", params);
        for (int j = 0; j < projects.size(); j++) {
            if (projects.getJSONObject(j).getString("path_with_namespace").equalsIgnoreCase(path)) {
                return true;
            }
        }
        return false;
    }

    public JSONObject createProject(JSONObject json) {
        return httpPost("/api/v4/projects", null, json);
    }

    public JSONObject getCurrentProject() {
        return httpGet("/api/v4/projects/" + encodeURL(repoPath), null);
    }
    
    public JSONObject searchBranches() {
        return httpGet("/api/v4/projects/" + encodeURL(repoPath) + "/repository/branches", null);
    }
    
    public JSONArray listBranches() {
        return httpGetNotPagination("/api/v4/projects/" + encodeURL(repoPath) + "/repository/branches", null);
    }


    /**
     * 分支保护接口 !! 这个接口可能存在版本兼容问题
     * @param jsonObject 
     * @return
     */
    public JSONObject protectBranch(JSONObject jsonObject) {
        JSONObject json = new JSONObject();
        if (jsonObject.containsKey("push_access_level") || jsonObject.containsKey("merge_access_level")) {
            json.put("name", jsonObject.getString("name"));
            json.put("push_access_level", jsonObject.getIntValue("push_access_level"));
            json.put("merge_access_level", jsonObject.getIntValue("merge_access_level"));
            return httpPost("/api/v4/projects/" + encodeURL(repoPath) + "/protected_branches", null, json);

        } else {
            // 老版本的接口
            json.put("developers_can_push", jsonObject.getBoolean("developers_can_push"));
            json.put("developers_can_merge", jsonObject.getBoolean("developers_can_merge"));
            return httpPut("/api/v4/projects/" + encodeURL(repoPath) + "/repository/branches/" + encodeURL(jsonObject.getString("name")) + "/protect", null, json);
        }
    }

    /**
     * 使用的是旧版api
     * @param name 
     */
    public void removeProtectBranch(String name) {
        httpPut("/api/v4/projects/" + encodeURL(repoPath) + "/repository/branches/" + encodeURL(name) + "/unprotect", null, null);
    }

    /**
     * <pre>
     * 列出所有已经保护的分支的信息
     * [
     *   {
     *     "name": "master",
     *     "merged": false,
     *     "protected": true,
     *     "default": true,
     *     "developers_can_push": false,
     *     "developers_can_merge": false,
     *     "can_push": true,
     *     "commit": {
     *       "author_email": "john@example.com",
     *       "author_name": "John Smith",
     *       "authored_date": "2012-06-27T05:51:39-07:00",
     *       "committed_date": "2012-06-28T03:44:20-07:00",
     *       "committer_email": "john@example.com",
     *       "committer_name": "John Smith",
     *       "id": "7b5c3cc8be40ee161ae89a06bba6229da1032a0c",
     *       "short_id": "7b5c3cc",
     *       "title": "add projects API",
     *       "message": "add projects API",
     *       "parent_ids": [
     *         "4ad91d3c1144c406e50c7b33bae684bd6837faf8"
     *       ]
     *     }
     *   },
     *   ...
     * ]
     * </pre>
     * 
     * @return
     */
    public JSONArray listProtectBranches() {
        JSONArray retObj = new JSONArray();
        JSONArray array = listBranches();
        for (int j = 0; j < array.size(); j++) {
            if (array.getJSONObject(j).getBoolean("protected")) {
                retObj.add(array.getJSONObject(j));
            }
        }
        return retObj;
    }
    /**
     * 列出项目组成员, 没有包含继承的成员在内, 不分页的方法
     * 传入group_id或者是组名
     * @return
     */
    public JSONArray listGroupMembers(String group) {
        return httpGetNotPagination("/api/v4/groups/" + encodeURL(group) + "/members", null);
    }
    public JSONObject searchGroupMembers(String group) {
        return httpGet("/api/v4/groups/" + encodeURL(group) + "/members", null);
    }

    public JSONArray listProjectMembers(boolean inheritedMembers) {
        JSONArray jsonArray = httpGetNotPagination("/api/v4/projects/" + encodeURL(repoPath) + "/members", null);
        if (inheritedMembers) {
            // 根据初步观察, 如果成员权限相同冲突, 当项目成员权限存在, 则项目成员权限生效; 否则则是上层项目组的成员生效, 依次往上
            
            Map<Integer, JSONObject> groupMemberMap = new HashMap<>();
            Map<Integer, Boolean> projectMemberMap = new HashMap<>();
    
            for (int i = 0; i < jsonArray.size(); i++) {
                projectMemberMap.put(jsonArray.getJSONObject(i).getIntValue("id"), Boolean.TRUE);
            }
            
            JSONObject project = getCurrentProject();
            String[] pathWithNamespaceArray = project.getString("path_with_namespace").split("/");
            StringBuilder group = new StringBuilder();
            for (int i = 0; i < pathWithNamespaceArray.length; i++) {
                if (i > 0) {
                    group.append("/");
                }
                group.append(pathWithNamespaceArray[i]);
                
                try {
                    // 为了将path转成group name
                    JSONObject groupJson = getGroup(group.toString());
                    JSONArray groupJsonArray = listGroupMembers(group.toString());
                    for (int j = 0; j < groupJsonArray.size(); j++) {
                        JSONObject user = groupJsonArray.getJSONObject(j);
                        user.put("group_path", groupJson.getString("full_path"));
                        user.put("group_name", groupJson.getString("full_name"));
                        if (!projectMemberMap.containsKey(user.getIntValue("id"))) {
                            groupMemberMap.put(user.getIntValue("id"), user);
                        }
                    }
                } catch (Exception e) {
                    // 这里可能是group不存在
                    logger.error(e.toString(), e);
                    break;
                }
            }
            jsonArray.addAll(groupMemberMap.values());
        }
        
        return jsonArray;
    }
    public JSONArray listProjectMembers() {
        return listProjectMembers(false);
    }

    public JSONObject searchProjectMembers() {
        return httpGet("/api/v4/projects/" + encodeURL(repoPath) + "/members", null);
    }

    /**
     * <ul>
     * <li>10 => Guest access</li>
     * <li>20 => Reporter access</li>
     * <li>30 => Developer access</li>
     * <li>40 => Maintainer access</li>
     * <li>50 => Owner access # Only valid for groups</li>
     * </ul>
     * @param user_id 
     * @param access_level
     * @param expires_at
     * @return
     */
    public JSONObject addProjectMember(int user_id, int access_level, String expires_at) {
        JSONObject json = new JSONObject();
        json.put("user_id", user_id);
        json.put("access_level", access_level);
        json.put("expires_at", expires_at);
        return httpPost("/api/v4/projects/" + encodeURL(repoPath) + "/members", null, json);
    }
    
    public JSONObject updateProjectMember(int user_id, int access_level, String expires_at) {
        JSONObject json = new JSONObject();
        json.put("access_level", access_level);
        json.put("expires_at", expires_at);
        return httpPut("/api/v4/projects/" + encodeURL(repoPath) + "/members/" + user_id, null, json);
    }
    
    public void removeProjectMember(int user_id) {
        httpDelete("/api/v4/projects/" + encodeURL(repoPath) + "/members/" + user_id, null, null);
    }

    /**
     * 把一个项目组的授权给一个项目
     * <ul>
     * <li>10 => Guest access</li>
     * <li>20 => Reporter access</li>
     * <li>30 => Developer access</li>
     * <li>40 => Maintainer access</li>
     * <li>50 => Owner access # Only valid for groups</li>
     * </ul>
     * @param group_id 
     * @param group_access
     * @param expires_at
     * @return
     */
    public JSONObject addShareProjectWithGroup(int group_id, int group_access, String expires_at) {
        JSONObject json = new JSONObject();
        json.put("group_id", group_id);
        json.put("group_access", group_access);
        json.put("expires_at", expires_at);
        return httpPost("/api/v4/projects/" + encodeURL(repoPath) + "/share", null, json);
    }
    
    public void removeShareProjectWithGroup(int group_id) {
        httpDelete("/api/v4/projects/" + encodeURL(repoPath) + "/share/" + group_id, null, null);
    }

    /**
     * <pre>
     * [
     *     {
     *       "group_id": 4,
     *       "group_name": "Twitter",
     *       "group_access_level": 30
     *     },
     *     {
     *       "group_id": 3,
     *       "group_name": "Gitlab Org",
     *       "group_access_level": 10
     *     }
     * ]
     * </pre>
     * 
     * @return 
     */
    public JSONArray listProjectSharedWithGroups(boolean withGroupMembers) {
        JSONObject json = getCurrentProject();
        JSONArray groups = json.getJSONArray("shared_with_groups");
        for (int i = 0; i < groups.size(); i++) {
            JSONObject groupJson = groups.getJSONObject(i);
            int id = groupJson.getIntValue("group_id");
            JSONObject group = getGroup(id);
            groupJson.putAll(group);
            if (withGroupMembers) {
                groupJson.put("groupMembers", listGroupMembers(group.getString("full_path")));
            }
        }
        return groups;
    }


    //统一处理返回结果
    public static JSONObject handleGitLabResult(Map<String, List<String>> headers, String result) {
        JSONObject retObj;
        if (StringUtils.isEmpty(result)) {
            retObj = new JSONObject();
        } else {
            Object object = JSON.parse(result);
            if (object instanceof JSONObject) {
                retObj = (JSONObject) object;
            } else if (object instanceof JSONArray) {

                retObj = new JSONObject();
                retObj.put("list", object);

                // 如果是带分页的接口, 需要把总页数等返回, gitlab是存在header里面的, 在这里直接也用header不太方便
                savePaginationHeader(headers, "X-Total", retObj, "total");
                savePaginationHeader(headers, "X-Total-Pages", retObj, "totalPages");
                savePaginationHeader(headers, "X-Per-Page", retObj, "perPage");
                savePaginationHeader(headers, "X-Page", retObj, "page");
                savePaginationHeader(headers, "X-Next-Page", retObj, "nextPage");
                savePaginationHeader(headers, "X-Prev-Page", retObj, "prevPage");

            } else {
                logger.error(result);
                throw new JSONException(result);
            }
        }
        return retObj;
    }


    //分页相关参数如果没返回 统一默认都是-1
    public static void savePaginationHeader(Map<String,List<String>> headers, String headerKey, JSONObject retObj, String name) {
        int x = -1;
        if(MapUtils.isNotEmpty(headers)){
            List<String> headerValueList = headers.get(headerKey);
            if(CollectionUtils.isNotEmpty(headerValueList)){
                x = Integer.parseInt(StringUtils.isNotBlank(headerValueList.get(0)) ? headerValueList.get(0) : "-1");
            }
        }
        if (x != -1) {
            retObj.put(name, x);
        }
    }

    

}
