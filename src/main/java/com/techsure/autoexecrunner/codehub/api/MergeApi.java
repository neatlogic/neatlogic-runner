package com.techsure.autoexecrunner.codehub.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.codehub.dto.commit.CommitInfo;
import com.techsure.autoexecrunner.codehub.dto.merge.MergeFileEntry;
import com.techsure.autoexecrunner.codehub.dto.merge.MergeResultInfo;
import com.techsure.autoexecrunner.codehub.exception.GitOpsException;
import com.techsure.autoexecrunner.codehub.exception.SVNOpsException;
import com.techsure.autoexecrunner.codehub.git.GitWorkingCopy;
import com.techsure.autoexecrunner.codehub.svn.SVNWorkingCopy;
import com.techsure.autoexecrunner.codehub.utils.JSONUtils;
import com.techsure.autoexecrunner.codehub.utils.WorkingCopyUtils;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.tmatesoft.svn.core.SVNMergeRange;
import org.tmatesoft.svn.core.SVNMergeRangeList;

/**
 * 
 * @author zouye
 * @date 2020年8月13日 下午9:03:07
 * @Description 执行代码合并，根据传入的参数有无 commitList 判断是按分支合并还是按需求合并,返回合并的结果
 *
 */
@Service
public class MergeApi extends PrivateApiComponentBase {
	Logger logger = LoggerFactory.getLogger(MergeApi.class);

	@Override
	public String getToken() {
		return "codehub/merge";
	}


	@Input({
			@Param(name = "repoType", type = ApiParamType.STRING, desc = "仓库类型"),
			@Param(name = "url", type = ApiParamType.STRING, desc = "url"),
			@Param(name = "username", type = ApiParamType.STRING, desc = "用户"),
			@Param(name = "password", type = ApiParamType.STRING, desc = "密码"),
			@Param(name = "srcBranch", type = ApiParamType.STRING, desc = "源分支"),
			@Param(name = "targetBranch", type = ApiParamType.STRING, desc = "目标分支"),
			@Param(name = "commitList", type = ApiParamType.JSONARRAY, desc = "commit列表"),
			@Param(name = "commitMerger", type = ApiParamType.STRING, desc = "合并提交人"),
			@Param(name = "commitMergerEmail", type = ApiParamType.STRING, desc = "合并提交人邮箱"),
			@Param(name = "commitMergerComment", type = ApiParamType.STRING, desc = "合并提交描述"),
			@Param(name = "isRevert", type = ApiParamType.BOOLEAN, desc = "是否为revert"),
			@Param(name = "mergeBaseOnly", type = ApiParamType.BOOLEAN, desc = "是否只取mergeBase，不做合并"),
			@Param(name = "mergeBase", type = ApiParamType.STRING, desc = "mergeBase commitId"),
			@Param(name = "isFirstIssue", type = ApiParamType.BOOLEAN, desc = "")
	})
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		String repoType = JSONUtils.optString(jsonObj,"repoType", "").trim().toLowerCase();
		String url = JSONUtils.optString(jsonObj,"url", "").trim();
		String username = JSONUtils.optString(jsonObj,"username", "");
		String pwd = JSONUtils.optString(jsonObj,"password", "");

		String srcBranch = JSONUtils.optString(jsonObj,"srcBranch", "").trim();
		String targetBranch = JSONUtils.optString(jsonObj,"targetBranch", "").trim();
		JSONArray commitList = jsonObj.getJSONArray("commitList");
		String commitMerger = JSONUtils.optString(jsonObj, "commitMerger" );
		String commitMergerEmail = JSONUtils.optString(jsonObj, "commitMergerEmail" );
		String commitMergerComment = JSONUtils.optString(jsonObj, "commitMergerComment" );
		Boolean isRevert = JSONUtils.optBoolean(jsonObj,"isRevert", false );
		Boolean mergeBaseOnly = JSONUtils.optBoolean(jsonObj, "mergeBaseOnly", false );
		String oldMergeBase = JSONUtils.optString(jsonObj,"mergeBase","");
		boolean isFirstIssue = JSONUtils.optBoolean(jsonObj,"isFirstIssue", false);
		
		String wcPath = WorkingCopyUtils.getWcPath(jsonObj);
		String branchRealPath = WorkingCopyUtils.getBranchRealPath(jsonObj, targetBranch);
		
		JSONObject ret = new JSONObject();

