package com.techsure.autoexecrunner.codehub.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.codehub.dto.commit.CommitInfo;
import com.techsure.autoexecrunner.codehub.dto.diff.FileDiffInfo;
import com.techsure.autoexecrunner.codehub.git.GitWorkingCopy;
import com.techsure.autoexecrunner.codehub.svn.SVNWorkingCopy;
import com.techsure.autoexecrunner.codehub.utils.JSONUtils;
import com.techsure.autoexecrunner.codehub.utils.WorkingCopyUtils;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.restful.annotation.Description;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Output;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class CommitDetailApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "获取提交详情";
    }

    @Input({
            @Param(name = "repoType", type = ApiParamType.STRING, desc = "仓库类型"),
            @Param(name = "url", type = ApiParamType.STRING, desc = "仓库url"),
            @Param(name = "username", type = ApiParamType.STRING, desc = "用户"),
            @Param(name = "password", type = ApiParamType.STRING, desc = "密码"),
            @Param(name = "commitList", type = ApiParamType.JSONARRAY, desc = "commit列表"),
            @Param(name = "srcStartCommit", type = ApiParamType.STRING, desc = "开始提交源"),
            @Param(name = "targetStartCommit", type = ApiParamType.STRING, desc = "开始提交目标"),
            @Param(name = "targetEndCommit", type = ApiParamType.STRING, desc = "结束提交目标"),
            @Param(name = "srcEndCommit", type = ApiParamType.STRING, desc = "结束提交源"),
            @Param(name = "srcBranch", type = ApiParamType.STRING, desc = "源分支"),
            @Param(name = "targetBranch", type = ApiParamType.STRING, desc = "目标分支"),
    })
    @Output({
    })
    @Description(desc = "获取提交详情接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String repoType = JSONUtils.optString(jsonObj,"repoType", "").trim().toLowerCase();
        String url = JSONUtils.optString(jsonObj,"url", "").trim();
        String username = JSONUtils.optString(jsonObj,"username", "");
        String pwd = JSONUtils.optString(jsonObj,"password", "");
        JSONArray commitList = jsonObj.getJSONArray("commitList");

        String srcStartCommit = JSONUtils.optString(jsonObj,"srcStartCommit", null);
        String targetStartCommit = JSONUtils.optString(jsonObj,"targetStartCommit", "");
        String targetEndCommit = JSONUtils.optString(jsonObj,"targetEndCommit", "");
        String srcEndCommit = JSONUtils.optString(jsonObj,"srcEndCommit", "");

        String srcBranch = JSONUtils.optString(jsonObj,"srcBranch");
        String targetBranch = JSONUtils.optString(jsonObj,"targetBranch");

        String wcPath = WorkingCopyUtils.getWcPath(jsonObj);

        JSONObject jsonObject = new JSONObject();

        if (repoType.equals("svn")) {
            Collection<?> srcCommitIdList = null;
            SVNWorkingCopy wc = null;
            try {
                wc = new SVNWorkingCopy(wcPath, url, username, pwd, JSONUtils.optString(jsonObj,"mainBranch", ""), JSONUtils.optString(jsonObj,"branchesPath", ""), JSONUtils.optString(jsonObj,"tagsPath", ""));

                // 没有提供 commitList, 则从目标分支的 mergeinfo 中获取本次MR合并的内容
                if (CollectionUtils.isEmpty(commitList)) {
                    if (StringUtils.isBlank(srcBranch) || StringUtils.isBlank(srcEndCommit)
                            || StringUtils.isBlank(targetBranch) || StringUtils.isBlank(targetEndCommit)) {
                        throw new RuntimeException("请指定参数 srcBranch, srcEndCommit, targetBranch, targetEndCommit");
                    }

                    String[] oldMergedCommits = wc.getSrcBranchMergedCommitsFromMergeInfo(srcBranch, srcEndCommit, targetBranch, targetEndCommit);
                    String[] allMergedCommits = wc.getSrcBranchMergedCommitsFromMergeInfo(srcBranch, srcEndCommit, targetBranch, null);
                    if (allMergedCommits != null && allMergedCommits.length != 0) {
                        if (oldMergedCommits == null || oldMergedCommits.length == 0) {
                            srcCommitIdList = Arrays.asList(allMergedCommits);
                        } else {
                            srcCommitIdList = CollectionUtils.subtract(Arrays.asList(allMergedCommits), Arrays.asList(oldMergedCommits));
                        }
                    }
                } else {
                    srcCommitIdList = commitList;
                }

                List<CommitInfo> mergeCommitList = wc.getCommitsForBranchByCommitIdRange(targetBranch, null, targetEndCommit, -1, true);
                jsonObject.put("targetCommitList", mergeCommitList);

                JSONArray srcCommitList = new JSONArray();
                // 获取源分支上的 commit 的详细信息
                for (Object commit: srcCommitIdList) {
                    CommitInfo commitInfo = wc.getCommitDetail(Long.valueOf((String)commit));
                    List<FileDiffInfo> diffInfoList = commitInfo.getDiffInfoList();
                    WorkingCopyUtils.setChangeInfo(commitInfo, diffInfoList);

                    srcCommitList.add(commitInfo);
                }

                jsonObject.put("srcCommitList", srcCommitList);
            } finally {
                if (wc != null) {
                    wc.close();
                }
            }
        } else if (repoType.equals("gitlab")) {

            // git分支型传入start end, 需要同时返回issue, issue关联的commit, 和commit的diff
            GitWorkingCopy wc = null;
            try {
                wc = new GitWorkingCopy(wcPath, url, username, pwd);
                wc.update();

                jsonObject = WorkingCopyUtils.getGitCommitDetail(wc, srcStartCommit, targetStartCommit);
            } finally {
                if (wc != null) {
                    wc.close();
                }
            }
        }

        return jsonObject;
    }

    @Override
    public String getToken() {
        return "codehub/commit/commitdetail";
    }
}
