package com.techsure.autoexecrunner.codehub.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.codehub.dto.cache.Cache;
import com.techsure.autoexecrunner.codehub.dto.commit.CommitInfo;
import com.techsure.autoexecrunner.codehub.git.GitWorkingCopy;
import com.techsure.autoexecrunner.codehub.svn.SVNWorkingCopy;
import com.techsure.autoexecrunner.codehub.utils.JSONUtils;
import com.techsure.autoexecrunner.codehub.utils.WorkingCopyUtils;
import com.techsure.autoexecrunner.common.config.Config;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;
import com.techsure.autoexecrunner.restful.annotation.Description;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.tmatesoft.svn.core.SVNMergeRangeList;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author zouye
 * @Date 2020-11-02
 * @Description 根据源分支、目标分支搜索 commit 及其关联的需求列表
 **/
@Service
public class IssueCommitSearchApi extends PrivateApiComponentBase {
    private static Logger logger = LoggerFactory.getLogger(IssueCommitSearchApi.class);

    @Override
    public String getToken() {
        return "codehub/issue/commit/search";
    }


    @Override
    public String getName() {
        return null;
    }

    @Input({
            @Param(name = "repoType", type = ApiParamType.STRING, desc = "仓库类型"),
            @Param(name = "url", type = ApiParamType.STRING, desc = "url"),
            @Param(name = "username", type = ApiParamType.STRING, desc = "用户"),
            @Param(name = "password", type = ApiParamType.STRING, desc = "密码"),
            @Param(name = "srcBranch", type = ApiParamType.STRING, desc = "源分支"),
            @Param(name = "targetBranch", type = ApiParamType.STRING, desc = "目标分支"),
            @Param(name = "maxSearchCount", type = ApiParamType.INTEGER, desc = "最大搜索数"),
            @Param(name = "issueList", type = ApiParamType.JSONARRAY, desc = "issue列表"),
            @Param(name = "issueOnly", type = ApiParamType.BOOLEAN, desc = ""),
            @Param(name = "groupByIssue", type = ApiParamType.BOOLEAN, desc = ""),
            @Param(name = "srcStartCommit", type = ApiParamType.STRING, desc = "开始提交源"),
            @Param(name = "srcEndCommit", type = ApiParamType.STRING, desc = "结束提交源"),
            @Param(name = "repositoryServiceId", type = ApiParamType.LONG, desc = "仓库服务id"),
            @Param(name = "repositoryId", type = ApiParamType.LONG, desc = "仓库id"),
            @Param(name = "forceFlush", type = ApiParamType.BOOLEAN, desc = "强制刷新"),
            @Param(name = "onlyOpenStatusCommit", type = ApiParamType.BOOLEAN, desc = "是否只展示开启的commit")
    })
    @Description(desc = "查看提交issue接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        String repoType = JSONUtils.optString(jsonObj, "repoType", "").trim().toLowerCase();
        String url = JSONUtils.optString(jsonObj, "url", "").trim();
        String username = JSONUtils.optString(jsonObj, "username", "");
        String pwd = JSONUtils.optString(jsonObj, "password", "");
        String srcBranch = JSONUtils.optString(jsonObj, "srcBranch", "");
        String targetBranch = JSONUtils.optString(jsonObj, "targetBranch", "");
        int maxSearchCount = JSONUtils.optInt(jsonObj, "maxSearchCount", Config.MAX_GET_COMMIT_LOG);
        JSONArray issueList = jsonObj.getJSONArray("issueList");

        boolean issueOnly = JSONUtils.optBoolean(jsonObj, "issueOnly", false);
        boolean groupByIssue = JSONUtils.optBoolean(jsonObj, "groupByIssue", false);

        String wcPath = WorkingCopyUtils.getWcPath(jsonObj);
        String srcStartCommit = JSONUtils.optString(jsonObj, "srcStartCommit", null);
        String srcEndCommit = JSONUtils.optString(jsonObj, "srcEndCommit", null);

        Long repositoryServiceId = jsonObj.getLong("repositoryServiceId");
        Long repositoryId = jsonObj.getLong("repositoryId");
        boolean forceFlush = JSONUtils.optBoolean(jsonObj, "forceFlush", false);

        // MR创建页面 搜索commit只展示未合并过的commit
        boolean onlyOpenStatusCommit = JSONUtils.optBoolean(jsonObj, "onlyOpenStatusCommit", false);

        JSONArray retList = new JSONArray();
        if (repoType.equals("svn")) {
            SVNWorkingCopy wc = null;
            try {
                wc = new SVNWorkingCopy(wcPath, url, username, pwd, JSONUtils.optString(jsonObj, "mainBranch", ""), JSONUtils.optString(jsonObj, "branchesPath", ""), JSONUtils.optString(jsonObj, "tagsPath", ""));
                List<CommitInfo> commitInfoList = new ArrayList<>();

                if (Config.CACHE_ENABLE) {
                    Cache cache = new Cache(repositoryServiceId, repositoryId, srcBranch);

                    // --bug=1008004 --user=邹叶 【代码中心】码线上有提交，创建MR时却提示需求无效 https://www.tapd.cn/54247054/s/1154320
                    // srcEndCommit 为空，说明请求来自创建MR，此时走强制刷新：从远程取提交并更新本地缓存。
                    // 也可以从缓存取一部分，再从远程取增量部分，但是这样还是处理不了历史提交的需求号被修改的情况。
                    if (StringUtils.isBlank(srcEndCommit)) {
                        forceFlush = true;
                    }

                    if (!forceFlush) {
                        JSONArray cacheList = null;
                        boolean needMore = false;

                        // 已合并的mr会提供 srcStartCommit，不管 mexSearchCount有多大，都只取 srcStartCommit, srcEndCommit 之间的 commit
                        if (StringUtils.isNotBlank(srcStartCommit)) {
                            cacheList = cache.getSVNCommitsFromCache(srcStartCommit, srcEndCommit, maxSearchCount);
                        } else {
                            // 没有提供 srcStartCommit，则使用缓存的 head 作为 srcStartCommit
                            String cacheHead = cache.getHead();

                            // 如果没有取到缓存的head，认为没有缓存
                            if (StringUtils.isNotBlank(cacheHead)) {
                                cacheList = cache.getSVNCommitsFromCache(cacheHead, srcEndCommit, maxSearchCount);

                                // 没有提供 srcStartCommit，则必须返回 maxSearchCount 个 commit
                                // 缓存中的 commit 不够，就再实时获取
                                if (CollectionUtils.isEmpty(cacheList)
                                        || Long.parseLong(srcEndCommit) > cacheList.getJSONObject(cacheList.size() - 1).getLong("commitId")
                                        || cacheList.size() < maxSearchCount) {    // commit数量不满足maxSearchCount,必须needMore返回maxSearchCount个commit
                                    needMore = true;
                                }
                            }
                        }

                        // 没有缓存，实时从远程仓库获取
                        if (CollectionUtils.isEmpty(cacheList)) {
                            needMore = true;
                        } else if (needMore) { // 有缓存，但是数量不够
                            srcEndCommit = cacheList.getJSONObject(0).getString("commitId");
                            maxSearchCount = maxSearchCount - cacheList.size();
                        }

                        if (needMore) {
                            commitInfoList = wc.getBranchCommitListByCommitIdRange(srcBranch, srcStartCommit, srcEndCommit, maxSearchCount);
                            if (CollectionUtils.isNotEmpty(commitInfoList)) {
                                if (CollectionUtils.isEmpty(cacheList)) {
                                    cache.writeCommitsToCache(JSONArray.parseArray(JSON.toJSONString(commitInfoList)));
                                } else {
                                    // 防止刚好出现srcEndCommit commit重复
                                    int size = commitInfoList.size();
                                    if (CollectionUtils.isNotEmpty(cacheList)
                                            && commitInfoList.get(commitInfoList.size() - 1).getCommitId().equals(cacheList.getJSONObject(0).getString("commitId"))) {
                                        commitInfoList.remove(size - 1);
                                    }

                                    if (CollectionUtils.isNotEmpty(commitInfoList)) {
                                        cache.updateSVNCommitsToCache(commitInfoList.get(0).getCommitId(), JSONArray.parseArray(JSON.toJSONString(commitInfoList)));
                                    }

                                    commitInfoList.addAll(JSONArray.parseArray(cacheList.toJSONString(), CommitInfo.class));
                                }

                                cache.setHead(commitInfoList.get(0).getCommitId());
                            }
                        } else {
                            commitInfoList.addAll(JSONArray.parseArray(cacheList.toJSONString(), CommitInfo.class));
                        }

                    } else {
                        commitInfoList = wc.getBranchCommitListByCommitIdRange(srcBranch, srcStartCommit, srcEndCommit, maxSearchCount);
                        cache.updateSVNCommitsToCache(commitInfoList.get(0).getCommitId(), JSONArray.parseArray(JSON.toJSONString(commitInfoList)));

                        long cacheHead = NumberUtils.toLong(cache.getHead(), 0);

                        // 【缓存读写异常, 缓存还是存在重复的commit的问题】https://www.tapd.cn/54247054/bugtrace/bugs/view?bug_id=1154247054001008118
                        // bug , 没有更新head
                        if (Long.parseLong(commitInfoList.get(0).getCommitId()) > cacheHead) {
                            cache.setHead(commitInfoList.get(0).getCommitId());
                        }
                    }


                } else {
                    commitInfoList = wc.getBranchCommitListByCommitIdRange(srcBranch, srcStartCommit, srcEndCommit, maxSearchCount);
                }

                if (CollectionUtils.isNotEmpty(commitInfoList)) {
                    if (StringUtils.isNotBlank(targetBranch)) {

                        // 获取目标分支上copy时的rev号, 源分支上比它还小的rev都是不需要的
                        long copyRevision = wc.getCopyRevision(targetBranch);

                        SVNMergeRangeList rangeList = wc.getMergeRangeList(srcBranch, targetBranch);
                        if (rangeList != null) {

                            Iterator<CommitInfo> it = commitInfoList.iterator();
                            while (it.hasNext()) {
                                CommitInfo commitInfo = it.next();
                                commitInfo.setMergeStatus(rangeList.includes(Long.parseLong(commitInfo.getId())) ? "merged" : "open");

                                // 只需要展示未合并的commit
                                if (onlyOpenStatusCommit) {

                                    if (copyRevision > Long.parseLong(commitInfo.getCommitId()) || !commitInfo.getMergeStatus().equals("open")) {
                                        it.remove();
                                    }
                                }
                            }

                        }
                    }
                    // 如果参数未指定需求号列表，就不过滤需求
                    if (CollectionUtils.isNotEmpty(issueList)) {
                        for (int i = 0; i < issueList.size(); i++) {
                            String issueNo = issueList.getString(i);
                            JSONObject issueObj = new JSONObject();
                            boolean isIssueMerged = true;
                            issueObj.put("issueNo", issueNo);

                            JSONArray issueCommitList = new JSONArray();
                            for (CommitInfo commitInfo : commitInfoList) {

                                if (StringUtils.isBlank(commitInfo.getComment())) {
                                    continue;
                                }

                                // 可能一个 commit 会关联多个 issue
                                Set<String> commitIssueSet = WorkingCopyUtils.parseCommentIssueNo(commitInfo.getComment());
                                if (commitIssueSet.contains(issueNo)) {

                                    CommitInfo newCommitInfo = new CommitInfo();
                                    // 防止共用bean对象, 做到一个issue一个commitVo
                                    BeanUtils.copyProperties(commitInfo, newCommitInfo);
                                    newCommitInfo.setIssueNo(issueNo);
                                    issueCommitList.add(newCommitInfo);


                                    if (isIssueMerged && commitInfo.getMergeStatus().equals("open")) {
                                        isIssueMerged = false;
                                    }
                                }
//								
                            }

                            if (issueCommitList.isEmpty()) {
                                issueObj.put("status", "invalid");
                            } else {
                                issueObj.put("status", isIssueMerged ? "merged" : "open");
                            }

                            issueObj.put("commitList", issueCommitList);
                            if (groupByIssue) {
                                retList.add(issueObj);
                            } else {
                                retList.addAll(issueCommitList);
                            }
                        }
                    } else {
                        Map<String, List<CommitInfo>> issueMap = new HashMap<>();
                        for (CommitInfo commitInfo : commitInfoList) {

                            Set<String> issueSet = WorkingCopyUtils.parseCommentIssueNo(commitInfo.getMessage());
                            if (CollectionUtils.isEmpty(issueSet)) {
                                if (issueOnly) {
                                    continue;
                                } else {
                                    issueSet.add("");
                                }
                            }

                            if (groupByIssue) {
                                for (String issueNo : issueSet) {
                                    CommitInfo newCommitInfo = new CommitInfo();
                                    // 防止共用bean对象, 做到一个issue一个commitVo
                                    BeanUtils.copyProperties(commitInfo, newCommitInfo);
                                    newCommitInfo.setIssueNo(issueNo);
                                    if (issueMap.get(issueNo) == null) {
                                        issueMap.put(issueNo, new ArrayList<>());
                                    }
                                    issueMap.get(issueNo).add(newCommitInfo);
                                }
                            } else {
                                commitInfo.setIssueNo(StringUtils.join(issueSet, ","));
                            }
                        }
                        if (groupByIssue) {
                            if (!issueMap.isEmpty()) {
                                for (String issueNo : issueMap.keySet()) {
                                    JSONObject issueObj = new JSONObject();
                                    issueObj.put("issueNo", issueNo);
                                    issueObj.put("commitList", issueMap.get(issueNo));

                                    retList.add(issueObj);
                                }
                            }
                        } else {
                            retList = JSONArray.parseArray(JSON.toJSONString(commitInfoList));
                        }

                    }
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                throw new ApiRuntimeException(ex);
            } finally {
                if (wc != null) {
                    wc.close();
                }
            }
        } else {
            GitWorkingCopy wc = null;
            try {
                wc = new GitWorkingCopy(wcPath, url, username, pwd);
                wc.update();
//				wc.checkout(targetBranch, true);
                Cache cache = new Cache(repositoryServiceId, repositoryId, srcBranch);

                String targetStartCommit = JSONUtils.optString(jsonObj, "targetStartCommit", "");
                List<String> commitIdList = null;
                List<CommitInfo> commitInfoList = null;

                if (StringUtils.isBlank(srcStartCommit) || StringUtils.isBlank(targetStartCommit)) { // not merged
                    targetStartCommit = wc.resolveBranch(targetBranch);
                    srcStartCommit = wc.resolveBranch(srcBranch);

                    if (StringUtils.isBlank(targetStartCommit)) {
                        throw new ApiRuntimeException(String.format("branch '%s' is not exist", targetBranch));
                    }

                    if (StringUtils.isBlank(srcStartCommit)) {
                        throw new ApiRuntimeException(String.format("branch '%s' is not exist", srcBranch));
                    }
                }

                commitIdList = wc.gitRevListCommand(String.format("%s..%s", targetStartCommit, srcStartCommit), "--max-count=" + maxSearchCount);

                List<String> mismatchCommitIdList = new ArrayList<>();
                if (Config.CACHE_ENABLE && !forceFlush) {
                    Map<String, Object> cacheRetMap = cache.readGitCommitsFromCache(commitIdList);
                    if (cacheRetMap != null) {
                        JSONArray cacheCommitList = null;
                        if (cacheRetMap.get("matchedCommitList") != null) {
                            cacheCommitList = (JSONArray) cacheRetMap.get("matchedCommitList");
                        }

                        if (CollectionUtils.isNotEmpty(cacheCommitList)) {
                            commitInfoList = JSONArray.parseArray(cacheCommitList.toJSONString(), CommitInfo.class);
                        }

                        if (cacheRetMap.get("mismatchCommitIdList") != null) {
                            mismatchCommitIdList = (List<String>) cacheRetMap.get("mismatchCommitIdList");
                        }
                    }
                } else {
                    mismatchCommitIdList = commitIdList;
                }

                if (CollectionUtils.isNotEmpty(mismatchCommitIdList)) {
                    List<CommitInfo> noCacheList = new ArrayList<>();
                    for (String commitId : mismatchCommitIdList) {
                        noCacheList.add(wc.getCommit(commitId));
                    }

                    if (CollectionUtils.isEmpty(commitInfoList)) {
                        commitInfoList = noCacheList;
                    } else {
                        commitInfoList.addAll(noCacheList);
                    }

                    if (Config.CACHE_ENABLE) {
                        cache.writeGitCommitsToCache(JSONArray.parseArray(JSON.toJSONString(noCacheList)));
                    }
                }
                // 分支型
                if (CollectionUtils.isNotEmpty(commitInfoList)) {
                    Map<String, List<CommitInfo>> issueMap = new HashMap<>();
                    for (CommitInfo commitInfo : commitInfoList) {
                        String message = StringUtils.isBlank(commitInfo.getComment()) ? "" : commitInfo.getComment();

                        Set<String> issueSet = WorkingCopyUtils.parseCommentIssueNo(message);
                        if (CollectionUtils.isEmpty(issueSet) && issueOnly) {
                            continue;
                        }

                        // 处理空需求
                        if (CollectionUtils.isEmpty(issueSet)) {
                            issueSet.add("");
                        }

                        commitInfo.setIssueNo(StringUtils.join(issueSet, ","));
                        for (String issueNo : issueSet) {
                            CommitInfo newCommitInfo = new CommitInfo();
                            // 防止共用bean对象, 做到一个issue一个commitVo
                            BeanUtils.copyProperties(commitInfo, newCommitInfo);
                            newCommitInfo.setIssueNo(issueNo);
                            if (issueMap.get(issueNo) == null) {
                                issueMap.put(issueNo, new ArrayList<>());
                            }
                            issueMap.get(issueNo).add(newCommitInfo);
                        }
                    }
                    if (groupByIssue) {
                        if (!issueMap.isEmpty()) {
                            for (String issueNo : issueMap.keySet()) {
                                JSONObject issueObj = new JSONObject();
                                issueObj.put("issueNo", issueNo);
                                issueObj.put("commitList", issueMap.get(issueNo));

                                retList.add(issueObj);
                            }
                        }
                    } else {
                        retList = JSONArray.parseArray(JSON.toJSONString(commitInfoList));
                    }
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
                throw new ApiRuntimeException(ex);
            } finally {
                if (wc != null) {
                    wc.close();
                }
            }
        }

        if (CollectionUtils.isNotEmpty(retList)) {
            // 根据 commit 对需求升序排序
            retList.sort((o1, o2) -> {
                JSONArray commitList1 = ((JSONObject) JSON.toJSON(o1)).getJSONArray("commitList");
                JSONArray commitList2 = ((JSONObject) JSON.toJSON(o2)).getJSONArray("commitList");
                if (CollectionUtils.isEmpty(commitList1) && CollectionUtils.isEmpty(commitList2)) {
                    return 0;
                } else if (CollectionUtils.isEmpty(commitList1)) {
                    return 1;
                } else if (CollectionUtils.isEmpty(commitList2)) {
                    return -1;
                } else {
                    Date date1 = commitList1.getJSONObject(commitList1.size() - 1).getDate("committerDate");
                    Date date2 = commitList2.getJSONObject(commitList2.size() - 1).getDate("committerDate");

                    if (date1 == null && date2 == null) {
                        return 0;
                    } else if (date1 == null) {
                        return 1;
                    } else if (date2 == null) {
                        return -1;
                    } else {
                        return Long.compare(date1.getTime(), date2.getTime());
                    }
                }
            });
        }

        return retList;
    }

    // TODO 根据需求号过滤 SVN commit
    public void searchSVNCommitByIssue(SVNWorkingCopy wc, String srcBranch, String targetBranch, String srcStartCommit, String srcEndCommit) {

    }

    public void groupByIssue() {

    }

}