		if (repoType.equals("svn")) {
			SVNWorkingCopy wc = null;
			try {
				// SVN 合并在目标分支所在目录下进行
	            wc = new SVNWorkingCopy(branchRealPath, url, username, pwd , JSONUtils.optString(jsonObj,"mainBranch",""), JSONUtils.optString(jsonObj,"branchesPath",""), JSONUtils.optString(jsonObj,"tagsPath",""));
	            if (!wc.hasBranch(srcBranch)) {
	            	throw new RuntimeException(String.format("branch '%s' is not exist", srcBranch));
	            }
	            
	            if (!wc.hasBranch(targetBranch)) {
	            	throw new RuntimeException(String.format("branch '%s' is not exist", targetBranch));
	            }
	            wc.lock();
				
				try {
					if (mergeBaseOnly) {
						// svn 分支合并，mergebase 从 mergeinfo 中取
						SVNMergeRangeList mergeRangeList = wc.getMergeRangeList(srcBranch, targetBranch);
						if (mergeRangeList != null && !mergeRangeList.isEmpty()) {
							SVNMergeRange lastMergeRange = mergeRangeList.getRanges()[mergeRangeList.getSize() - 1];
							ret.put("mergeBase", lastMergeRange.getEndRevision());
						}
						
						CommitInfo headCommit = wc.getHeadCommit(srcBranch);
						if (headCommit != null) {
							ret.put("srcStartCommit", headCommit.getCommitId());
						}
					} else {
						//【代码中心】SVN需求合并报错 https://www.tapd.cn/54247054/bugtrace/bugs/view?bug_id=1154247054001007995
						boolean isIssueMerge = CollectionUtils.isNotEmpty(commitList);
						if (!isIssueMerge || isFirstIssue) {
							wc.revertCommand();
						}
						
						wc.checkout(targetBranch);
						
						// 需求型合并
						if (isIssueMerge) {
							svnIssueMerge(ret, wc, srcBranch, targetBranch, commitList, commitMergerComment, isRevert);
						} else {
							svnBranchMerge(ret, wc, srcBranch, targetBranch, oldMergeBase, commitMergerComment, isRevert);
						}
					}
				} catch (Exception e) {
					// 报错时清理 workingcopy , 避免因为lock报错 , 而只执行了revert导致和正在合并的操作冲突
					wc.revertCommand();
					throw e;
				}
				
				
			} catch (Exception ex) {
				ret.put("status", "failed");
				ret.put("message", StringUtils.isNotBlank(ex.getMessage()) ? ex.getMessage() : "SVN merge failed, please check log for detail");
				logger.error(ex.getMessage(), ex);
				
			} finally {
				if (wc != null) {
					wc.unlock();
					wc.close();
				}
			}
		} else if (repoType.equals("gitlab")) {
			GitWorkingCopy wc = null;
			try {
				wc = new GitWorkingCopy(wcPath, url, username, pwd);
				wc.lock();
				wc.resetAndUpdate();
				
				try {
					String srcCommitId = wc.resolveBranch(srcBranch);
					String targetCommitId = wc.resolveBranch(targetBranch);
					if (StringUtils.isBlank(srcCommitId)) {
						throw new RuntimeException(String.format("branch %s is not exist", srcBranch));
					}
					
					if (StringUtils.isBlank(targetCommitId)) {
						throw new RuntimeException(String.format("branch %s is not exist", targetBranch));
					}
					
					if (mergeBaseOnly) {
						String mergeBase = wc.getMergeBaseCommand(srcCommitId, targetCommitId);
						ret.put("srcStartCommit", srcCommitId);
						ret.put("targetStartCommit", targetCommitId);
						ret.put("mergeBase", mergeBase);
					} else {
						wc.checkout(targetBranch, true);
	
						if (commitList != null && !commitList.isEmpty()) {
							gitIssueMerge(ret, wc, srcBranch, targetBranch, commitList, commitMergerComment, commitMerger, commitMergerEmail);
						} else {
							gitBranchMerge(ret, wc, srcBranch, targetBranch, oldMergeBase, commitMergerComment, commitMerger, commitMergerEmail, isRevert);
						}
					}
				} catch (Exception e) {
					wc.reset();
					throw e;
				}
				
			} catch (Exception ex) {
				ret.put("status", "failed");
				ret.put("message", StringUtils.isNotBlank(ex.getMessage()) ? ex.getMessage() : "git merge failed, please check log for detail");

				logger.error(ex.getMessage(), ex);
			} finally {
				if (wc != null) {
					wc.unlock();
					wc.close();
				}
			}
		}

