/**
 * 撤销检查，判断指定分支上的某个 commit 是否可被撤销
 */
package com.techsure.autoexecrunner.codehub.api;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.codehub.dto.merge.MergeResultInfo;
import com.techsure.autoexecrunner.codehub.git.GitWorkingCopy;
import com.techsure.autoexecrunner.codehub.svn.SVNWorkingCopy;
import com.techsure.autoexecrunner.codehub.utils.JSONUtils;
import com.techsure.autoexecrunner.codehub.utils.WorkingCopyUtils;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.restful.annotation.Description;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**   
 * @ClassName   RevertCheckApi
 * @author      zouye
 * @date        2021-04-19   
 *    
 */
@Service
public class RevertCheckApi extends PrivateApiComponentBase {
	Logger logger = LoggerFactory.getLogger(RevertCheckApi.class);

	@Override
	public String getToken() {
		return "codehub/revertcheck";
	}

	@Override
	public String getName() {
		return "检查是否可以revert接口";
	}

	@Input({
			@Param(name = "repoType", type = ApiParamType.STRING, desc = "仓库类型"),
			@Param(name = "url", type = ApiParamType.STRING, desc = "url"),
			@Param(name = "username", type = ApiParamType.STRING, desc = "用户"),
			@Param(name = "password", type = ApiParamType.STRING, desc = "密码"),
			@Param(name = "branch", type = ApiParamType.STRING, desc = "分支 id"),
			@Param(name = "commit", type = ApiParamType.STRING, desc = "提交 id"),
			@Param(name = "commitMerger", type = ApiParamType.STRING, desc = "提交合并人"),
			@Param(name = "commitMergerEmail", type = ApiParamType.STRING, desc = "提交合并人邮箱"),
			@Param(name = "commitMergerComment", type = ApiParamType.STRING, desc = "合并信息")
	})
	@Description(desc = "检查是否可以revert接口")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		String repoType = JSONUtils.optString(jsonObj,"repoType", "").trim().toLowerCase();
		String url = JSONUtils.optString(jsonObj,"url", "").trim();
		String username = JSONUtils.optString(jsonObj,"username", "");
		String pwd = JSONUtils.optString(jsonObj,"password", "");

		String branch = JSONUtils.optString(jsonObj,"branch", "").trim();
		String commit = JSONUtils.optString(jsonObj,"commit", "");
		String commitMerger = JSONUtils.optString(jsonObj, "commitMerger" );
		String commitMergerEmail = JSONUtils.optString(jsonObj, "commitMergerEmail" );
		String commitMergerComment = JSONUtils.optString(jsonObj, "commitMergerComment" );
		
		String wcPath = WorkingCopyUtils.getWcPath(jsonObj);
		String branchRealPath = WorkingCopyUtils.getBranchRealPath(jsonObj, branch);
		
		JSONObject ret = new JSONObject();

		if (repoType.equals("svn")) {
			SVNWorkingCopy wc = null;
			try {
	            wc = new SVNWorkingCopy(branchRealPath, url, username, pwd , JSONUtils.optString(jsonObj,"mainBranch",""), JSONUtils.optString(jsonObj,"branchesPath",""), JSONUtils.optString(jsonObj,"tagsPath",""));
			} finally {
				if (wc != null) {
					wc.close();
				}
			}
		} else if (repoType.equals("gitlab")) {
			GitWorkingCopy wc = null;
			String revertBranch = null;
			boolean canRevert = false;
			
			try {
				wc = new GitWorkingCopy(wcPath, url, username, pwd);
				
				if (!wc.hasBranch(branch)) {
					throw new RuntimeException(String.format("branch '%s' not exist", branch));
				}
				wc.lock();
				
				wc.reset();
				wc.checkout(branch, true);
				
				// 生成 revert 分支
				revertBranch = "revert-" + StringUtils.substring(commit, 0, 8);
				
				try {
					// 已经存在的分支就删除后再创建新的, 避免接口调用多次revertcheck导致 revert-xxx分支上的revert log重复 , 合并后生成重复多余的log
					if (wc.hasLocalBranch(revertBranch)) {
						wc.deleteLocalBranch(revertBranch);
					}
					
					wc.createLocalBranch(revertBranch, branch);
					wc.checkout(revertBranch, false);
					
					// 在 revert 分支执行 revert 操作，如果报错或冲突，则无法 revert
					MergeResultInfo mergeInfo = wc.revertCommitCommand(commit);
					
					if (mergeInfo.isConflict()) {
						wc.reset();
						wc.checkout(branch, false);
						wc.deleteLocalBranch(revertBranch);
					} else {
						wc.commit(commitMerger, commitMergerEmail, commitMergerComment);
						canRevert = true;
					}
				} catch (Exception e) {
					// revert 分支已经创建，但是执行 revert 操作异常，则删除本地的 revert 分支
					if (StringUtils.isNotBlank(revertBranch) && wc.hasBranch(revertBranch)) {
						wc.reset();
						wc.checkout(branch, false);
						wc.deleteLocalBranch(revertBranch);
					}
					throw e;
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				throw e;
			} finally {
				if (wc != null) {
					wc.unlock();
					wc.close();
				}
			}
			
			ret.put("canRevert", canRevert);
			ret.put("revertBranch", revertBranch);
		}
		
		return ret;
	}



}
