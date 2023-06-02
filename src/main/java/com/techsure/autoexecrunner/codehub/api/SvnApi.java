package com.techsure.autoexecrunner.codehub.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.codehub.dto.diff.FileInfo;
import com.techsure.autoexecrunner.codehub.svn.SVNWorkingCopy;
import com.techsure.autoexecrunner.codehub.utils.JSONUtils;
import com.techsure.autoexecrunner.codehub.utils.SvnAgentUtils;
import com.techsure.autoexecrunner.codehub.utils.WorkingCopyUtils;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;
import com.techsure.autoexecrunner.restful.annotation.Description;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;


import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;


/**
 * 给外部系统调用的 SVN API 接口
 * 
 * @author yujh
 */
@Service
public class SvnApi extends PrivateApiComponentBase {

    @Override
    public String getToken() {
        return "codehub/svn";
    }

    @Override
    public String getName() {
        return "SVN API 接口";
    }

    @Input({
            @Param(name = "method", type = ApiParamType.STRING, desc = "方法"),
            @Param(name = "agentUrl", type = ApiParamType.STRING, desc = "svn代理对应url"),
            @Param(name = "agentUsername", type = ApiParamType.STRING, desc = "svn代理用户名"),
            @Param(name = "agentPassword", type = ApiParamType.STRING, desc = "svn代理密码"),
            @Param(name = "paramJsonObj", type = ApiParamType.JSONOBJECT, desc = "json字符串参数"),
            @Param(name = "path", type = ApiParamType.STRING, desc = "svn路径"),
            @Param(name = "groupId", type = ApiParamType.STRING, desc = "组名称"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字"),
            @Param(name = "url", type = ApiParamType.STRING, desc = "url"),
            @Param(name = "username", type = ApiParamType.STRING, desc = "用户"),
            @Param(name = "password", type = ApiParamType.STRING, desc = "密码"),
            @Param(name = "mainBranch", type = ApiParamType.STRING, desc = "分支"),
            @Param(name = "branchesPath", type = ApiParamType.STRING, desc = "分支路径"),
            @Param(name = "tagsPath", type = ApiParamType.STRING, desc = "标签路径")
    })
    @Description(desc = "SVN API接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String method = jsonObj.getString("method");
        String agentUrl = JSONUtils.optString(jsonObj,"agentUrl", "");
        String agentUsername = JSONUtils.optString(jsonObj,"agentUsername", "");
        String agentPassword = JSONUtils.optString(jsonObj,"agentPassword", "");
        JSONObject paramJsonObj = jsonObj.getJSONObject("paramJsonObj");
        String path = JSONUtils.optString(jsonObj,"path", "");
        String groupId = JSONUtils.optString(jsonObj,"groupId", "");
        String keyword = JSONUtils.optString(jsonObj,"keyword", "");

        String url = JSONUtils.optString(jsonObj,"url", "").trim();
        String username = JSONUtils.optString(jsonObj,"username", "");
        String pwd = JSONUtils.optString(jsonObj,"password", "");
        String mainBranch = JSONUtils.optString(jsonObj,"mainBranch", "");
        String branchesPath = JSONUtils.optString(jsonObj,"branchesPath", "");
        String tagsPath = JSONUtils.optString(jsonObj,"tagsPath", "");
    
        // 不包含仓库信息，只获取svn服务全局级别的配置
        if (!jsonObj.containsKey("repoPath") && method.equals("getdelegation")) {
            return SvnAgentUtils.getDelegation(jsonObj);
        }
        
        String wcPath = WorkingCopyUtils.getWcPath(jsonObj);
        SVNWorkingCopy wc = null;
        try {
            wc = new SVNWorkingCopy(wcPath, url, username, pwd, mainBranch, branchesPath, tagsPath);
    
            // getRepositoryRoot返回域名是小写的
            int idx = wc.getRepositoryRoot().lastIndexOf('/');
            String baseUrl = wc.getRepositoryRoot().substring(0, idx);
            if (!url.startsWith(baseUrl)) {
                throw new ApiRuntimeException(String.format("仓库地址'%s'与远程地址'%s'域名不匹配", url , baseUrl));
            }
	        JSONObject jsonObject;
	        if ("putauth".equals(method)) {
	        	if (paramJsonObj == null) {
	        		throw new ApiRuntimeException("请指定权限列表");
	        	}
	        	String repoName = wc.getRepositoryRoot().substring(idx + 1);
                jsonObj.put("repo", repoName);
	        	if (!SvnAgentUtils.getDelegation(jsonObj)) {
	        		throw new ApiRuntimeException("禁止修改仓库权限");
                }
	        	
	        	if (StringUtils.equals(StringUtils.stripEnd(wc.getRepositoryRoot(), "/"), StringUtils.stripEnd(url, "/"))) {
        			path = wc.getRepositoryRoot().substring(idx + 1);
        		} else {
        			// 代码中心的仓库只是远程仓库的一个子目录
        			path = StringUtils.replace(url, baseUrl, "");
        		}
	        	
	        	paramJsonObj.put("path", path);
	            jsonObject = SvnAgentUtils.putAuth(agentUrl, agentUsername, agentPassword, paramJsonObj);
	
	        } else if ("getauth".equals(method)) {
	        	// 增加一个参数 控制, 返回组里面的用户getmemberbygroup
        
                boolean withGroupMembers = jsonObj.getBooleanValue("withGroupMembers");
	        	// 传入了 path，则一定是查询子目录的权限
	        	if (StringUtils.isNotBlank(path)) {
	        		// 传给 svn agent 的 path 必须有仓库名称
	        		path = StringUtils.replace(url, baseUrl, "") + "/" + path;
	        		jsonObject = SvnAgentUtils.getAuth(agentUrl, agentUsername, agentPassword, path);
	        	} else {
	        		// 没有给定path，但是代码中心的仓库和远程仓库一一对应
	        		if (StringUtils.equals(StringUtils.stripEnd(wc.getRepositoryRoot(), "/"), StringUtils.stripEnd(url, "/"))) {
	        			String repoName = wc.getRepositoryRoot().substring(idx + 1);
		        		jsonObject = SvnAgentUtils.getRepoAuth(agentUrl, agentUsername, agentPassword, repoName);
	        		} else {
	        			// 代码中心的仓库只是远程仓库的一个子目录
	        			path = StringUtils.replace(url, baseUrl, "");
		        		jsonObject = SvnAgentUtils.getAuth(agentUrl, agentUsername, agentPassword, path);
	        		}
	        	}
        
                if (withGroupMembers) {
                    String status = jsonObject.getString("status");
                    if (StringUtils.equals(status, "OK")) {
                        Map<String, JSONArray> groupCache = new HashMap<>();
                        JSONObject data = jsonObject.getJSONObject("content");
                        Collection<Object> jsonArrays = data.values();
                        for (Object object : jsonArrays) {
                            JSONArray jsonArray = (JSONArray) JSON.toJSON(object);
                            for (int i = 0; i < jsonArray.size(); i++) {
                                JSONObject authJsonObj = jsonArray.getJSONObject(i);
                                String type = authJsonObj.getString("type");     
                                String name = authJsonObj.getString("name");
    
                                if ("group".equals(type)) {
                                    JSONArray groupUser = null;
                                    if (groupCache.containsKey(name)) {
                                        groupUser = groupCache.get(name);
                                    } else {
                                        JSONObject group = SvnAgentUtils.getMember(agentUrl, agentUsername, agentPassword, name, keyword);
                                        if (StringUtils.equals(group.getString("status"), "OK")) {
                                            groupUser = group.getJSONArray("content");
                                            groupCache.put(name, groupUser);
                                        }
                                    }
                                    if (groupUser != null) {
                                        authJsonObj.put("users", String.join(", ", groupUser.toJSONString()));
                                    }
                                }
                            }
                        }
                    }
                }
	        	
	        } else if ("getmemberbygroup".equals(method)) {
	            jsonObject = SvnAgentUtils.getMember(agentUrl, agentUsername, agentPassword, groupId, keyword);
	
	        } else if ("getgroup".equals(method)) {
	            jsonObject = SvnAgentUtils.getGroup(agentUrl, agentUsername, agentPassword, groupId);
	
	        } else if ("pathtree".equals(method)) {
	        	List<FileInfo> fileInfoList = null;
	        	if (StringUtils.isBlank(path)) {
	        		fileInfoList = new ArrayList<>();
	        		FileInfo fileInfo = new FileInfo();
	        		
	        		if (StringUtils.equals(StringUtils.stripEnd(wc.getRepositoryRoot(), "/"), StringUtils.stripEnd(url, "/"))) {
	        			fileInfo.setPath("/");
	        		} else {
	        			fileInfo.setPath(StringUtils.stripStart(url, wc.getRepositoryRoot()));
	        		}
	        		
	        		fileInfo.setType('D');
	        		
	        		fileInfoList.add(fileInfo);
	        	} else {
	        		fileInfoList = wc.listFolder(-1, StringUtils.stripStart(path, "/"));
	        	}
	        	
	        	WorkingCopyUtils.fileAndPathSort(fileInfoList, true);
	        	return fileInfoList;
	        }  else if ("getdelegation".equals(method)) {
                String repoName = wc.getRepositoryRoot().substring(idx + 1);
                jsonObj.put("repo", repoName);
	        	return SvnAgentUtils.getDelegation(jsonObj);
	        } else {
	            throw new ApiRuntimeException("method error: " + method);
	        }
	
	        String status = jsonObject.getString("status");
	        if (StringUtils.equals(status, "OK")) {
	            if (jsonObject.containsKey("content")) {
	                return jsonObject.get("content");
	            } else {
	                return jsonObject;
	            }
	        } else {
	            throw new ApiRuntimeException("调用svn代理出错：" + jsonObject.toString());
	        }
        } catch (Exception e) {
        	throw new ApiRuntimeException(e);
        } finally {
        	if (wc != null) {
        		wc.close();
        	}
        }
    }
    
/*    @Override
    public JSONArray help() {
        JSONArray jsonArray = new JSONArray();

        ApiHelpUtils.addRepoAuthJsonObj(jsonArray);

        ApiHelpUtils.addRepoAuthJsonObj(jsonArray);

        JSONObject jsonObj = new JSONObject();
        jsonObj.put("name", "agentUrl");
        jsonObj.put("type", "String");
        jsonObj.put("desc", "svn代理对应url");
        jsonArray.add(jsonObj);

        jsonObj = new JSONObject();
        jsonObj.put("name", "agentUsername");
        jsonObj.put("type", "String");
        jsonObj.put("desc", "svn代理用户名");
        jsonArray.add(jsonObj);

        jsonObj = new JSONObject();
        jsonObj.put("name", "agentPassword");
        jsonObj.put("type", "String");
        jsonObj.put("desc", "svn代理密码");
        jsonArray.add(jsonObj);

        jsonObj = new JSONObject();
        jsonObj.put("name", "paramJsonObj");
        jsonObj.put("type", "String");
        jsonObj.put("desc", "json字符串参数");
        jsonArray.add(jsonObj);

        jsonObj = new JSONObject();
        jsonObj.put("name", "path");
        jsonObj.put("type", "String");
        jsonObj.put("desc", "svn路径");
        jsonArray.add(jsonObj);

        jsonObj = new JSONObject();
        jsonObj.put("name", "groupId");
        jsonObj.put("type", "String");
        jsonObj.put("desc", "组名称");
        jsonArray.add(jsonObj);

        jsonObj = new JSONObject();
        jsonObj.put("name", "repo");
        jsonObj.put("type", "String");
        jsonObj.put("desc", "仓库名称");
        jsonArray.add(jsonObj);

        ApiHelpUtils.addSVNWorkingCopyPathJsonObj(jsonArray);
        return jsonArray;
    }*/

}
