package com.techsure.autoexecrunner.codehub.api;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.techsure.autoexecrunner.codehub.dto.cache.Cache;
import com.techsure.autoexecrunner.codehub.dto.commit.CommitInfo;
import com.techsure.autoexecrunner.codehub.dto.diff.FileDiffInfo;
import com.techsure.autoexecrunner.codehub.git.GitWorkingCopy;
import com.techsure.autoexecrunner.codehub.svn.SVNWorkingCopy;
import com.techsure.autoexecrunner.codehub.utils.JSONUtils;
import com.techsure.autoexecrunner.codehub.utils.WorkingCopyUtils;
import com.techsure.autoexecrunner.common.config.Config;
import com.techsure.autoexecrunner.constvalue.ApiParamType;
import com.techsure.autoexecrunner.restful.annotation.Description;
import com.techsure.autoexecrunner.restful.annotation.Input;
import com.techsure.autoexecrunner.restful.annotation.Output;
import com.techsure.autoexecrunner.restful.annotation.Param;
import com.techsure.autoexecrunner.restful.core.privateapi.PrivateApiComponentBase;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


/**
 * @author zouye
 * @description diff 接口，可用于 MR 中的提交的差异比较，也可以直接比较两个 commit，它们之间的差异：<br/>
 * 1、MR 的 diff 使用缓存，两个 commit 直接比较不使用缓存。
 *  因为非 MR commit 做 diff 时，两个 commit 可能来自分支，也可能来自 tag，而缓存主要用来处理review页面性能，只和分支对应（需求型）。<br/>
 * 2、MR 的 diff 必须传分支，两个 commit 的比较不用传分支。<br/>
 * 3、MR 的 diff 如果不传 rightCommitId，可通过分支、srcStartCommit 等参数间接得到一个用于比较的 commit，两个 commit 的 diff 至少要提供 rightCommitId，否则报错。
 */
@Service
public class DiffApi extends PrivateApiComponentBase {
	Logger logger = LoggerFactory.getLogger(DiffApi.class);

