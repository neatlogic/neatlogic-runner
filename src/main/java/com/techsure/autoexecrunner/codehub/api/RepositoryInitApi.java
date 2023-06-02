package com.techsure.autoexecrunner.codehub.api;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.codehub.exception.SVNOpsException;
import com.techsure.autoexecrunner.codehub.git.GitWorkingCopy;
import com.techsure.autoexecrunner.codehub.svn.SVNWorkingCopy;
import com.techsure.autoexecrunner.codehub.utils.GitlabUtils;
import com.techsure.autoexecrunner.codehub.utils.JSONUtils;
import com.techsure.autoexecrunner.codehub.utils.SvnAgentUtils;
import com.techsure.autoexecrunner.codehub.utils.WorkingCopyUtils;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;
import com.techsure.autoexecrunner.restful.annotation.Description;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.tmatesoft.svn.core.wc.SVNRevision;

@Service
public class RepositoryInitApi extends PrivateApiComponentBase {
	
	Logger logger = LoggerFactory.getLogger(RepositoryInitApi.class);
	
	@Override
	public String getToken() {
		return "codehub/repository/init";
	}

	@Override
	public String getName() {
		return "仓库初始化接口";
	}
	

	/** 仓库初始化，包括导入模式和新建模式 */
	@Input({
			@Param(name = "createMode", type = ApiParamType.STRING, desc = "create mode, 'import' or 'manual'"),
			@Param(name = "repoType", type = ApiParamType.STRING, desc = "仓库类型"),
			@Param(name = "url", type = ApiParamType.STRING, desc = "url"),
			@Param(name = "username", type = ApiParamType.STRING, desc = "用户"),
			@Param(name = "password", type = ApiParamType.STRING, desc = "密码"),
			@Param(name = "checkoutBranches", type = ApiParamType.JSONARRAY, desc = "checkout列表"),
			@Param(name = "mainBranch", type = ApiParamType.STRING, desc = "主分支"),
			@Param(name = "branchesPath", type = ApiParamType.STRING, desc = "分支路径"),
			@Param(name = "tagsPath", type = ApiParamType.STRING, desc = "标签路径")
	})
	@Description(desc = "仓库初始化接口")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		String createMode = JSONUtils.optString(jsonObj,"createMode", "").trim();
		String repoType = JSONUtils.optString(jsonObj,"repoType", "").trim().toLowerCase();
		String url = JSONUtils.optString(jsonObj,"url", "").trim();
		String username = JSONUtils.optString(jsonObj,"username", "");
		String pwd = JSONUtils.optString(jsonObj,"password", "");
		JSONArray checkoutBranches = jsonObj.getJSONArray("checkoutBranches");

		String wcPath = WorkingCopyUtils.getWcPath(jsonObj);
		JSONObject returnJsonObject = new JSONObject();

