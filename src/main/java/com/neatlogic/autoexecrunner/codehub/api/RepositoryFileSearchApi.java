package com.neatlogic.autoexecrunner.codehub.api;


import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.codehub.dto.commit.CommitInfo;
import com.neatlogic.autoexecrunner.codehub.dto.diff.FileInfo;
import com.neatlogic.autoexecrunner.codehub.git.GitWorkingCopy;
import com.neatlogic.autoexecrunner.codehub.svn.SVNWorkingCopy;
import com.neatlogic.autoexecrunner.codehub.utils.JSONUtils;
import com.neatlogic.autoexecrunner.codehub.utils.WorkingCopyUtils;
import com.neatlogic.autoexecrunner.restful.annotation.Description;
import com.neatlogic.autoexecrunner.restful.annotation.Input;
import com.neatlogic.autoexecrunner.restful.annotation.Param;
import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;
import com.neatlogic.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**仓库文件搜索接口
 *
 * @author fengt
 */
@Service
public class RepositoryFileSearchApi extends PrivateApiComponentBase {
	
	@Override
	public String getToken() {
		return "codehub/repositoryfilesearch";
	}

	@Override
	public String getName() {
		return "仓库文件搜索接口";
	}


	@Input({
			@Param(name = "repoType", type = ApiParamType.STRING, desc = "仓库类型"),
			@Param(name = "url", type = ApiParamType.STRING, desc = "url"),
			@Param(name = "username", type = ApiParamType.STRING, desc = "用户"),
			@Param(name = "password", type = ApiParamType.STRING, desc = "密码"),
			@Param(name = "queryType", type = ApiParamType.STRING, desc = "搜索类型(branch,tag)"),
			@Param(name = "queryName", type = ApiParamType.STRING, desc = "搜索类型对应的搜索值"),
			@Param(name = "mainBranch", type = ApiParamType.STRING, desc = "主分支"),
			@Param(name = "branchesPath", type = ApiParamType.STRING, desc = "分支路径"),
			@Param(name = "tagsPath", type = ApiParamType.STRING, desc = "标签路径"),
			@Param(name = "subFilePath", type = ApiParamType.STRING, desc = "git(相对于工程根的目录路径),svn(相对于仓库根的路径，譬如：branches/1.0.0/src/java/test/test.java)"),
			@Param(name = "skipCount", type = ApiParamType.INTEGER, desc = "略过的数量，当skipCount=0时，代表不略过"),
			@Param(name = "limitCount", type = ApiParamType.INTEGER, desc = "查询的最大数量，当limitCount=0是，代表查询全部,默认是1000")
	})
	@Description(desc = "仓库文件搜索接口")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		String repoType = JSONUtils.optString(jsonObj,"repoType", "").trim().toLowerCase();
		String url = JSONUtils.optString(jsonObj,"url", "").trim();
		String username = JSONUtils.optString(jsonObj,"username", "");
		String pwd = JSONUtils.optString(jsonObj,"password", "");
		String queryType  = JSONUtils.optString(jsonObj,"queryType", "");
		String queryName  = JSONUtils.optString(jsonObj,"queryName", "");
        String mainBranch  = JSONUtils.optString(jsonObj,"mainBranch", "");
        String branchesPath  = JSONUtils.optString(jsonObj,"branchesPath", "");
        String tagsPath  = JSONUtils.optString(jsonObj,"tagsPath", "");
		String subFilePath  = JSONUtils.optString(jsonObj,"subFilePath", "");
		int skipCount = JSONUtils.optInt(jsonObj,"skipCount",0 );
		int limitCount = JSONUtils.optInt(jsonObj,"limitCount",1000 );
		
