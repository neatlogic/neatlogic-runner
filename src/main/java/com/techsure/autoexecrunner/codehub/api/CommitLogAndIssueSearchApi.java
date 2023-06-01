package com.techsure.autoexecrunner.codehub.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.codehub.dto.commit.CommitInfo;
import com.techsure.autoexecrunner.codehub.exception.GitOpsException;
import com.techsure.autoexecrunner.codehub.git.GitWorkingCopy;
import com.techsure.autoexecrunner.codehub.svn.SVNWorkingCopy;
import com.techsure.autoexecrunner.codehub.utils.JSONUtils;
import com.techsure.autoexecrunner.codehub.utils.WorkingCopyUtils;
import com.techsure.autoexecrunner.common.config.Config;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Output;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @author yujh
 *
 *         根据分支,最大检索条数等查询commit和commit包含的issue信息, 支持前后方向的搜索, 配合commit入库功能
 *         
 *         由于startTime判断的先后顺序关系,增量搜索可能会包含starttime当前的commit在内,也不排除可能存在时间相同的commit, startTime传入毫秒时间戳即可
 *         
 *         返回的是commit的列表, commit可能会关联多个需求
 */
@Service
@Deprecated
public class CommitLogAndIssueSearchApi extends PrivateApiComponentBase {

	@Override
	public String getToken() {
		return "codehu/commit/commitlogandissuesearch";
	}

	@Override
	public String getName() {
		return "查询commit和commit包含的issue信息";
	}


	@Input({
			@Param(name = "repoType", type = ApiParamType.STRING, desc = "仓库类型"),
			@Param(name = "url", type = ApiParamType.STRING, desc = "url", isRequired = true),
			@Param(name = "username", type = ApiParamType.STRING, desc = "用户"),
			@Param(name = "password", type = ApiParamType.STRING, desc = "密码"),
			@Param(name = "srcBranch", type = ApiParamType.STRING, desc = "源分支"),
			@Param(name = "targetBranch", type = ApiParamType.STRING, desc = "目标分支"),
			@Param(name = "maxSearchCount", type = ApiParamType.INTEGER, desc = "提交最大搜索数量"),
			@Param(name = "startCommitId", type = ApiParamType.STRING, desc = "获取日志的启始提交ID， null代表从头部开始"),
			@Param(name = "startTime", type = ApiParamType.STRING, desc = "最早的提交的时间（epochTime），0:不限制，否则获取的提交的时间要大于等于startTime，毫秒时间戳"),
			@Param(name = "mainBranch", type = ApiParamType.STRING, desc = "主分支"),
	})
	@Output({
	})
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {

		int MAXCOUNT = Config.MAX_GET_COMMIT_LOG; // 需要从配置文件中配置 暂定

		String repoType = JSONUtils.optString(jsonObj,"repoType", "").trim().toLowerCase();
		String url = JSONUtils.optString(jsonObj,"url", "").trim();
		String username = JSONUtils.optString(jsonObj,"username", "");
		String pwd = JSONUtils.optString(jsonObj,"password", "");
		String branch = JSONUtils.optString(jsonObj,"srcBranch", "");
		String targetBranch = JSONUtils.optString(jsonObj,"targetBranch", "");
		int maxSearchCount = JSONUtils.optInt(jsonObj,"maxSearchCount");
		if (maxSearchCount == 0) {
			maxSearchCount = MAXCOUNT;
		}

		String startCommitId = JSONUtils.optString(jsonObj,"startCommitId", "");
		long startTime = JSONUtils.optLong(jsonObj,"startTime", 0L);

		String wcPath = WorkingCopyUtils.getWcPath(jsonObj);

		List<CommitInfo> commitInfoList = new ArrayList<>();
		if (repoType.equals("svn")) {
			SVNWorkingCopy wc = new SVNWorkingCopy(wcPath, url, username, pwd , JSONUtils.optString(jsonObj,"mainBranch",""), JSONUtils.optString(jsonObj,"branchesPath",""), JSONUtils.optString(jsonObj,"tagsPath",""));
			// svn暂时没实现fork point查询
			if (startTime != 0) {
				startTime = startTime / 1000;
				// 每次必须读取增量的commit
				commitInfoList = wc.getCommitsForBranch(branch, null, MAXCOUNT, startTime, true);

				if (StringUtils.isNotEmpty(startCommitId)) {
					// 以后进入之后的读取,判断是否要读取旧的commit
					commitInfoList.addAll(wc.getCommitsForBranch(branch, startCommitId, maxSearchCount, 0, true));
				}

			} else {
				// 首次读取
				commitInfoList = wc.getCommitsForBranch(branch, null, maxSearchCount, 0, true);
			}

			wc.close();

		} else if (repoType.equals("gitlab")) {
			GitWorkingCopy wc = new GitWorkingCopy(wcPath, url, username, pwd);
			wc.update();
			if (startTime != 0) {

				// 时间是秒数
				startTime = startTime / 1000;

				// 每次必须读取增量的commit
				commitInfoList = gitGetCommitsForBranch(wc, branch, targetBranch, null, MAXCOUNT, startTime);

				if (StringUtils.isNotEmpty(startCommitId)) {
					// 以后进入之后的读取,判断是否要读取旧的commit
					commitInfoList.addAll(gitGetCommitsForBranch(wc, branch, targetBranch, startCommitId, maxSearchCount, 0));
				}

			} else {
				// 首次读取
				commitInfoList = gitGetCommitsForBranch(wc, branch, targetBranch, null, maxSearchCount, 0);
			}

			// 需要close 防止内存泄露
			wc.close();
		}

		if (CollectionUtils.isNotEmpty(commitInfoList)) {
			for (int i = 0; i < commitInfoList.size(); i++) {
				CommitInfo commitInfo = commitInfoList.get(i);
				parseCommentIssueNo(commitInfo);
			}
		}

		// svn commit查询的方法 查出来的commit顺序不按照时间排序
		Collections.sort(commitInfoList, new Comparator<CommitInfo>() {
			@Override
			public int compare(CommitInfo o1, CommitInfo o2) {
				if (o1.getCommitterDate() == null && o2.getCommitterDate() == null) {
					return 0;
				}

				if (o1.getCommitterDate() == null) {
					return -1;
				}

				if (o2.getCommitterDate() == null) {
					return 1;
				}

				return Long.compare(o1.getCommitterDate().getTime(), o2.getCommitterDate().getTime());
			}
		});

		return commitInfoList;
	}

	private List<CommitInfo> gitGetCommitsForBranch(GitWorkingCopy wc, String branch, String targetBranch, String startCommitId, int maxSearchCount, long startTime) throws GitOpsException {
		// 内部封装的是两个不同的方法, 需要处理下目标分支为空的情况
		if (StringUtils.isNotEmpty(targetBranch)) {
			return wc.getCommitsForBranch(branch, targetBranch, startCommitId, maxSearchCount, startTime, true);
		} else {
			return wc.getCommitsForBranch(branch, startCommitId, maxSearchCount, startTime, true);
		}
	}

	private void parseCommentIssueNo(CommitInfo commitInfo) {
		Matcher matcher = null;
		if (Config.ISSUE_PATTERN != null && StringUtils.isNotEmpty(commitInfo.getComment())) {

			matcher = Config.ISSUE_PATTERN.matcher(commitInfo.getComment());

			if (matcher.find() && matcher.groupCount() >= 1) {
				commitInfo.setIssueNo(matcher.group(1));
			}
		}

	}
}