	@Override
	public String getToken() {
		return "codehub/diff";
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
			@Param(name = "leftCommitId", type = ApiParamType.STRING, desc = ""),
			@Param(name = "rightCommitId", type = ApiParamType.STRING, desc = ""),
			@Param(name = "filePath", type = ApiParamType.STRING, desc = "文件路径"),
			@Param(name = "srcBranch", type = ApiParamType.STRING, desc = "源分支"),
			@Param(name = "targetBranch", type = ApiParamType.STRING, desc = "目标分支"),
			@Param(name = "srcStartCommit", type = ApiParamType.STRING, desc = "起始提交源"),
			@Param(name = "maxChangeCount", type = ApiParamType.INTEGER, desc = "最大变更数"),
			@Param(name = "maxSearchCount", type = ApiParamType.INTEGER, desc = "最大搜索数"),
			@Param(name = "mainBranch", type = ApiParamType.STRING, desc = "主分支"),
			@Param(name = "branchesPath", type = ApiParamType.STRING, desc = "分支路径"),
			@Param(name = "tagsPath", type = ApiParamType.STRING, desc = "标签路径"),
			@Param(name = "forceFlush", type = ApiParamType.BOOLEAN, desc = "强制刷新"),
			@Param(name = "repositoryServiceId", type = ApiParamType.LONG, desc = "仓库服务id",isRequired = true),
			@Param(name = "repositoryId", type = ApiParamType.LONG, desc = "仓库id",isRequired = true),
			@Param(name = "isMrDiff", type = ApiParamType.BOOLEAN, desc = "是否合并差异")
	})
	@Output({
	})
	@Description(desc = "查询合并差异接口")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		String repoType = JSONUtils.optString(jsonObj,"repoType", "").trim().toLowerCase();
		String url = JSONUtils.optString(jsonObj,"url", "").trim();
		String username = JSONUtils.optString(jsonObj,"username", "");
		String pwd = JSONUtils.optString(jsonObj,"password", "");
		String leftCommitId = JSONUtils.optString(jsonObj,"leftCommitId");
		String rightCommitId = JSONUtils.optString(jsonObj,"rightCommitId");
		String filePath = JSONUtils.optString(jsonObj,"filePath");
		String srcBranch = JSONUtils.optString(jsonObj,"srcBranch");
		String targetBranch = JSONUtils.optString(jsonObj,"targetBranch");
		String srcStartCommit = JSONUtils.optString(jsonObj,"srcStartCommit");
		int maxChangeCount = JSONUtils.optInt(jsonObj,"maxChangeCount", -1);
		int maxSearchCount = JSONUtils.optInt(jsonObj,"maxSearchCount", 300);
		boolean forceFlush = JSONUtils.optBoolean(jsonObj,"forceFlush", false);
		Long repositoryServiceId = jsonObj.getLong("repositoryServiceId");
		Long repositoryId = jsonObj.getLong("repositoryId");
		boolean isMrDiff = JSONUtils.optBoolean(jsonObj,"isMrDiff", false);

		String wcPath = WorkingCopyUtils.getWcPath(jsonObj);

		JSONObject jsonObject = new JSONObject();

		if (repoType.equals("svn")) {
			SVNWorkingCopy wc = null;
			if (isMrDiff && StringUtils.isBlank(srcBranch)) {
				throw new RuntimeException("请指定参数 'srcBranch'");
			}

			try {
				wc = new SVNWorkingCopy(wcPath, url, username, pwd, JSONUtils.optString(jsonObj,"mainBranch", ""), JSONUtils.optString(jsonObj,"branchesPath", ""), JSONUtils.optString(jsonObj,"tagsPath", ""));
				JSONArray fileDiffList = null;

				if (isMrDiff) {
					JSONArray commitList = new JSONArray();
					List<CommitInfo> newCommitList = null;
					Cache cache = new Cache(repositoryServiceId, repositoryId, srcBranch);

					newCommitList = getCommitInfoList(wc, cache, jsonObj);

					if (CollectionUtils.isNotEmpty(newCommitList)) {
						//JsonConfig config = new JsonConfig();
						//config.registerJsonBeanProcessor(CommitInfo.class, new MyJsonBeanProcessor());
						//commitList = JSONArray.fromObject(newCommitList, config);
						//这里不用自定义转了 fastjson会自动将Date转成时间戳
						commitList = JSONArray.parseArray(JSON.toJSONString(newCommitList));
					}

					if (StringUtils.isBlank(rightCommitId)) {
						if (StringUtils.isNotBlank(srcStartCommit)) { // 已合并
							rightCommitId = srcStartCommit;
						} else {
							if (CollectionUtils.isNotEmpty(commitList)) {
								// 点击变更tab, 显示的diff数据应该要跟需求有关联的最新commit
								rightCommitId = commitList.getJSONObject(0).getString("commitId");
							} else {
								throw new RuntimeException("已提供的参数不满足 diff plugin 的使用条件");
							}
						}
					}

					if (StringUtils.isBlank(leftCommitId)) {
						leftCommitId = wc.getParent(rightCommitId);
					}

					jsonObj.put("leftCommitId", leftCommitId);
					jsonObj.put("rightCommitId", rightCommitId);
					fileDiffList = getFileDiffInfoList(wc, cache, jsonObj);
					jsonObject.put("commitList", commitList);
				} else {
					if (StringUtils.isBlank(rightCommitId)) {
						throw new RuntimeException("请指定参数 'rightCommitId'");
					}

					if (StringUtils.isBlank(leftCommitId)) {
						leftCommitId = wc.getParent(rightCommitId);
					}

					List<FileDiffInfo> fileDiffInfos = wc.getDiffInfo(filePath, Long.parseLong(leftCommitId), Long.parseLong(rightCommitId), maxChangeCount);
					fileDiffList = JSONArray.parseArray(JSON.toJSONString(fileDiffInfos));
				}
				jsonObject.put("fileDiffList", fileDiffList);
				jsonObject.put("leftCommitId", leftCommitId);
				jsonObject.put("rightCommitId", rightCommitId);
			} catch (Exception ex) {
				logger.error(ex.getMessage(), ex);
				throw new RuntimeException(ex);
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

				JSONArray fileDiffInfos = null;
				String targetStartCommit = JSONUtils.optString(jsonObj,"targetStartCommit", "");
				List<String> commitIdList = null;
				List<String> mismatchCommitIdList = new ArrayList<>();
				List<CommitInfo> commitInfoList = null;

				if (isMrDiff) {
					if (StringUtils.isBlank(srcStartCommit) || StringUtils.isBlank(targetStartCommit)) { // no merged

						targetStartCommit = wc.resolveBranch(targetBranch);
						srcStartCommit = wc.resolveBranch(srcBranch);

						if (srcStartCommit == null) {
							throw new RuntimeException(String.format("分支'%s'不存在", srcBranch));
						}

						if (targetStartCommit == null) {
							throw new RuntimeException(String.format("分支'%s'不存在", targetBranch));
						}
					}

					commitIdList = wc.gitRevListCommand(String.format("%s..%s", targetStartCommit, srcStartCommit), "--max-count=" + maxSearchCount);

					if (StringUtils.isBlank(rightCommitId)) { // 没有指定 diff 的commit，则比较分支
						leftCommitId = targetStartCommit;
						rightCommitId = srcStartCommit;
					} else {
						if (StringUtils.isBlank(leftCommitId)) {
							leftCommitId = wc.getParent(rightCommitId);
						}
					}

					Cache cache = new Cache(repositoryServiceId, repositoryId, srcBranch);
					if (Config.CACHE_ENABLE && !forceFlush) {
						if (StringUtils.isBlank(filePath)) {
							fileDiffInfos = cache.readGitDiffFromCache(leftCommitId, rightCommitId);
						}

						Map<String, Object> cacheRetMap = cache.readGitCommitsFromCache(commitIdList);
						if (cacheRetMap != null) {
							JSONArray cacheCommitList = null;
							if (cacheRetMap.get("matchedCommitList") != null) {
								cacheCommitList = (JSONArray)cacheRetMap.get("matchedCommitList");
							}

							if (CollectionUtils.isNotEmpty(cacheCommitList)) {
								commitInfoList = cacheCommitList.toJavaList(CommitInfo.class);
							}

							if (cacheRetMap.get("mismatchCommitIdList") != null) {
								mismatchCommitIdList = (List<String>) cacheRetMap.get("mismatchCommitIdList");
							}
						}
					} else {
						mismatchCommitIdList = commitIdList;
					}

					if (CollectionUtils.isEmpty(fileDiffInfos)) { // no cache, disable cache, or file diff
						List<FileDiffInfo> diffs = wc.getDiffInfo(rightCommitId, leftCommitId, filePath, maxChangeCount);
						if (CollectionUtils.isNotEmpty(diffs)) {
							fileDiffInfos = JSONArray.parseArray(JSON.toJSONString(diffs));

							if (Config.CACHE_ENABLE && StringUtils.isBlank(filePath)) {
								cache.writeGitDiffToCache(leftCommitId, rightCommitId, fileDiffInfos);
							}
						}
					}

					if (CollectionUtils.isNotEmpty(mismatchCommitIdList)) {
						List<CommitInfo> noCacheList = new ArrayList<>();
						for (String commitId: mismatchCommitIdList) {
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
				} else {
					if (StringUtils.isBlank(rightCommitId)) {
						throw new RuntimeException("请指定参数 'rightCommitId'");
					}

					if (StringUtils.isBlank(leftCommitId)) {
						leftCommitId = wc.getParent(rightCommitId);
					}

					List<FileDiffInfo> diffs = wc.getDiffInfo(rightCommitId, leftCommitId, filePath, maxChangeCount);
					fileDiffInfos = JSONArray.parseArray(JSON.toJSONString(diffs));
				}

				jsonObject.put("commitList", commitInfoList);
				jsonObject.put("fileDiffList", fileDiffInfos);
				jsonObject.put("leftCommitId", leftCommitId);
				jsonObject.put("rightCommitId", rightCommitId);
			} catch(Exception ex) {
				logger.error(ex.getMessage(), ex);
				throw new RuntimeException(ex);
			} finally {
				if (wc != null) {
					wc.close();
				}
			}

		}

		return jsonObject;
	}



	/** 获取 commit 列表，如果指定了需求号列表，则按照需求号过滤commit */
	private List<CommitInfo> getCommitInfoList(SVNWorkingCopy wc, Cache cache, JSONObject jsonObj) throws Exception {
		JSONArray issueList = jsonObj.getJSONArray("issueList");
		String srcBranch = JSONUtils.optString(jsonObj,"srcBranch");
		String srcStartCommit = JSONUtils.optString(jsonObj,"srcStartCommit");
		String srcEndCommit = JSONUtils.optString(jsonObj,"srcEndCommit");
		int maxSearchCount = JSONUtils.optInt(jsonObj,"maxSearchCount", 300);
		boolean forceFlush = JSONUtils.optBoolean(jsonObj,"forceFlush", false);
		boolean filterByIssue = CollectionUtils.isNotEmpty(issueList);

		List<CommitInfo> commitInfoList = new ArrayList<>();
		if (Config.CACHE_ENABLE) {
			// --bug=1008004 --user=邹叶 【代码中心】码线上有提交，创建MR时却提示需求无效 https://www.tapd.cn/54247054/s/1154320
			// srcEndCommit 为空，走强制刷新：从远程取提交并更新本地缓存。
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
								|| cacheList.size() < maxSearchCount) {	// commit数量不满足maxSearchCount,必须needMore返回maxSearchCount个commit
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
							cache.writeCommitsToCache(JSONArray.parseArray(JSONArray.toJSONString(commitInfoList)));
						} else {
							// 防止刚好出现srcEndCommit commit重复
							int size = commitInfoList.size();
							if (CollectionUtils.isNotEmpty(cacheList)
									&& commitInfoList.get(size - 1).getCommitId().equals(cacheList.getJSONObject(0).getString("commitId"))) {
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

				// 【缓存读写异常, 缓存还是存在重复的commit的问题】https://www.tapd.cn/54247054/bugtrace/bugs/view?bug_id=1154247054001008118
				// bug , 没有更新head
				long cacheHead = NumberUtils.toLong(cache.getHead(), 0);
				if (Long.parseLong(commitInfoList.get(0).getCommitId()) > cacheHead) {
					cache.setHead(commitInfoList.get(0).getCommitId());
				}
			}
		} else {
			commitInfoList = wc.getBranchCommitListByCommitIdRange(srcBranch, srcStartCommit, srcEndCommit, maxSearchCount);
		}

		if (CollectionUtils.isNotEmpty(commitInfoList)) {
			Iterator<CommitInfo> it = commitInfoList.iterator();
			while(it.hasNext()) {
				CommitInfo commitInfo = it.next();

				// commit可能是包含多个需求的
				Set<String> issueSet = WorkingCopyUtils.parseCommentIssueNo(commitInfo.getMessage());
				if (CollectionUtils.isNotEmpty(issueSet)) {
					if (filterByIssue) {
						boolean noneMatch = true;
						for (String s : issueSet) {
							if (issueList.contains(s)) {
								noneMatch = false;
								break;
							}
						}
						if (noneMatch) {
							// 多个需求, 没有一个需求匹配到就remove
							it.remove();
							continue;
						}
					}

					// ?TODO 全部存
					commitInfo.setIssueNo(String.join(",", issueSet));
				} else if (filterByIssue) {
					it.remove();
					// commit没有关联需求, 并且filterByIssue为true, 说明是不想要的
				}

			}
		}

		return commitInfoList;
	}

	private JSONArray getFileDiffInfoList(SVNWorkingCopy wc, Cache cache, JSONObject jsonObj) throws Exception {
		String leftCommitId = JSONUtils.optString(jsonObj,"leftCommitId");
		String rightCommitId = JSONUtils.optString(jsonObj,"rightCommitId");
		String filePath = JSONUtils.optString(jsonObj,"filePath");
		int maxChangeCount = JSONUtils.optInt(jsonObj,"maxChangeCount", -1);
		boolean forceFlush = JSONUtils.optBoolean(jsonObj,"forceFlush", false);

		List<FileDiffInfo> fileDiffInfos = null;
		boolean needCache = false;

		// 指定了文件路径，说明是被折叠的，不走缓存
		if (StringUtils.isBlank(filePath) && Config.CACHE_ENABLE && !forceFlush) {
			needCache = true;
		}

		JSONArray retList = null;
		if (needCache) {
			retList = cache.getSVNDiffFromCache(leftCommitId, rightCommitId);
		}

		if (CollectionUtils.isEmpty(retList)) {
			fileDiffInfos = wc.getDiffInfo(filePath, Long.parseLong(leftCommitId), Long.parseLong(rightCommitId), maxChangeCount);

			retList = JSONArray.parseArray(JSON.toJSONString(fileDiffInfos));
			// 没有缓存，添加
			if (StringUtils.isBlank(filePath) && Config.CACHE_ENABLE) {		// fixed: 设置了forceFlush之后需要更新缓存, 但是原来的needCache还是false
				cache.writeSVNDiffToCache(leftCommitId, rightCommitId, retList);
			}
		}

		return retList;
	}


/*	private class MyJsonBeanProcessor implements JsonBeanProcessor {
		@Override
		public JSONObject processBean(Object arg0, JsonConfig arg1) {
			CommitInfo commitInfo = (CommitInfo)arg0;
			JSONObject json = new JSONObject();
			
			json.put("committerDate", commitInfo.getCommitterDateTimestamp());
			json.put("authorDate", commitInfo.getAuthorDateTimestamp());
			json.put("commitId", commitInfo.getCommitId());
			json.put("message", commitInfo.getMessage());
			json.put("author", commitInfo.getAuthor());
			json.put("committer", commitInfo.getCommitter());
			json.put("authorEmail", commitInfo.getAuthorEmail());
			json.put("committerEmail", commitInfo.getCommitterEmail());
			json.put("issueNo", commitInfo.getIssueNo());
			
			return json;
		}
	}*/



/*	@Override
	public JSONArray help() {
		JSONArray jsonArray = new JSONArray();

		ApiHelpUtils.addRepoAuthJsonObj(jsonArray);

		JSONObject jsonObj = new JSONObject();
		jsonObj.put("name", "commintList");
		jsonObj.put("type", "JSONArray");
		jsonObj.put("desc", "commit列表");
		jsonArray.add(jsonObj);

		ApiHelpUtils.addSVNWorkingCopyPathJsonObj(jsonArray);

		return jsonArray;
	}*/

}
