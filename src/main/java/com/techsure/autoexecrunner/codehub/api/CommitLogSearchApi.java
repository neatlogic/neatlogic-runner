package com.techsure.autoexecrunner.codehub.api;

import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.codehub.dto.commit.CommitInfo;
import com.techsure.autoexecrunner.codehub.git.GitWorkingCopy;
import com.techsure.autoexecrunner.codehub.svn.SVNWorkingCopy;
import com.techsure.autoexecrunner.codehub.utils.JSONUtils;
import com.techsure.autoexecrunner.codehub.utils.WorkingCopyUtils;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;
import com.techsure.autoexecrunner.restful.annotation.Description;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Output;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**根据分支名称或者标签名称查询commitlog列表(支持分页查询)
 *
 * @author fengt
 */
@Component
public class CommitLogSearchApi extends PrivateApiComponentBase {

	@Override
	public String getToken() {
		return "codehub/commitlog/search";
	}


	@Override
	public String getName() {
		return "根据分支名称或者标签名称查询commitlog列表";
	}

	@Input({
			@Param(name = "repoType", type = ApiParamType.STRING, desc = "仓库类型"),
			@Param(name = "url", type = ApiParamType.STRING, desc = "url"),
			@Param(name = "username", type = ApiParamType.STRING, desc = "用户"),
			@Param(name = "password", type = ApiParamType.STRING, desc = "密码"),
			@Param(name = "queryType", type = ApiParamType.STRING, desc = "查询类型"),
			@Param(name = "queryName", type = ApiParamType.STRING, desc = "查询字段名称",isRequired = true),
			@Param(name = "startCommitId", type = ApiParamType.STRING, desc = "获取日志的启始提交ID， null代表从头部开始"),
			@Param(name = "endCommitId", type = ApiParamType.STRING, desc = "获取日志的终止提交ID"),
			@Param(name = "startTime", type = ApiParamType.STRING, desc = "最早的提交的时间（epochTime），0:不限制，否则获取的提交的时间要大于等于startTime，毫秒时间戳"),
			@Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页查询条数,默认是10"),
	})
	@Output({
	})
	@Description(desc = "查询commitlog列表接口")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		String repoType = JSONUtils.optString(jsonObj,"repoType", "").trim().toLowerCase();
		String url = JSONUtils.optString(jsonObj,"url", "").trim();
		String username = JSONUtils.optString(jsonObj,"username", "");
		String pwd = JSONUtils.optString(jsonObj,"password", "");
		String queryType = JSONUtils.optString(jsonObj, "queryType","branch" );
		String queryName = JSONUtils.optString(jsonObj,"queryName", "");
		String startCommitId = JSONUtils.optString(jsonObj,"startCommitId", null);
		String endCommitId = JSONUtils.optString(jsonObj,"endCommitId", null);
		int pageSize = JSONUtils.optInt(jsonObj, "pageSize",10 );
		long startTime = JSONUtils.optLong(jsonObj,"startTime", 0L);

		if (StringUtils.isBlank(queryName)) {
			throw new ApiRuntimeException("查询字段名称（标签名称或者分支名称）不能为空");
		}
		if(StringUtils.isBlank( startCommitId )){
			startCommitId = null;
		}
		if (StringUtils.isBlank(endCommitId)) {
			endCommitId = null;
		} else {
			// 指定了结束点就不能限制条数, 否则会导致merge commit获取不全
			pageSize = 0;
		}

		String wcPath = WorkingCopyUtils.getWcPath(jsonObj);

		List<CommitInfo> commitInfoList = null;
		if (repoType.equals("svn")) {
			SVNWorkingCopy wc = null;
			try {
				wc = new SVNWorkingCopy(wcPath, url, username, pwd , JSONUtils.optString(jsonObj,"mainBranch",""), JSONUtils.optString(jsonObj,"branchesPath",""), JSONUtils.optString(jsonObj,"tagsPath",""));
				if("branch".equalsIgnoreCase( queryType )){
					if(!wc.hasBranch( queryName )){
						throw new ApiRuntimeException("分支\""+ queryName +"\"不存在");
					}
					if (startTime > 0){
						commitInfoList = wc.getCommitsForBranch(queryName, startCommitId, pageSize, startTime,true);

					}else {
						commitInfoList = wc.getCommitsForBranchByCommitIdRange(queryName, startCommitId, endCommitId, pageSize, true);
					}
				}
				else{
					if(!wc.hasTag( queryName )){
						throw new ApiRuntimeException("标签\""+ queryName +"\"不存在");
					}
					commitInfoList = wc.getCommitsForTag(queryName, startCommitId, pageSize, startTime, true);
				}

			} finally {
				if(wc != null){
					// 需要close 防止内存泄露
					wc.close();
				}
			}

		} else if (repoType.equals("gitlab")) {
			GitWorkingCopy wc = null;
			try {
				wc = new GitWorkingCopy(wcPath, url, username, pwd);
				wc.update();

				if("branch".equalsIgnoreCase( queryType )){
					if(!wc.hasBranch( queryName )){
						throw new ApiRuntimeException("分支\""+ queryName +"\"不存在");
					}

					commitInfoList = wc.getCommitsForBranch(queryName, startCommitId, pageSize, startTime ,true);
				}
				else{
					if(!wc.hasTag( queryName )){
						throw new ApiRuntimeException("标签\""+ queryName +"\"不存在");
					}
					commitInfoList = wc.getCommitsForTag(queryName, startCommitId, pageSize, startTime, true);
				}
			} finally {
				if (wc != null) {
					// 需要close 防止内存泄露
					wc.close();
				}
			}
		}

		return commitInfoList;
	}
}