		String wcPath = WorkingCopyUtils.getWcPath(jsonObj);
		String commitId = null;
		List<FileInfo> fileInfoList = null;
		CommitInfo lastCommit = null;
		if (repoType.equals("svn")) {
			
            SVNWorkingCopy wc = null;
            try {
            	wc = new SVNWorkingCopy(wcPath, url, username, pwd , mainBranch, branchesPath, tagsPath);
    			if("branch".equals( queryType )){
    				String branchName = queryName;
    				if(!wc.hasBranch(branchName)){
    					wc.close();
    					throw new ApiRuntimeException(String.format("分支 '%s' 不存在",branchName));
    				}
    				
    				if(StringUtils.isBlank(subFilePath)){
    					subFilePath = wc.getRealBranchPath(branchName);
    				}
    				fileInfoList = wc.listFolder(wc.resolveBranch( branchName ) , subFilePath , skipCount, limitCount);
					lastCommit = wc.getCommit(wc.resolvePath(subFilePath));
				}else if("tag".equals( queryType )){
    				String tagName = queryName;
    				if(!wc.hasTag(tagName)){
    					wc.close();
    					throw new ApiRuntimeException(String.format("标签 '%s' 不存在",tagName));
    				}
    				
    				if(StringUtils.isBlank(subFilePath)){
    					subFilePath = wc.getRealTagPath(tagName);
    				}
					long rev = wc.resolveTag(tagName);
    				fileInfoList = wc.listFolder( rev, subFilePath , skipCount, limitCount);
					lastCommit = wc.getCommit(wc.resolvePath(subFilePath));

				} else {
    				throw new ApiRuntimeException(String.format("不支持的查询类型: '%s'", queryType));
    			}
            } catch (Exception ex) {
            	throw new ApiRuntimeException(ex);
            } finally {
            	if (wc != null) {
            		wc.close();
            	}
            }
		} else if (repoType.equals("gitlab")) {
			GitWorkingCopy wc = null;
			try {
				wc = new GitWorkingCopy(wcPath, url, username, pwd);
				wc.update();

				if ("branch".equals( queryType ) || "tag".equals( queryType )) {
					if("branch".equals( queryType )){
						commitId = wc.resolveBranch(queryName);
						if (commitId == null) {
							throw new ApiRuntimeException(String.format("分支 '%s' 不存在", queryName));
						}
					}else if("tag".equals( queryType )){
						commitId = wc.resolveTag(queryName);
						if (commitId == null) {
							throw new ApiRuntimeException(String.format("标签 '%s' 不存在", queryName));
						}
					}

					if(StringUtils.isNotEmpty( commitId )){
						fileInfoList = wc.listFolder(commitId, subFilePath, skipCount, limitCount);
						lastCommit = wc.getFilePathLastCommit(commitId, subFilePath);
					}
				} else {
					throw new ApiRuntimeException(String.format("不支持的查询类型: '%s'", queryType));
				}
			} catch (Exception ex) {
				throw new ApiRuntimeException(ex);
			} finally {
				if (wc != null) {
					wc.close();
				}
			}
		}
		
		WorkingCopyUtils.fileAndPathSort(fileInfoList, false);
		
		JSONObject ret = new JSONObject();
		ret.put("parentPath", subFilePath);
		ret.put("list", fileInfoList);
		ret.put("lastCommit", lastCommit);

		return ret;
	}


/*	@Override
	public JSONArray help() {
		JSONArray jsonArray = new JSONArray();

		ApiHelpUtils.addRepoAuthJsonObj(jsonArray);

		JSONObject jsonObj = new JSONObject();
		jsonObj.put("name", "queryType");
		jsonObj.put("type", "String");
		jsonObj.put("desc", "搜索类型(branch,tag)");
		jsonArray.add(jsonObj);

		jsonObj = new JSONObject();
		jsonObj.put("name", "queryName");
		jsonObj.put("type", "String");
		jsonObj.put("desc", "搜索类型对应的搜索值");
		jsonArray.add(jsonObj);

		jsonObj = new JSONObject();
		jsonObj.put("name", "subFilePath");
		jsonObj.put("type", "String");
		jsonObj.put("desc", "git(相对于工程根的目录路径),svn(相对于仓库根的路径，譬如：branches/1.0.0/src/java/test/test.java)");
		jsonArray.add(jsonObj);

		jsonObj = new JSONObject();
		jsonObj.put("name", "skipCount");
		jsonObj.put("type", "Integer");
		jsonObj.put("desc", "略过的数量，当skipCount=0时，代表不略过");
		jsonArray.add(jsonObj);

		jsonObj = new JSONObject();
		jsonObj.put("name", "limitCount");
		jsonObj.put("type", "Integer");
		jsonObj.put("desc", "查询的最大数量，当limitCount=0是，代表查询全部,默认是1000");
		jsonArray.add(jsonObj);

		ApiHelpUtils.addSVNWorkingCopyPathJsonObj(jsonArray);

		return jsonArray;
	}*/


}