		return ret;
	}

	/**
	 * @throws SVNOpsException 
	 * @Title: svnIssueMerge   
	 * @Description: svn 需求型合并
	 * @param: @param ret， JSONObject，合并结果将被写入这个对象
	 * @param: @param wc， svn working copy
	 * @param: @param targetBranch
	 * @param: @param commitList      commit 列表
	 * @param: @param isRevert      是否为 revert
	 * @return: void      
	 * @throws
	 */
	public void svnIssueMerge(JSONObject ret, SVNWorkingCopy wc, String srcBranch, String targetBranch, 
			JSONArray commitList, String commitMergerComment, Boolean isRevert) throws SVNOpsException {
		if (commitList == null || commitList.isEmpty()) {
			ret.put("status", "invalid");

			return;
		}

		//String commitId = "";
		//List<JSONObject> commitFileList = new ArrayList<>();
		Set<String> fileList = new HashSet<>();
		
		/* 一个需求只 cherrypick 一次的话，cherrypick 前会做排序
		// revert：降序排序，commitId 大的先merge
		// standard：升序排序，commitId 小的先merge
		Collections.sort(commitList, new Comparator<JSONObject>() {

			@Override
			public int compare(JSONObject o1, JSONObject o2) {
				Long commitId1 = o1.optLong("commitId");
				Long commitId2 = o2.optLong("commitId");
				
				return isRevert ? commitId2.compareTo(commitId1) : commitId1.compareTo(commitId2);
			}
		});*/
		
		String[] commits = new String[commitList.size()];
 		for (int i = 0; i < commitList.size(); i++) {
			commits[i] = commitList.getJSONObject(i).getString("commitId");
		}

		MergeResultInfo mRes = wc.CherryPick(srcBranch, targetBranch, commits, isRevert, false);

		if (CollectionUtils.isNotEmpty(mRes.getMergeFileEntrys())) {
			for (MergeFileEntry entry: mRes.getMergeFileEntrys()) {
				if (StringUtils.isNotBlank(entry.getFilePath())) {
					fileList.add(entry.getFilePath());
				}
			}
		}

		ret.put("commitFileList", fileList);
		if (mRes.isConflict()) {
			ret.put("status", "conflict");
			if (logger.isErrorEnabled()) {
				logger.error(String.format("Merge issue revisions %s from branch '%s' to '%s' conflict,  flie list: %s", String.join(",", commits), srcBranch, targetBranch, fileList));
			}
			
			wc.revertCommand();
		} else {
			String mergeCommitId = wc.commit(commitMergerComment);
			ret.put("status", "merged");
			ret.put("mergeCommitId", mergeCommitId);

			logger.info(String.format("Merge issue revisions %s from branch '%s' to '%s' succeed,  flie list: %s", String.join(",", commits), srcBranch, targetBranch, fileList));
		}
	}

	/**
	 * 
	 * @Title: svnBranchMerge   
	 * @Description: TODO  
	 * @param: @param ret
	 * @param: @param wc
	 * @param: @param srcBranch
	 * @param: @param targetBranch
	 * @param: @param commitMergerComment
	 * @param: @throws SVNOpsException      
	 * @return: void      
	 * @throws
	 */
	public void svnBranchMerge(JSONObject ret, SVNWorkingCopy wc, String srcBranch, String targetBranch, String mergeBase,
			String commitMergerComment, Boolean isRevert) throws SVNOpsException {
		
		SVNMergeRangeList mergeRangeList = wc.getMergeRangeList(srcBranch, targetBranch);
		CommitInfo srcBranchHeadCommit = wc.getHeadCommit(srcBranch);
		
		Set<String> fileList = new HashSet<>();
		String status = null;
		List<String> issueNoList = null;
		Map<String, List<CommitInfo>> issueCommitMap = null;
		JSONArray issueCommitList = new JSONArray();
		
		CommitInfo mergeCommit = null;
		
		// 不管是否已合并，本次MR源分支的 start commit 都是源分支的 head commit
		String srcStartCommit = srcBranchHeadCommit.getCommitId();
		
		// 目标分支的  start commit 取 head commit 的 parent（对于已合并的情况，如果经历过多次合并，则不准）。
		String targetStartCommit = null;
		
		// 如果当前目标分支的 mergeinfo 中已经包含源分支的 head，说明已经合并
		if (mergeRangeList != null && mergeRangeList.includes(Long.valueOf(srcBranchHeadCommit.getCommitId()))) {
			status = "finish";
		} else {
			MergeResultInfo mRes = wc.BranchMerge(srcBranch, targetBranch, false);
			if (CollectionUtils.isNotEmpty(mRes.getMergeFileEntrys())) {
				for (MergeFileEntry e: mRes.getMergeFileEntrys()) {
					if (StringUtils.isNotBlank(e.getFilePath())) {
						fileList.add(e.getFilePath());
					}
				}
			}
			
			if (mRes.isConflict()) {
				if (logger.isErrorEnabled()) {
					logger.error(String.format("Merge branch '%s' into '%s' conflict,  flie list: %s", srcBranch, targetBranch, fileList));
				}
				wc.revertCommand();
				status = "conflict";
			} else {
				status = "finish";
				
				List<CommitInfo> commitList = wc.getCommitsForBranchByCommitIdRange(srcBranch, null, mergeBase, 0, false);
				WorkingCopyUtils.setChangeInfo(commitList);
				
				// 映射为 issueNo: commitInfo 格式
				issueCommitMap = WorkingCopyUtils.mapIssueCommit(commitList);
				
				// 将本次合并的所有 commit 中关联的需求号抽取出来
				issueNoList = new ArrayList<>();
				for (String issueNo: issueCommitMap.keySet()) {
					if (StringUtils.isNotBlank(issueNo)) {
						issueNoList.add(issueNo);
					}
					JSONObject issueObj = new JSONObject();
					issueObj.put("issueNo", issueNo);
					issueObj.put("commitList", issueCommitMap.get(issueNo));
					issueCommitList.add(issueObj);
				}
				
				// 将提交关联的所有需求号附加到 merge commit 的 message 开头
				String mergedIssueNoStr = StringUtils.defaultString(StringUtils.join(issueNoList, ","));
				if (StringUtils.isNotBlank(mergedIssueNoStr)) {
					commitMergerComment = mergedIssueNoStr + " " + commitMergerComment;
				}
				
				String mergeCommitId = wc.commit(commitMergerComment);
				mergeCommit = wc.getCommit(Long.valueOf(mergeCommitId));
				
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Merge branch '%s' into '%s' finish,  flie list: %s", srcBranch, targetBranch, fileList));
				}
			}
		}
		
		if (StringUtils.equals(status, "finish")) {
			if (issueCommitMap == null) {
				List<CommitInfo> commitList = wc.getCommitsForBranchByCommitIdRange(srcBranch, null, mergeBase, -1, false);
				WorkingCopyUtils.setChangeInfo(commitList);
				
				// 映射为 issueNo: commitInfo 格式
				issueCommitMap = WorkingCopyUtils.mapIssueCommit(commitList);
				
				// 将本次合并的所有 commit 中关联的需求号抽取出来
				issueNoList = new ArrayList<>();
				for (String issueNo: issueCommitMap.keySet()) {
					if (StringUtils.isNotBlank(issueNo)) {
						issueNoList.add(issueNo);
					}
					
					JSONObject issueObj = new JSONObject();
					issueObj.put("issueNo", issueNo);
					issueObj.put("commitList", issueCommitMap.get(issueNo));
					issueCommitList.add(issueObj);
				}
			}
			
			if (mergeCommit != null) {
				ret.put("mergeCommitId", mergeCommit.getCommitId());
				ret.put("mergeCommit", mergeCommit);
			}
			
			List<CommitInfo> targetCommits = wc.getCommitsForBranch(targetBranch, null, 2, -1, true);
			if (CollectionUtils.isNotEmpty(targetCommits) && targetCommits.size() == 2) {
				targetStartCommit = targetCommits.get(1).getCommitId();
			} else {
				logger.error(String.format("Get commit list of target branch '%s' failed.", targetBranch));
			}
			
			ret.put("srcStartCommit", srcStartCommit);
			ret.put("targetStartCommit", targetStartCommit);
			ret.put("issueCommitList", issueCommitList);
			ret.put("issueNoList", issueNoList);
		}
		
		ret.put("status", status);
	}
	
	public void gitIssueMerge(JSONObject ret, GitWorkingCopy wc, String srcBranch, String targetBranch, JSONArray commitList,
			String commitMergerComment, String commitMerger, String commitMergerEmail) throws GitOpsException {
		if (commitList == null || commitList.isEmpty()) {
			ret.put("status", "invalid");

			return;
		}

		// issue merge, 按照提交的先后顺序逐个合并
		boolean isConflict = false;
		List<JSONObject> commitFileList = new ArrayList<>();
		String commitId = "";
		for (int i = commitList.size() - 1; i >= 0; i--) {
			commitId = commitList.getJSONObject(i).getString("commitId");
			MergeResultInfo mRes = wc.cherryPickCommand(commitId);

			if (CollectionUtils.isNotEmpty(mRes.getMergeFileEntrys())) {
				JSONObject obj = new JSONObject();

				obj.put("fileList", mRes.getMergeFileEntrys());
				obj.put("commitId", commitId);

				commitFileList.add(obj);
			}

			if (mRes.isConflict()) {
				isConflict = true;

				break;
			}
		}

		ret.put("commitFileList", commitFileList);

		if (isConflict) {
			if (logger.isInfoEnabled()) {
				logger.info(String.format("Merge commit %s from branch %s to %s conflict,  flie list: %s", commitId, srcBranch, targetBranch, commitFileList));
				logger.info("revert working copy...");
			}

			ret.put("status", "conflict");
			wc.reset();
		} else {
			String mergeCommitId = wc.commit( commitMerger, commitMergerEmail, commitMergerComment );
			
			ret.put("status", "merged");
			ret.put("mergeCommitId", mergeCommitId);
		}
	}
	
	public void gitBranchMerge(JSONObject ret, GitWorkingCopy wc, String srcBranch, String targetBranch, String mergeBase,
			String commitMergerComment, String commitMerger, String commitMergerEmail, Boolean isRevert) throws GitOpsException {
		
		// 在代码中心创建MR，却线下合并的情况，也能记录合并历史
		Map<String, String> mergeInfo = wc.getMergeInfo(srcBranch, targetBranch, mergeBase);
		boolean isMerged = StringUtils.equals("true", mergeInfo.get("isMerged"));
		String mergeCommitId = null;
		
		if (isMerged) {
			mergeCommitId = mergeInfo.get("mergeCommitId");
			
			ret.put("status", "finish");
			ret.put("srcStartCommit", mergeInfo.get("srcStartCommit"));
			ret.put("targetStartCommit", mergeInfo.get("targetStartCommit"));
			ret.put("mergeBase", mergeInfo.get("mergeBase"));
		} else {
			// 记录分支位置
			String srcStartCommit = wc.resolveBranch(srcBranch);
			String targetStartCommit = wc.resolveBranch(targetBranch);
			
			MergeResultInfo mRes = wc.BranchMerge(srcBranch);
			
			if (CollectionUtils.isNotEmpty(mRes.getMergeFileEntrys())) {
				JSONObject obj = new JSONObject();
				
				obj.put("fileList", mRes.getMergeFileEntrys());
			}
			
			if (mRes.isConflict()) {
				if (logger.isInfoEnabled()) {
					logger.info(String.format("Merge branch from %s to %s conflict,  flie list: %s", srcBranch, targetBranch, mRes.getMergeFileEntrys()));
					logger.info("revert working copy...");
				}
				wc.reset();
				
				ret.put("status", "conflict");

			} else {
				
				// 合并成功, commit之前需要获取到分支的所有关联的需求, 附加到提交message中
				List<CommitInfo> commitList = new ArrayList<>();
				List<String> commitIdList = wc.gitRevListCommand(String.format("%s..%s", targetStartCommit, srcStartCommit));
				for (String commitId: commitIdList) {
					CommitInfo commitInfo = wc.getCommitDetail(commitId);
					WorkingCopyUtils.setChangeInfo(commitInfo, commitInfo.getDiffInfoList());
					commitList.add(commitInfo);
				}
				JSONArray issueCommitList = new JSONArray();
				
				// 映射为 issueNo: commitInfo 格式
				Map<String, List<CommitInfo>> issueCommitMap = WorkingCopyUtils.mapIssueCommit(commitList);
				
				// 将本次合并的所有 commit 中关联的需求号抽取出来
				List<String> issueNoList = new ArrayList<>();
				for (String issueNo: issueCommitMap.keySet()) {
					if (StringUtils.isNotBlank(issueNo)) {
						issueNoList.add(issueNo);
					}
					
					JSONObject issueObj = new JSONObject();
					issueObj.put("issueNo", issueNo);
					issueObj.put("commitList", issueCommitMap.get(issueNo));
					issueCommitList.add(issueObj);
				}
				
				// 将提交关联的所有需求号附加到 merge commit 的 message 开头
				String mergedIssueNoStr = StringUtils.defaultString(StringUtils.join(issueNoList, ","));
				if (StringUtils.isNotBlank(mergedIssueNoStr)) {
					commitMergerComment = mergedIssueNoStr + " " + commitMergerComment;
				}

				mergeCommitId = wc.commit( commitMerger, commitMergerEmail, commitMergerComment );
				wc.push();
				
				// 合并成功后清理源分支
				if (isRevert) {
					wc.deleteLocalBranch(srcBranch);
				}
				
				String newMergeBase = wc.getMergeBaseCommand(srcStartCommit, targetStartCommit);
				
				ret.put("status", "finish");
				ret.put("srcStartCommit", srcStartCommit);
				ret.put("targetStartCommit", targetStartCommit);
				ret.put("mergeBase", newMergeBase);
				
				ret.put("issueCommitList", issueCommitList);
				ret.put("issueNoList", issueNoList);
			}
		}
		
		if (StringUtils.isNotBlank(mergeCommitId)) {
			ret.put("mergeCommitId", mergeCommitId);
			ret.put("mergeCommit", wc.getCommit(mergeCommitId));
		}
	}

	@Override
	public String getName() {
		return null;
	}