		if (repoType.equals("svn")) {
			if (createMode.equals("manual")) {
				if (!SvnAgentUtils.getDelegation(jsonObj)) {
					throw new ApiRuntimeException("没有权限创建仓库");
				}
				SvnAgentUtils.createRepo(jsonObj);
			}
			if (createMode.equals("manual") || createMode.equals("import")) {
				String mainBranch = JSONUtils.optString(jsonObj,"mainBranch", "");
				String branchesPath = JSONUtils.optString(jsonObj,"branchesPath", "");
				String tagsPath = JSONUtils.optString(jsonObj,"tagsPath", "");

				// 创建仓库时无需 checkout 代码，只要不做merge操作，都不用checkout
				SVNWorkingCopy wc = new SVNWorkingCopy(wcPath, url, username, pwd, mainBranch, branchesPath, tagsPath);
				try {
					wc.getCommit(SVNRevision.HEAD.getNumber());
				} catch (SVNOpsException e) {
					wc.close();
					throw new ApiRuntimeException(e.getMessage(), e);
				}
				
				// getRepositoryRoot返回域名是小写的
				int idx = wc.getRepositoryRoot().lastIndexOf('/');
				String baseUrl = wc.getRepositoryRoot().substring(0, idx);
				if (!url.startsWith(baseUrl)) {
					throw new ApiRuntimeException(String.format("仓库地址'%s'与远程地址'%s'域名不匹配", url , baseUrl));
				}

				if (createMode.equals("manual")){
					if (StringUtils.isNotEmpty(mainBranch) && !wc.hasBranch(mainBranch)) {
						wc.mkDir(mainBranch);
					}

					if (StringUtils.isNotEmpty(branchesPath) && !wc.hasBranch("")) {
						wc.mkDir(branchesPath);
					}

					if (StringUtils.isNotEmpty(tagsPath) && !wc.hasTag("")) {
						wc.mkDir(tagsPath);
					}
				}
				else{
					// 验证分支和标签输入是否合法
					if (StringUtils.isNotEmpty(mainBranch) && !wc.hasBranch(mainBranch)) {
						wc.close();
						throw new ApiRuntimeException("主干分支 '" + mainBranch + "' 不存在");
					}

					// hasBranch 默认会取branchesPath路径验证
					if (StringUtils.isNotEmpty(branchesPath) && !wc.hasBranch("")) {
						wc.close();
						throw new ApiRuntimeException("分支路径 '" + branchesPath + "' 不存在");
					}

					if (StringUtils.isNotEmpty(tagsPath) && !wc.hasTag("")) {
						wc.close();
						throw new ApiRuntimeException("标签路径 '" + tagsPath + "' 不存在");
					}
				}
				
				if (checkoutBranches != null) {
					// 执行同步操作, 多个分支
					// 在这里如果有多个分支需要checkout, 那么需要提前同时先lock
					// 不用一个个来,  免得checkout中途某个分支时才发现lock失败
					List<SVNWorkingCopy> svnWorkingCopyList = new ArrayList<>();
					try {
						for (int i = 0; i < checkoutBranches.size(); i++) {
							SVNWorkingCopy wcBranch = null;
							String branch = checkoutBranches.getString(i);
							String branchRealPath = WorkingCopyUtils.getBranchRealPath(jsonObj, branch);
							// SVN 合并在目标分支所在目录下进行	 , checkout的repoLocalDir指向分支目录
							wcBranch = new SVNWorkingCopy(branchRealPath, url, username, pwd, mainBranch, branchesPath, tagsPath);
							svnWorkingCopyList.add(wcBranch);
							wcBranch.lock();
						}
						for (int i = 0; i < checkoutBranches.size(); i++) {
							// checkout之前重置一下分支目录, 可能此时该目录已经存在svn锁或者垃圾数据, 需要清理一下
							try {
								svnWorkingCopyList.get(i).revertCommand();
							} catch (Exception ignored) {
								// 可能是分支目录不存在 , 或者.svn目录不存在 , 或者.svn有问题等等
							}
							svnWorkingCopyList.get(i).checkout(checkoutBranches.getString(i));
						}
					} finally {
						for (SVNWorkingCopy svnWorkingCopy : svnWorkingCopyList) {								
							svnWorkingCopy.unlock();
							svnWorkingCopy.close();
						}
					}
				}
				
				wc.close();
			}
		} else if (repoType.equals("gitlab")) {
			if (createMode.equals("manual")) {
				GitlabUtils.createProject(jsonObj);
			}
			// 创建成功后还得重新clone下来
			if (createMode.equals("manual") || createMode.equals("import")) {
				GitWorkingCopy wc = new GitWorkingCopy(wcPath, url, username, pwd);
				// 如果目录已经clone成功了,那么就pull同步
				File localPath = new File(wcPath);
				if (localPath.exists()) {
					try {
						wc.update();

						// 返回调用者默认分支等信息
						String defaultBranch = wc.getRemoteDefaultBranch();
						returnJsonObject.put("defaultBranch", defaultBranch);
						if (wc.hasBranch("master")) {
							returnJsonObject.put("mainBranch", "master");
						} else {
							returnJsonObject.put("mainBranch", defaultBranch);
						}
					} catch (Exception e) {
						wc.close();
						throw new ApiRuntimeException(e.getMessage(), e);
					}
				}
				wc.close();
			}
		}

		return returnJsonObject;
	}


/*	@Override
	public JSONArray help() {
		JSONArray jsonArray = new JSONArray();
		ApiHelpUtils.addRepoAuthJsonObj(jsonArray);

		JSONObject jsonObj = new JSONObject();
		jsonObj.put("name", "createMode");
		jsonObj.put("type", "String");
		jsonObj.put("desc", "create mode, 'import' or 'manual'");
		jsonArray.add(jsonObj);

		ApiHelpUtils.addSVNWorkingCopyPathJsonObj(jsonArray);

		return jsonArray;
	}*/


}