/*	@Override
	public JSONArray help() {
		JSONArray jsonArray = new JSONArray();

		ApiHelpUtils.addRepoAuthJsonObj(jsonArray);

		JSONObject jsonObj = new JSONObject();
		jsonObj.put("name", "srcBranch");
		jsonObj.put("type", "String");
		jsonObj.put("desc", "源分支");
		jsonArray.add(jsonObj);

        jsonObj = new JSONObject();
        jsonObj.put("name", "targetBranch");
        jsonObj.put("type", "String");
        jsonObj.put("desc", "目标分支");
        jsonArray.add(jsonObj);

		jsonObj = new JSONObject();
		jsonObj.put("name", "commitList");
		jsonObj.put("type", "JSONArray");
		jsonObj.put("desc",
				"提交ID列表，格式：[\n"
									+ "{\"commitId\":\"f99c6dd31d6c268323042f17f52e86eed2f2c384\"},\n"
									+ "{\"commitId\":\"782c439259952ecb023c01301268453d7864d6a0\"},\n"
									+ "{\"commitId\":\"4d36a0eff183b55b92b33af8a1ebf173e140fbe6\"},\n"
									+ "{\"commitId\":\"faceb3e7e1b4e74329781970028813ea1a8b2b95\"},\n"
									+ "{\"commitId\":\"41e55997da2f604c580ebc69f06442a7f8e4cffa\"}\n" +
								"]");
		jsonArray.add(jsonObj);

		jsonObj = new JSONObject();
		jsonObj.put("name", "commitMerger");
		jsonObj.put("type", "String");
		jsonObj.put("desc", "合并提交人");
		jsonArray.add(jsonObj);

		jsonObj = new JSONObject();
		jsonObj.put("name", "commitMergerEmail");
		jsonObj.put("type", "String");
		jsonObj.put("desc", "合并提交人邮件");
		jsonArray.add(jsonObj);

		jsonObj = new JSONObject();
		jsonObj.put("name", "commitMergerComment");
		jsonObj.put("type", "String");
		jsonObj.put("desc", "合并提交描述");
		jsonArray.add(jsonObj);

		jsonObj = new JSONObject();
		jsonObj.put("name", "isRevert");
		jsonObj.put("type", "Boolean");
		jsonObj.put("desc", "是否为revert");
		jsonArray.add(jsonObj);

		jsonObj = new JSONObject();
		jsonObj.put("name", "mergeBaseOnly");
		jsonObj.put("type", "Boolean");
		jsonObj.put("desc", "是否只取mergeBase，不做合并");
		jsonArray.add(jsonObj);

		jsonObj = new JSONObject();
		jsonObj.put("name", "mergeBase");
		jsonObj.put("type", "String");
		jsonObj.put("desc", "mergeBase commitId");
		jsonArray.add(jsonObj);

		ApiHelpUtils.addSVNWorkingCopyPathJsonObj(jsonArray);

		return jsonArray;
	}*/

}
