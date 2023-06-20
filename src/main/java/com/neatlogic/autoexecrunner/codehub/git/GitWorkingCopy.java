/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.neatlogic.autoexecrunner.codehub.git;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.codehub.constvalue.MergeFileStatus;
import com.neatlogic.autoexecrunner.codehub.dto.commit.CommitInfo;
import com.neatlogic.autoexecrunner.codehub.dto.diff.FileDiffInfo;
import com.neatlogic.autoexecrunner.codehub.dto.diff.FileInfo;
import com.neatlogic.autoexecrunner.codehub.dto.merge.MergeFileEntry;
import com.neatlogic.autoexecrunner.codehub.dto.merge.MergeResultInfo;
import com.neatlogic.autoexecrunner.codehub.exception.GitOpsException;
import com.neatlogic.autoexecrunner.codehub.exception.LockFailedException;
import com.neatlogic.autoexecrunner.codehub.utils.RegexUtils;
import com.neatlogic.autoexecrunner.codehub.utils.WorkingCopyUtils;
import com.neatlogic.autoexecrunner.util.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.IndexDiff.StageState;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.SubmoduleConfig.FetchRecurseSubmodulesMode;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.*;


public class GitWorkingCopy {
	private String repoLocalDir = null;
	private String remoteUrl = null;
	private String username = "";
	private String password = "";
	private CredentialsProvider credsProvider = null;

	private Git wcGit = null;

	
	private File lockFile = null;
	private RandomAccessFile randomAccessFile = null;
	private FileChannel lockChannel = null;
	private FileLock buildLock = null;
	
	/**
	 * 构造函数，如果本地目录不存在，则使用远程URL创建一个新的本地Working Copy
	 *
	 * @param repoLocalDir 本地Working Copy存放的目录
	 * @param remoteUrl    远程仓库的URL
	 * @param username     访问远程Git仓库的用户名
	 * @param password     访问远程仓库的密码
	 * @throws GitOpsException
	 */
	public GitWorkingCopy(String repoLocalDir, String remoteUrl, String username, String password) throws GitOpsException {
		this.repoLocalDir = repoLocalDir;
		this.username = username;
		this.password = password;
		this.remoteUrl = remoteUrl;

		credsProvider = new GitCredentialsProvider(this.username, this.password);

		File localPath = new File(repoLocalDir);
		if (!localPath.exists()) {
			cloneRepo();
		}

		try {
			wcGit = Git.open(localPath);
		} catch (IOException e) {
			try {
				FileUtil.deleteDirectoryOrFile(repoLocalDir);
				cloneRepo();
				wcGit = Git.open(localPath);
			} catch (GitOpsException e1) {
				throw e1;
			} catch (IOException e2) {
				throw new GitOpsException("Open git dir " + repoLocalDir + " failed(" + e.getMessage() + ").", e);
			}
		}
	}

	/**
	 * 克隆远程库到本地目录
	 *
	 * @throws GitOpsException
	 */
	public void cloneRepo() throws GitOpsException {
		// prepare a new folder for the cloned repository
		File localPath = new File(repoLocalDir);
		if (localPath.exists()) {
			throw new GitOpsException("Directory:" + repoLocalDir + " already exists!");
		}

		// then clone
		try {
			Git.cloneRepository().setURI(remoteUrl).setDirectory(localPath).setCredentialsProvider(credsProvider).setCloneSubmodules(true).setCloneAllBranches(true).call();
		} catch (GitAPIException e) {
			throw new GitOpsException("Clone " + remoteUrl + " to " + repoLocalDir + " failed, " + e.getMessage(), e);
		}
	}

	/**
	 * 关闭本地Git仓库实例，如果不关闭会导致资源泄漏
	 */
	public void close() {
		
		if (wcGit != null) {
			wcGit.close();
		}
	}

	/**
	 * 本地Git仓库实例是否已经关闭
	 *
	 * @return true：已关闭，false：未关闭
	 */
	public boolean isClose() {
		if (wcGit == null) {
			return true;
		}
		return false;
	}

	/**
	 * 获取commit的简要信息，比如提交人、提交时间、提交信息等。不包含 diff 内容。
	 *
	 * @param commitId commit id
	 * @return
	 * @throws GitOpsException
	 */
	public CommitInfo getCommit(String commitId) throws GitOpsException {
		GitRepository gitRepo = null;

		try {
			gitRepo = new GitRepository(repoLocalDir);
			CommitInfo commitInfo = gitRepo.getCommitDetail(commitId, true);

			return commitInfo;
		} finally {
			if (gitRepo != null) {
				gitRepo.close();
			}
		}
	}

	/**
	 * 获取commit的明细信息，包括commit修改的文件，插入删除的行数，修改的上下文信息
	 *
	 * @param commitId commit id
	 * @return
	 * @throws GitOpsException
	 */
	public CommitInfo getCommitDetail(String commitId) throws GitOpsException {
		GitRepository gitRepo = null;

		try {
			gitRepo = new GitRepository(repoLocalDir);
			CommitInfo commitInfo = gitRepo.getCommitDetail(commitId, false);

			return commitInfo;
		} finally {
			if (gitRepo != null) {
				gitRepo.close();
			}
		}
	}

	/**
	 * 按照时间倒序获取从startCommitId开始的，最近不超过maxCount，不早于startTime的提交日志
	 *
	 * @param startCommitId 获取日志的启始commit id(不包含此commit)， null代表从头部开始
	 * @param maxCount   最多获取的commit数，0:不限制，否则最多获取maxCount条记录
	 * @param startTime    最早的commit的时间（epochTime），0:不限制，否则获取的commit的时间要大于等于startTime
	 * @param logOnly  true:只获取日志不分析commit修改的文件
	 * @throws GitOpsException
	 */
	public List<CommitInfo> getCommits(String startCommitId, int maxCount, long startTime, boolean logOnly) throws GitOpsException {
		GitRepository gitRepo = null;

		try {
			gitRepo = new GitRepository(repoLocalDir);
			List<CommitInfo> commitInfoList = gitRepo.getCommits(startCommitId, maxCount, startTime, logOnly);

			return commitInfoList;
		} finally {
			if (gitRepo != null) {
				gitRepo.close();
			}
		}
	}

	/**
	 * 获取从某个分支从startCommitId开始，最近不超过maxCount，不早于startTime的commit log，注意：对于那些先提交后push的commit会获取不到
	 *
	 * @param branch  分支名称
	 * @param startCommitId 获取日志的启始commit id(不包含此commit)， null代表从头部开始
	 * @param maxCount 最多获取的commit数，0:不限制，否则最多获取maxCount条记录
	 * @param startTime  最早的commit的时间（epochTime），0:不限制，否则获取的commit的时间要大于等于startTime
	 * @param logOnly true:只获取日志不分析commit修改的文件
	 * @throws GitOpsException
	 */
	public List<CommitInfo> getCommitsForBranch(String branch, String startCommitId, int maxCount, long startTime, boolean logOnly) throws GitOpsException {
		GitRepository gitRepo = null;

		try {
			gitRepo = new GitRepository(repoLocalDir);

			List<CommitInfo> commitInfoList = gitRepo.getCommitsForBranch(branch, startCommitId, maxCount, startTime, logOnly);

			return commitInfoList;
		} finally {
			if (gitRepo != null) {
				gitRepo.close();
			}
		}
	}

	/**
	 * 获取某个tag从startCommitId开始，最近不超过maxCount，不早于startTime的commit log
	 *
	 * @param tag     标签名称
	 * @param startCommitId 获取日志的启始commit id(不包含此commit)， null代表从头部开始
	 * @param maxCount   最多获取的commit数，0:不限制，否则最多获取maxCount条记录
	 * @param startTime    最早的commit的时间（epochTime），0:不限制，否则获取的commit的时间要大于等于startTime
	 * @param logOnly true:只获取日志不分析commit修改的文件
	 * @throws GitOpsException
	 */
	public List<CommitInfo> getCommitsForTag(String tag, String startCommitId, int maxCount, long startTime, boolean logOnly) throws GitOpsException {
		GitRepository gitRepo = null;

		try {
			gitRepo = new GitRepository(repoLocalDir);

			List<CommitInfo> commitInfoList = gitRepo.getCommitsForTag(tag, startCommitId, maxCount, startTime, logOnly);

			return commitInfoList;
		} finally {
			if (gitRepo != null) {
				gitRepo.close();
			}
		}
	}

	/**
	 * 获取branch从fromBranch分支出来的commit，或者branch并入fromBranch的commit点，从startCommitId开始，最近不超过maxCount，不早于startTime的commit log
	 *
	 * @param branch 源分支名，准备merge进入dstBranch的分支名
	 * @param fromBranch 目标分支名，准备接收srcBranch并入的分支名
	 * @param startCommitId 获取日志的启始commit id(不包含此commit)， null代表从头部开始
	 * @param maxCount   最多获取的commit数，0:不限制，否则最多获取maxCount条记录
	 * @param startTime    最早的commit的时间（epochTime），0:不限制，否则获取的commit的时间要大于等于startTime
	 * @param logOnly   true:只获取日志不分析commit修改的文件
	 * @return
	 * @throws GitOpsException
	 */
	public List<CommitInfo> getCommitsForBranch(String branch, String fromBranch, String startCommitId, int maxCount, long startTime, boolean logOnly) throws GitOpsException {
		GitRepository gitRepo = null;

		try {
			gitRepo = new GitRepository(repoLocalDir);
			List<CommitInfo> commitInfoList = gitRepo.getCommitsForBranch(branch, fromBranch, startCommitId, maxCount, startTime, logOnly);

			return commitInfoList;
		} finally {
			if (gitRepo != null) {
				gitRepo.close();
			}
		}
	}
	
	public List<CommitInfo> getCommitsForMerge(String srcBranch, String dstBranch, int maxCount) throws GitOpsException {
		GitRepository gitRepo = new GitRepository(repoLocalDir);
		try {
			return gitRepo.getCommitsForMerge(srcBranch, dstBranch, maxCount, true);
		} finally {
			gitRepo.close();
		}
	}
	
	public List<CommitInfo> getCommitsForMergeByCommitRange(String srcStartCommit, String dstStartCommit, int maxCount) throws GitOpsException {
		String[] command = new String[] {"git", "rev-list", String.format("%s..%s", dstStartCommit, srcStartCommit), "--max-count=" + maxCount};
		JSONObject ret = WorkingCopyUtils.executeCommand(repoLocalDir, command);
		List<CommitInfo> retList = new ArrayList<>();
		
		if (StringUtils.equals(ret.getString("status"), "success") && StringUtils.isNotBlank(ret.getString("result"))) {
			String[] commits = ret.getString("result").split("(\n|\r\n)");
			for (String commit: commits) {
				retList.add(this.getCommit(commit));
			}
		}
		//GitRepository gitRepo = new GitRepository(repoLocalDir);
		//return gitRepo.getCommitsForMerge(srcStartCommit, srcStartCommit, maxCount, false);
		
		return retList;
	}
	
	
/*    public List<CommitInfo> getCommitsForMerge(String srcBranch, String dstBranch, int maxCount) throws GitOpsException {
    	GitRepository gitRepo = null;
    	try {
    		gitRepo = new GitRepository(repoLocalDir);
	    	Repository repo = wcGit.getRepository();
	    	ObjectId srcCommitId = repo.resolve("refs/remotes/origin/" + srcBranch);
	    	ObjectId dstCommitId = repo.resolve("refs/remotes/origin/" + dstBranch);
	    	
	    	List<RevCommit> mergeBaseList = gitRepo.getMergeBaseList(srcCommitId.getName(), dstCommitId.getName());
	    	List<CommitInfo> commitInfoList = new LinkedList<>();
	    	gitRepo.getCommitsForMerge(commitInfoList, srcCommitId.getName(), mergeBaseList, maxCount, true);
	    	
	    	return commitInfoList;
    	} catch (Exception e) {
    		throw new GitOpsException("Get commits failed(" + e.getMessage() + ").", e);
    	}
    }*/

	/**
	 * 重置当前Working Copy，做所有的merge或者文件修改操作前都需要执行reset，规避之前的失败操作的影响
	 *
	 * @throws GitOpsException
	 */
	public void reset() throws GitOpsException {
		// clear the merge state
		try {
			Repository repo = wcGit.getRepository();
			repo.writeMergeCommitMsg(null);
			repo.writeMergeHeads(null);
			repo.writeCherryPickHead(null);
			repo.writeCommitEditMsg(null);
			repo.writeRevertHead(null);

			wcGit.reset().setMode(ResetType.HARD).call();

			wcGit.clean().call();
		} catch (GitAPIException | IOException e) {
			throw new GitOpsException("Reset working copy " + repoLocalDir + " failed(" + e.getMessage() + ").", e);
		}
	}

	/**
	 * 对本地Working Copy执行fetch更新，每次操作前都需要执行，让本地Working Copy跟远程仓库同步
	 *
	 * @throws GitOpsException
	 */
	public void update() throws GitOpsException {
		try {
			wcGit.fetch().setCredentialsProvider(credsProvider).setRecurseSubmodules(FetchRecurseSubmodulesMode.YES).setRemoveDeletedRefs(true).setForceUpdate(true).setCheckFetchedObjects(true).call();
		} catch (GitAPIException e) {
			throw new GitOpsException("Fetch for " + repoLocalDir + " failed(" + e.getMessage() + ").", e);
		}
	}

	/**
	 * 对本地Working Copy重置并执行fetch更新，每次操作前都需要执行，让本地Working Copy跟远程仓库同步
	 *
	 * @throws GitOpsException
	 */
	public void resetAndUpdate() throws GitOpsException {
		this.reset();
		this.update();
	}

	/**
	 * 检出某个分支，需要在执行reset和fetch update后才检出，才能保证当前检出的分支跟远程仓库是一致的
	 *
	 * @param branch 检出的分支名称
	 * @param referRemote 是否关联远程分支<br>
	 * true: 如果 working copy 不存在此分支，远程存在这个分支，则建立两个分支的对应关系，checkout 后执行一次 pull 操作<br>
	 * false: 纯 working copy 操作，不关联远程
	 * @throws GitOpsException
	 */
	public void checkout(String branch, boolean referRemote) throws GitOpsException {
		try {
			CheckoutCommand checkoutCmd = wcGit.checkout().setName(branch);

			if (referRemote) {
				Ref remoteRef = wcGit.getRepository().exactRef("refs/remotes/origin/" + branch);
				Ref localRef = wcGit.getRepository().exactRef("refs/heads/" + branch);
				if (localRef == null && remoteRef != null) {
					checkoutCmd.setCreateBranch(true).setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).setStartPoint("origin/" + branch);
				}
			}

			checkoutCmd.call();
			
			if (referRemote) {
				wcGit.pull().setCredentialsProvider(credsProvider).call();
			}
		} catch (GitAPIException | IOException e) {
			throw new GitOpsException("Checkout " + branch + " failed(" + e.getMessage() + ").", e);
		}
	}

	/**
	 * fetch和update merge某个分支，一般不需要使用
	 *
	 * @throws GitOpsException
	 */
	public void pull() throws GitOpsException {
		try {
			wcGit.pull().setCredentialsProvider(credsProvider).call();
		} catch (GitAPIException e) {
			throw new GitOpsException("Push " + repoLocalDir + " failed, " + e.getMessage(), e);
		}
	}

	/**
	 * 把当前修改commit到本地仓库
	 *
	 * @param commiterName  提交人名称，使用调用实名用户的用户ID
	 * @param commiterEmail 提交人邮件地址，使用实名用户的email
	 * @param comment       提交message
	 * @return 提交的commit的id
	 * @throws GitOpsException
	 */
	public String commit(String commiterName, String commiterEmail, String comment) throws GitOpsException {
		try {
			CommitCommand commitCmd = wcGit.commit();
			commitCmd.setCommitter(commiterName, commiterEmail).setMessage(comment);
			RevCommit commit = commitCmd.call();

			return commit.getName();
		} catch (GitAPIException e) {
			throw new GitOpsException("Commit " + repoLocalDir + " failed, " + e.getMessage(), e);
		}
	}

	/**
	 * 把当前分支push到远程仓库，在执行完merge后需要执行此操作
	 *
	 * @throws GitOpsException
	 */
	public void push() throws GitOpsException {
		try {
			wcGit.push().setCredentialsProvider(credsProvider).call();
		} catch (GitAPIException e) {
			throw new GitOpsException("Push " + repoLocalDir + " failed, " + e.getMessage(), e);
		}
	}

	/**
	 * 获取默认分支
	 * @return 默认分支名
	 * @throws GitOpsException
	 */
	public String getRemoteDefaultBranch() throws GitOpsException {
		try {
			String defaultBranch = null;

			Ref head = Git.lsRemoteRepository().setRemote(remoteUrl).setCredentialsProvider(credsProvider).callAsMap().get(Constants.HEAD);

			if (head != null) {
				if (head.isSymbolic()) {
					defaultBranch = head.getTarget().getName();
				} else {
					ObjectId objectId = head.getObjectId();
					if (objectId != null) {
						List<Ref> refs = wcGit.getRepository().getRefDatabase().getRefsByPrefix(Constants.R_REMOTES);
						for (Ref ref : refs) {
							if (ref.getObjectId().equals(objectId)) {
								defaultBranch = ref.getName();
								break;
							}
						}
					}
				}
			}
			if (StringUtils.isNotEmpty(defaultBranch)) {
				defaultBranch = StringUtils.removeStart(defaultBranch, "refs/remotes/origin/");
				defaultBranch = StringUtils.removeStart(defaultBranch, "refs/heads/");
			}
			return defaultBranch;
		} catch (GitAPIException | IOException e) {
			throw new GitOpsException("Get default branch failed, " + e.getMessage(), e);
		}
	}

	/**
	 * 获取所有的远程的分支列表
	 *
	 * @return
	 * @throws GitOpsException
	 */
	public List<String> getRemoteBranchList() throws GitOpsException {
		return getRemoteBranchList(null);
	}
	/**
	 * 获取所有的远程的分支列表
	 *
	 * @param matcher 正则表达式，仅返回匹配正则表达式的branch
	 * @return
	 * @throws GitOpsException
	 */
	public List<String> getRemoteBranchList(String matcher) throws GitOpsException {
		try {
			List<String> branchNameList = new ArrayList<String>();

			List<Ref> branchList = wcGit.branchList().setListMode(ListMode.REMOTE).call();
			for (Ref ref : branchList) {
				String branchName = ref.getName();

				// 注意: 此处branchName有时候会返回 HEAD, 或者是refs/remotes/origin/xxxx 等 所以要区分出来, 去掉前缀
				if (branchName.startsWith("refs/remotes/origin/")) {
					branchName = branchName.substring(20);
				}

				if(matcher != null) {
					if(RegexUtils.wildcardMatch(matcher, branchName)) {
						branchNameList.add(branchName);
					}
				} else {
					branchNameList.add(branchName);
				}
			}

			return branchNameList;
		} catch (GitAPIException e) {
			throw new GitOpsException("Get branch list failed, " + e.getMessage(), e);
		}
	}

	/**
	 * 获取branch指向的commitId
	 * @param branchName 标签名
	 * @return
	 * @throws GitOpsException
	 */
	public String resolveBranch(String branchName) throws GitOpsException {
		try {
			Ref ref = wcGit.getRepository().exactRef("refs/remotes/origin/" + branchName);

			if (ref != null) {
				return ref.getObjectId().getName();
			} else {
				
				// 尝试解析本地分支
				if (branchName.startsWith("revert-")) {
					ref = wcGit.getRepository().exactRef("refs/heads/" + branchName);
					if (ref != null) {
						return ref.getObjectId().getName();
					}
				}
				return null;
			}
		} catch (IOException e) {
			throw new GitOpsException("Resolve branch failed, " + e.getMessage(), e);
		}
	}

	/**
	 * 远程仓库是否有分支
	 *
	 * @param branchName 分支名称
	 * @return
	 * @throws GitOpsException
	 */
	public boolean hasBranch(String branchName) throws GitOpsException {
		try {
			Ref ref = wcGit.getRepository().exactRef("refs/remotes/origin/" + branchName);

			if (ref != null) {
				return true;
			} else {
				return false;
			}
		} catch (IOException e) {
			throw new GitOpsException("Check branch failed, " + e.getMessage(), e);
		}
	}
	
	/**
	 * 本地仓库是否有分支
	 *
	 * @param branchName 分支名称
	 * @return
	 * @throws GitOpsException
	 */
	public boolean hasLocalBranch(String branchName) throws GitOpsException {
		try {
			Ref ref = wcGit.getRepository().exactRef("refs/heads/" + branchName);
			
			return ref != null;
		} catch (IOException e) {
			throw new GitOpsException("Check branch failed, " + e.getMessage(), e);
		}
	}

	/**
	 * 远程分支的数量
	 *
	 * @return 分支数量
	 * @throws GitOpsException
	 */
	public int getBranchCount() throws GitOpsException {
		try {
			List<Ref> branchList = wcGit.branchList().setListMode(ListMode.REMOTE).call();

			return branchList.size();
		} catch (GitAPIException e) {
			throw new GitOpsException("Get branchs count failed, " + e.getMessage(), e);
		}
	}

	/**
	 * 创建远程分支，如果分支已经存在，则啥也不做
	 *
	 * @param branchName      新分支名称
	 * @param startBranchName 以哪个分支为起始创建新分支
	 * @throws GitOpsException
	 */
	public void createBranch(String branchName, String startBranchName) throws GitOpsException {
		createLocalBranch(branchName, startBranchName);

		try {
			wcGit.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password)).setRemote("origin").setRefSpecs(new RefSpec(branchName + ":" + branchName)).call();
		} catch (GitAPIException e) {
			throw new GitOpsException("Create branch " + branchName + " from " + startBranchName + " failed, " + e.getMessage(), e);
		}
	}
	
	/**
	 * 在 workingcopy 创建新分支，如果分支已经存在，则啥也不做
	 * @param branchName 新分支名称
	 * @param startBranchName 以哪个分支为起始创建新分支
	 * @throws GitOpsException      
	 */
	public void createLocalBranch(String branchName, String startBranchName) throws GitOpsException {
		try {
			wcGit.branchCreate().setName(branchName).setStartPoint("refs/remotes/origin/" + startBranchName).call();
		} catch (RefAlreadyExistsException e) {
			// pass
		} catch (GitAPIException e) {
			throw new GitOpsException("Create branch " + branchName + " from " + startBranchName + " failed, " + e.getMessage(), e);
		}
	}

	/**
	 * 删除远程分支
	 *
	 * @param branchName 分支名称
	 * @throws GitOpsException
	 */
	public void deleteBranch(String branchName) throws GitOpsException {
		deleteLocalBranch(branchName);

		try {
			Iterable<PushResult> origPushResults = wcGit.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password)).setRemote("origin").setRefSpecs(new RefSpec(":refs/heads/" + branchName)).call();
			for (PushResult pushResult : origPushResults) {
				RemoteRefUpdate remoteUpdate = pushResult.getRemoteUpdate("refs/heads/" + branchName);
				if (!RemoteRefUpdate.Status.OK.equals(remoteUpdate.getStatus())) {
					throw new GitOpsException("Delete branch " + branchName + " failed, message: " + pushResult.getMessages());
				}
			}
		} catch (GitAPIException e) {
			throw new GitOpsException("Delete branch " + branchName + " failed, " + e.getMessage(), e);
		}
	}
	
	/**
	 * 删除 working copy 中的分支，不会删除此分支对应的远程仓库分支
	 *
	 * @param branchName 分支名称
	 * @throws GitOpsException
	 */
	public void deleteLocalBranch(String branchName) throws GitOpsException {
		try {
			wcGit.branchDelete().setBranchNames(branchName).setForce(true).call();
		} catch (GitAPIException e) {
			throw new GitOpsException("Delete branch " + branchName + " failed, " + e.getMessage(), e);
		}
	}

	/**
	 * 获取远程的标签列表
	 *
	 * @return
	 * @throws GitOpsException
	 */
	public List<String> getRemoteTagList() throws GitOpsException {
		return getRemoteTagList(null);
	}

	/**
	 * 获取远程的标签列表
	 * @param matcher 正则表达式，仅返回匹配正则表达式的tag
	 * @return
	 * @throws GitOpsException
	 */
	public List<String> getRemoteTagList(String matcher) throws GitOpsException {
        try {
            List<String> tagNameList = new ArrayList<String>();

            Collection<Ref> refList = Git.lsRemoteRepository().setCredentialsProvider(credsProvider).setRemote(remoteUrl).call();
            for (Ref ref : refList) {
                String refName = ref.getName();
                if (refName.startsWith("refs/tags/")) {
                    String tagName = refName.replace("refs/tags/", "");
                    if(matcher != null) {
                        if(RegexUtils.wildcardMatch(matcher, tagName)) {
                            tagNameList.add(tagName);
                        }
                    } else {
                        tagNameList.add(tagName);
                    }
                }
            }

            return tagNameList;
        } catch (GitAPIException e) {
            throw new GitOpsException("Get remote tag list failed, " + e.getMessage(), e);
        }
	}

	/**
	 * 获取tag指向的commitId
	 * @param tagName 标签名
	 * @return
	 * @throws GitOpsException
	 */
	public String resolveTag(String tagName) throws GitOpsException {
		try {
			Ref ref = wcGit.getRepository().exactRef("refs/tags/" + tagName);

			if (ref != null) {
				return ref.getObjectId().getName();
			} else {
				return null;
			}
		} catch (IOException e) {
			throw new GitOpsException("Resolve tag failed, " + e.getMessage(), e);
		}
	}

	/**
	 * 是否存在标签
	 *
	 * @param tagName 标签名称
	 * @return
	 * @throws GitOpsException
	 */
	public boolean hasTag(String tagName) throws GitOpsException {
		try {
			Ref ref = wcGit.getRepository().exactRef("refs/tags/" + tagName);

			if (ref != null) {
				return true;
			} else {
				return false;
			}
		} catch (IOException e) {
			throw new GitOpsException("Check tag failed, " + e.getMessage(), e);
		}
	}

	/**
	 * 获取标签的数量
	 *
	 * @return
	 * @throws GitOpsException
	 */
	public int getTagCount() throws GitOpsException {
		try {
			List<Ref> tagList = wcGit.tagList().call();

			return tagList.size();
		} catch (GitAPIException e) {
			throw new GitOpsException("Get tags count failed, " + e.getMessage(), e);
		}
	}

	/**
	 * 创建标签
	 *
	 * @param tagName         标签名称
	 * @param startBranchName 创建标签的对应的分支名
	 * @throws GitOpsException
	 */
	public void createTag(String tagName, String startBranchName) throws GitOpsException, GitAPIException {
		RevWalk revWalk = null;
		try {
			Repository repo = wcGit.getRepository();
			ObjectId commitId = repo.resolve("refs/remotes/origin/" + startBranchName);
			revWalk = new RevWalk(repo);
			RevCommit commit = revWalk.parseCommit(commitId);

			wcGit.tag().setName(tagName).setObjectId(commit).call();
		} catch (RefAlreadyExistsException e) {
			// pass
		} catch (GitAPIException | RevisionSyntaxException | IOException e) {
			throw new GitOpsException("Create tag " + tagName + " from branch " + startBranchName + " failed, " + e.getMessage(), e);
		} finally {
			if (revWalk != null) {
				revWalk.close();
			}
		}

		try {
			wcGit.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password)).setPushTags().call();
		} catch (GitAPIException e) {
			//回滚操作
			wcGit.tagDelete().setTags("refs/tags/" + tagName).call();
			throw new GitOpsException("Create tag " + tagName + " from branch " + startBranchName + " failed, " + e.getMessage(), e);
		}
	}

	/**
	 * 删除标签
	 *
	 * @param tagName
	 * @throws GitOpsException, GitAPIException
	 */
	public void deleteTag(String tagName) throws GitOpsException, GitAPIException {
		RevObject revCommit = null;
		try {
			Ref ref = wcGit.getRepository().exactRef("refs/tags/" + tagName);
			revCommit = wcGit.getRepository().parseCommit(ref.getObjectId());
			wcGit.tagDelete().setTags("refs/tags/" + tagName).call();
		} catch (GitAPIException | IOException e) {
			throw new GitOpsException("Delete tag " + tagName + " failed, " + e.getMessage(), e);
		}

		try {
			Iterable<PushResult> pushResultIterable = wcGit.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password)).setRemote("origin").setRefSpecs(new RefSpec(":refs/tags/" + tagName)).call();
			handlePushResultIterable(pushResultIterable);
		} catch (Exception e) {
			//回滚
			wcGit.tag().setName(tagName).setObjectId(revCommit).call();
			throw new GitOpsException("Delete tag " + tagName + " failed, " + e.getMessage(), e);
		}
	}

    /**
	 * 处理PushResult
	 * @param pushResultIterable
     * @throws GitOpsException
	 */
	public void handlePushResultIterable(Iterable<PushResult> pushResultIterable) throws GitOpsException {
		if(pushResultIterable != null){
			for (PushResult pushResult : pushResultIterable) {
				Iterable<RemoteRefUpdate> remoteRefUpdates =  pushResult.getRemoteUpdates();
				if(remoteRefUpdates != null){
					for (RemoteRefUpdate remoteRefUpdate : remoteRefUpdates) {
						RemoteRefUpdate.Status pushStatus = remoteRefUpdate.getStatus();
						if (!pushStatus.equals(RemoteRefUpdate.Status.OK)) {
							if(StringUtils.isNotEmpty(pushResult.getMessages())){
								throw new GitOpsException(pushResult.getMessages());
							}
						}
					}
				}
			}
		}
	}

    /**
     * 下载整个仓库目录
     *
     * @param commitId commit的hash id或者是branch路径，例如：refs/remotes/origin/master
     * @param out      文件数据的输出到流
     * @throws GitOpsException
     */
    public void downloadRepo(String commitId, OutputStream out) throws GitOpsException {
		GitRepository gitRepo = null;
		try {
			gitRepo = new GitRepository(repoLocalDir);
			gitRepo.downloadRepo(commitId,out);
		}catch (GitOpsException e) {
			throw new GitOpsException("Dwonload repo at " + commitId + " failed, " + e.getMessage(), e);
		} finally {
			if (gitRepo != null) {
				gitRepo.close();
			}
		}
    }

	/**
	 * 以流的方式从仓库存储中获取某个目录的数据
	 *
	 * @param commitId commit的hash id或者是branch路径，例如：refs/remotes/origin/master
	 * @param filePath
	 * @param out      文件数据的输出到流
	 * @throws GitOpsException
	 */
	public void downloadDir(String commitId, String filePath, OutputStream out) throws GitOpsException {
		GitRepository gitRepo = null;
		try {
			gitRepo = new GitRepository(repoLocalDir);
			gitRepo.downloadDir(commitId, filePath, out);
		}catch (GitOpsException e) {
			throw new GitOpsException("Download " + filePath + " failed, " + e.getMessage(), e);
		} finally {
			if (gitRepo != null) {
				gitRepo.close();
			}
		}
	}

    /**
     * 以流的方式从仓库存储中获取文件数据
     *
     * @param commitId commit的hash id或者是branch路径，例如：refs/remotes/origin/master
     * @param filePath
     * @param out      文件数据的输出到流
     * @throws GitOpsException
     */
    public void downloadFile(String commitId, String filePath, OutputStream out) throws GitOpsException {
		GitRepository gitRepo = null;
		try {
			gitRepo = new GitRepository(repoLocalDir);
			gitRepo.downloadFile(commitId, filePath, out);
		}catch (GitOpsException e) {
				throw new GitOpsException("Download " + filePath + " failed, " + e.getMessage(), e);
		} finally {
			if (gitRepo != null) {
				gitRepo.close();
			}
		}
    }

    /**
     * 从仓库存储中获取文件数据
     *
     * @param commitId commit的hash id或者是branch路径，例如：refs/remotes/origin/master
     * @param filePath
     * @return 以字符串的方式返回文件内容，为了防止过度使用内存，最多返回10M内容，大文件应该用getSourceFileStream
     * @throws GitOpsException
     */
    public String getFileContent(String commitId, String filePath, int maxSize) throws GitOpsException {
		GitRepository gitRepo = null;
		try {
			gitRepo = new GitRepository(repoLocalDir);
			return gitRepo.getFileContent(commitId, filePath, maxSize);
		} finally {
			if (gitRepo != null) {
				gitRepo.close();
			}
		}
    }

    /**
     * 从仓库存储中获取文件部分行数据
     *
     * @param commitId commit的hash id或者是branch路径，例如：refs/remotes/origin/master
     * @param filePath
     * @param startLine 开始行号
     * @param lineCount 行数，如果是正向获取则为正数，如果是负向获取则为负数
     * @return 以字符串列表的方式返回文件内容
     * @throws GitOpsException
     */
    public List<String> getFileLines(String commitId, String filePath, int startLine, int lineCount) throws GitOpsException {
		GitRepository gitRepo = null;
		try {
			gitRepo = new GitRepository(repoLocalDir);
			return gitRepo.getFileLines(commitId, filePath, startLine, lineCount);
		} finally {
			if (gitRepo != null) {
				gitRepo.close();
			}
		}
    }
    
	/**
	 * 获取某个目录下的所有文件和子目录
	 *
	 * @param commitId commit的散列字串
	 * @param filePath 相对于工程根的目录路径
	 * @return
	 * @throws GitOpsException
	 */
	public List<FileInfo> listFolder(String commitId, String filePath) throws GitOpsException {
		GitRepository gitRepo = null;
		try {
			gitRepo = new GitRepository(repoLocalDir);
			return gitRepo.listFolder(commitId, filePath);
		} finally {
			if (gitRepo != null) {
				gitRepo.close();
			}
		}
	}

	/**
	 * 获取某个目录下的所有文件和子目录(支持分页)
	 *
	 * @param commitId commit的hash字串
	 * @param filePath 相对于工程根的目录路径
	 * @param skipCount 略过的数量，当skipCount=0时，代表不略过
	 * @param limitCount 查询的最大数量，当limitCount=0是，代表查询全部
	 * @author fengt
	 * @return
	 * @throws GitOpsException
	 */
	public List<FileInfo> listFolder(String commitId, String filePath,int skipCount, int limitCount) throws GitOpsException {
		GitRepository gitRepo = null;
		try {
			gitRepo = new GitRepository(repoLocalDir);
			return gitRepo.listFolder(commitId, filePath, skipCount, limitCount);
		} finally {
			if (gitRepo != null) {
				gitRepo.close();
			}
		}
	}

	/**
	 * 分析commit，用于commit修改代码的详细的diff信息，用于页面展示文件修改比对的展示 同时，包括：commit的作者，修改时间，提交人，提交时间
	 *
	 * @param ref commit引用，例如："refs/remotes/origin/2.0.0" 或 commitId
	 * @param filePath 文件路径
	 * @param maxChangeCount 最大diff行限制，超出限制则默认折叠diff行
	 * @return
	 * @throws GitOpsException
	 */
	public List<FileDiffInfo> getDiffInfo(String ref, String filePath, int maxChangeCount) throws GitOpsException {
		RevWalk revWalk = null;
		try {
			Repository repo = wcGit.getRepository();
			revWalk = new RevWalk(repo);

			ObjectId commitObjId = repo.resolve(ref);
			RevCommit commit = revWalk.parseCommit(commitObjId);

			if (commit.getParentCount() > 0) {
				return getDiffInfo(ref, commit.getParent(0).getName(), filePath, maxChangeCount);
			}
			
			return null;
		} catch (Exception e) {
			throw new GitOpsException(e);
		}
	}
	
	/**
	 * 分析commit，用于commit修改代码的详细的diff信息，用于页面展示文件修改比对的展示 同时，包括：commit的作者，修改时间，提交人，提交时间
	 *
	 * @param newRef commit引用，例如："refs/remotes/origin/2.0.0"
	 * @param oldRef commit引用，例如："refs/remotes/origin/1.0.0"
	 * @return
	 * @throws GitOpsException
	 */
	public List<FileDiffInfo> getDiffInfo(String newRef, String oldRef) throws GitOpsException {
		return getDiffInfo(newRef, oldRef, "", -1);
	}
	
	/**
	 * 分析commit，用于commit修改代码的详细的diff信息，用于页面展示文件修改比对的展示 同时，包括：commit的作者，修改时间，提交人，提交时间
	 *
	 * @param newRef commit引用，例如："refs/remotes/origin/2.0.0"
	 * @param oldRef commit引用，例如："refs/remotes/origin/1.0.0"
	 * @param filePath 指定的文件diff, 不传默认取所有文件的diff
	 * @param maxChangeCount 最大获取的changge数量限制, -1表示没有限制, 0表示只取diff基本数据
	 * @return
	 * @throws GitOpsException
	 */
	public List<FileDiffInfo> getDiffInfo(String newRef, String oldRef, String filePath, int maxChangeCount) throws GitOpsException {
		GitRepository gitRepo = null;
		try {
			gitRepo = new GitRepository(repoLocalDir);
			return gitRepo.getDiffInfo(newRef, oldRef, filePath, maxChangeCount);
		} finally {
			if (gitRepo != null) {
				gitRepo.close();
			}
		}
	}

	/**
	 * 获取working copy的当前状态，列出文件修改和冲突的清单
	 *
	 * @return
	 * @throws GitOpsException
	 */
	public MergeResultInfo getStatus() throws GitOpsException {

		try {
			MergeResultInfo mergeResultInfo = new MergeResultInfo();
			List<MergeFileEntry> fileEntryList = new ArrayList<MergeFileEntry>();

			StatusCommand statusCommand = wcGit.status();

			Status status = statusCommand.call();

			Collection<String> added = status.getAdded();
			Collection<String> changed = status.getChanged();
			Collection<String> removed = status.getRemoved();
			Collection<String> modified = status.getModified();
			Collection<String> missing = status.getMissing();
			Map<String, StageState> conflicting = status.getConflictingStageState();

			// build a sorted list of all paths except untracked and ignored
			TreeSet<String> sorted = new TreeSet<>();
			sorted.addAll(added);
			sorted.addAll(changed);
			sorted.addAll(removed);
			sorted.addAll(modified);
			sorted.addAll(missing);
			sorted.addAll(conflicting.keySet());

			// list each path
			for (String path : sorted) {
				MergeFileEntry fileEntry = new MergeFileEntry(path);

				if (added.contains(path)) {
					// x = 'A';
					fileEntry.setMergeStatus(MergeFileStatus.ADDED);
				} else if (changed.contains(path)) {
					// x = 'M';
					fileEntry.setMergeStatus(MergeFileStatus.UPDATED);
				} else if (removed.contains(path)) {
					// x = 'D';
					fileEntry.setMergeStatus(MergeFileStatus.DELETED);
				}

				if (modified.contains(path)) {
					// y = 'M';
					fileEntry.setMergeStatus(MergeFileStatus.MERGED);
				} else if (missing.contains(path)) {
					// y = 'D';
					fileEntry.setMergeStatus(MergeFileStatus.DELETED);
				}

				if (conflicting.containsKey(path)) {
					StageState stageState = conflicting.get(path);
					fileEntry.setConflict(true);
					mergeResultInfo.setConflict(true);

					switch (stageState) {
						case BOTH_DELETED:
							// x = 'D';
							// y = 'D';
							fileEntry.setMergeStatus(MergeFileStatus.TREE_CONFLICTED);
							break;
						case ADDED_BY_US:
							// x = 'A';
							// y = 'U';
							fileEntry.setMergeStatus(MergeFileStatus.TREE_CONFLICTED);
							break;
						case DELETED_BY_THEM:
							// x = 'U';
							// y = 'D';
							fileEntry.setMergeStatus(MergeFileStatus.TREE_CONFLICTED);
							break;
						case ADDED_BY_THEM:
							// x = 'U';
							// y = 'A';
							fileEntry.setMergeStatus(MergeFileStatus.TREE_CONFLICTED);
							break;
						case DELETED_BY_US:
							// x = 'D';
							// y = 'U';
							fileEntry.setMergeStatus(MergeFileStatus.TREE_CONFLICTED);
							break;
						case BOTH_ADDED:
							// x = 'A';
							// y = 'A';
							fileEntry.setMergeStatus(MergeFileStatus.CONFLICTED);
							break;
						case BOTH_MODIFIED:
							// x = 'U';
							// y = 'U';
							fileEntry.setMergeStatus(MergeFileStatus.CONFLICTED);
							break;
						default:
							throw new IllegalArgumentException("Unknown StageState: " + stageState);
					}
				}
				fileEntryList.add(fileEntry);
			}

			TreeSet<String> untracked = new TreeSet<>(status.getUntracked());
			for (String path : untracked) {
				MergeFileEntry fileEntry = new MergeFileEntry(path, MergeFileStatus.UNTRACKED, false);
				fileEntryList.add(fileEntry);
			}

			mergeResultInfo.setMergeFileEntrys(fileEntryList);

			return mergeResultInfo;
		} catch (RevisionSyntaxException | GitAPIException e) {
			throw new GitOpsException("Get Status failed " + e.getMessage(), e);
		}
	}

	/**
	 * 合并某个commit到当前working copy所在分支，合并前需要确定当前working
	 * copy处在正确的分支，合并前需要进行分支checkout和reset，合并后需要另外主动进行commit和push
	 *
	 * @param commitId commit的ID字串
	 * @return
	 * @throws GitOpsException
	 */
	public MergeResultInfo CherryPick(String commitId) throws GitOpsException {
		try {
			ObjectId mergeBase;
			mergeBase = wcGit.getRepository().resolve(commitId);

			wcGit.cherryPick().include(mergeBase).setNoCommit( true ).setMainlineParentNumber(1).call();

			MergeResultInfo mergeResultInfo = getStatus();

			return mergeResultInfo;
		} catch (RevisionSyntaxException | IOException | GitAPIException e) {
			throw new GitOpsException("Merge failed " + e.getMessage(), e);
		}
	}

	/**
	 * 合并多个commit到当前working copy所在分支，合并前需要确定当前working
	 * copy处在正确的分支，，合并前需要进行分支checkout和reset，合并后需要另外主动进行commit和push
	 *
	 * @param commitIds commit的ID字串
	 * @return
	 * @throws GitOpsException
	 */
	public MergeResultInfo CherryPick(String[] commitIds) throws GitOpsException {
		try {
			//mainlineParentNumber – the (1-based) parent number to diff against. This allows cherry-picking of merges.
			CherryPickCommand pickCmd = wcGit.cherryPick().setMainlineParentNumber(1).setNoCommit(true);
			for (String commitId : commitIds) {
				ObjectId mergeBase = wcGit.getRepository().resolve(commitId);
				pickCmd.include(mergeBase);
			}

			pickCmd.call();

			MergeResultInfo mergeResultInfo = getStatus();

			return mergeResultInfo;
		} catch (RevisionSyntaxException | IOException | GitAPIException e) {
			throw new GitOpsException("Merge failed " + e.getMessage(), e);
		}
	}

	/**
	 * 合并某个分支到当前working copy所在分支，合并前需要确定当前working
	 * copy处在正确的分支，合并前需要进行分支checkout和reset，合并后需要另外主动进行commit和push
	 *
	 * @param branch  合并到当前working copy的分支
	 * @return
	 * @throws GitOpsException
	 */
	public MergeResultInfo BranchMerge(String branch) throws GitOpsException {
		try {
			ObjectId mergeBase = resolveObjectId(branch);

			wcGit.merge().include(mergeBase).setCommit(false).setFastForward(MergeCommand.FastForwardMode.NO_FF).setSquash(false).call();

			MergeResultInfo mergeResultInfo = getStatus();

			return mergeResultInfo;
		} catch (RevisionSyntaxException | IOException | GitAPIException e) {
			throw new GitOpsException("Merge failed " + e.getMessage(), e);
		}
	}
	
	/**
	 * 使用命令执行分支合并
	 */
	public MergeResultInfo BranchMergeCommand(String srcBranch) throws GitOpsException {
		try {
			if (resolveBranch(srcBranch) == null) {
				throw new GitOpsException(String.format("branch '%s' not exist", srcBranch));
			}
			
			JSONObject ret = WorkingCopyUtils.executeCommand(repoLocalDir, "git", "merge", "origin/" + srcBranch, "--no-commit", "--no-ff", "--no-squash");
			if ("failed".equals(ret.getString("status"))) {
				throw new GitOpsException(ret.getString("result"));
			}
			
			MergeResultInfo mergeResultInfo = getStatus();
			return mergeResultInfo;
		} catch (Exception e) {
			throw new GitOpsException("Use git command merge failed " + e.getMessage(), e);
		}
	}


	/**
	 * 回退某个分支的commit，相当于做某些commit的反向patch，注意要在message里写入被撤销的commit的id以及需求号
	 * @param commitIds
	 * @return
	 * @throws GitOpsException
	 */
	public MergeResultInfo revertCommit(String[] commitIds) throws GitOpsException {
		try {
			RevertCommand revertCmd = wcGit.revert();
			for (String commitId : commitIds) {
				ObjectId mergeBase = wcGit.getRepository().resolve(commitId);
				revertCmd.include(mergeBase);
			}

			revertCmd.call();

			MergeResultInfo mergeResultInfo = getStatus();

			return mergeResultInfo;
		} catch (RevisionSyntaxException | IOException | GitAPIException e) {
			throw new GitOpsException("Revert failed " + e.getMessage(), e);
		}
	}

	/**
	 * 回退某个分支的commit，相当于做某些commit的反向patch，注意要在message里写入被撤销的commit的id以及需求号
	 * @param commitId
	 * @return
	 * @throws GitOpsException
	 */
	public MergeResultInfo revertCommit(String commitId) throws GitOpsException {
		return revertCommit(new String[] {commitId});
	}
	
	
	
	/**
	 * 直接调用git原生cherrypick命令
	 * 
	 * 合并某个commit到当前working copy所在分支，合并前需要确定当前working
	 * copy处在正确的分支，合并前需要进行分支checkout和reset，合并后需要另外主动进行commit和push
	 *
	 * @param commitId commit的ID字串
	 * @return
	 * @throws GitOpsException
	 */
	public MergeResultInfo cherryPickCommand(String commitId) throws GitOpsException {
		RevWalk revWalk = null;
		try {
			Repository repo = wcGit.getRepository();
			revWalk = new RevWalk(repo);

			ObjectId commitObjId = repo.resolve(commitId);
			RevCommit commit = revWalk.parseCommit(commitObjId);

			JSONObject retJsonObject;
			if (commit.getParentCount() > 1) {
				retJsonObject = WorkingCopyUtils.executeCommand(repoLocalDir, "git", "cherry-pick", "-n", "-m", "1", commitId);
				
			} else {
				retJsonObject = WorkingCopyUtils.executeCommand(repoLocalDir, "git", "cherry-pick", "-n", commitId);
			}

			
			MergeResultInfo mergeResultInfo = getStatus();

			if ("failed".equals(retJsonObject.getString("status")) && !mergeResultInfo.isConflict()) {
				// 重置目录可能的残留change
				reset();
				throw new GitOpsException("Cherry-Pick command failed: " + retJsonObject.getString("result"));
			}

			return mergeResultInfo;
		} catch (RevisionSyntaxException | IOException e) {
			// 重置目录可能的残留change
			reset();
			throw new GitOpsException("Cherry-Pick failed " + e.getMessage(), e);
		} finally {
			if (revWalk != null) {
				revWalk.close();
			}
		}
	}
	
	
	
	/**
	 * 直接调用 git 原生 revert 命令（jgit不支持 revert merge commit）<p>
	 * 
	 * 回退某个分支的commit，相当于做某些commit的反向patch，如果命令执行失败或出现异常，会调用 reset 重置 wc。<p>
	 * reset 后为冲突状态不会调用 reset。
	 * @param commitId
	 * @return
	 * @throws GitOpsException
	 */
	public MergeResultInfo revertCommitCommand(String commitId) throws GitOpsException {
		RevWalk revWalk = null;
		try {
			Repository repo = wcGit.getRepository();
			revWalk = new RevWalk(repo);

			ObjectId commitObjId = repo.resolve(commitId);
			RevCommit commit = revWalk.parseCommit(commitObjId);

			JSONObject retJsonObject;
			if (commit.getParentCount() > 1) {
				retJsonObject = WorkingCopyUtils.executeCommand(repoLocalDir, "git", "revert", "-n", "-m", "1", commitId);
				
			} else {
				retJsonObject = WorkingCopyUtils.executeCommand(repoLocalDir, "git", "revert", "-n", commitId);
			}
			
			MergeResultInfo mergeResultInfo = getStatus();

			if ("failed".equals(retJsonObject.getString("status")) && !mergeResultInfo.isConflict()) {
				// 重置目录可能的残留change
				reset();
				throw new GitOpsException("Revert Commit command failed: " + retJsonObject.getString("result"));
			}

			return mergeResultInfo;
		} catch (RevisionSyntaxException | IOException e) {		
			// 重置目录可能的残留change
			reset();
			throw new GitOpsException("Revert Commit failed " + e.getMessage(), e);
		} finally {
			if (revWalk != null) {
				revWalk.close();
			}
		}
	}


	/**
	 * @param sourceBranch 源分支
	 * @param targetBranch 目标分支
	 * @param commitIdList 需要判断是否合并成功的commit列表
	 * @return 返回已经合并的commit列表
	 * @throws GitOpsException
	 */
	public Set<String> isMergedInto(String sourceBranch, String targetBranch, Set<String> commitIdList) throws GitOpsException {
		RevWalk revWalk = null;
		Set<String> mergedCommitList = new HashSet<>();
		try {
			Repository repo = wcGit.getRepository();
			revWalk = new RevWalk(repo);

			RevCommit targetBranchHead = revWalk.parseCommit(repo.resolve("refs/remotes/origin/" + targetBranch));

			for (String commitId: commitIdList) {
				RevCommit commit = revWalk.parseCommit(repo.resolve(commitId));
				if (revWalk.isMergedInto(commit, targetBranchHead)){
					mergedCommitList.add(commitId);
				}
			}

			return mergedCommitList;
			
		} catch (RevisionSyntaxException | IOException e) {
			throw new GitOpsException("isMergedInto failed " + e.getMessage(), e);
		} finally {
			if (revWalk != null) {
				revWalk.close();
			}
		}
	}

	/**
	 * 检查源分支是否已经合入目标分支，没有检查 mergebase
	 * @param srcBranch
	 * @param targetBranch
	 * @return boolean      
	 * @throws
	 */
	public boolean isMerged(String srcBranch, String targetBranch) throws GitOpsException {
		RevWalk revWalk = null;
		
		try {
			Repository repo = wcGit.getRepository();
			revWalk = new RevWalk(repo);
			
			ObjectId srcObjectId = repo.resolve("refs/remotes/origin/" + srcBranch);
			ObjectId targetObjectId = repo.resolve("refs/remotes/origin/" + targetBranch);
			
			if (srcObjectId == null) {
				throw new GitOpsException(String.format("srcBranch '%s' is not exist", srcBranch));
			}
			
			if (targetObjectId == null) {
				throw new GitOpsException(String.format("targetBranch '%s' is not exist", targetBranch));
			}
			
			RevCommit targetBranchHead = revWalk.parseCommit(targetObjectId);
			RevCommit srcBranchHead = revWalk.parseCommit(srcObjectId);

			return revWalk.isMergedInto(srcBranchHead, targetBranchHead);
		} catch (Exception e) {
			throw new GitOpsException("isMergedInto failed " + e.getMessage(), e);
		} finally {
			if (revWalk != null) {
				revWalk.close();
			}
		}
	}
	
	/**
	 * 检查源分支是否已经合入目标分支，结合mergebase判断。用于在代码中心创建MR后，线下合并的情况。<br/>
	 * 创建MR时记录了当时的mergebase，创建MR后线下合并后继续在源分支提交（这种情况不结合mergebase是判断不了的），则也认为已合并。<br/>
	 * @param srcBranch 源分支名称
	 * @param targetBranch 目标分支名称
	 * @param mergeBase 创建MR时记录的 mergebase commitId
	 * @return boolean      
	 * @throws
	 */
	public boolean isMerged(String srcBranch, String targetBranch, String mergeBase) throws GitOpsException {
		RevWalk revWalk = null;
		
		try {
			Repository repo = wcGit.getRepository();
			revWalk = new RevWalk(repo);
			
			ObjectId srcObjectId = repo.resolve("refs/remotes/origin/" + srcBranch);
			ObjectId targetObjectId = repo.resolve("refs/remotes/origin/" + targetBranch);
			ObjectId mergeBaseObjectId = repo.resolve(mergeBase);
			
			if (srcObjectId == null) {
				throw new GitOpsException(String.format("srcBranch '%s' is not exist", srcBranch));
			}
			
			if (targetObjectId == null) {
				throw new GitOpsException(String.format("targetBranch '%s' is not exist", targetBranch));
			}
			
			if (mergeBaseObjectId == null) {
				throw new GitOpsException(String.format("mergeBase '%s' is not exist", mergeBase));
			}
			
			RevCommit targetBranchHead = revWalk.parseCommit(targetObjectId);
			RevCommit srcBranchHead = revWalk.parseCommit(srcObjectId);
			String newMergeBaseId = getMergeBaseCommand(srcBranchHead.getName(), targetBranchHead.getName());
			
			if (newMergeBaseId == null || newMergeBaseId.equals("")) {
				throw new GitOpsException(String.format("branch '%s' and '%s' has not merge base", srcBranch, targetBranch));
			}

			// 新 mergebase = 旧 mergebase, 没做过线下合并
			if (newMergeBaseId.equals(mergeBase)) {
				return false;
			// 新mergebase在目标分支上的第一个child是一个merge commit，则已合并
			} else {
				GitRepository gitRepo = new GitRepository(repoLocalDir);
				RevCommit firstChild = gitRepo.findFirstChild(targetBranchHead, revWalk.parseCommit(repo.resolve(newMergeBaseId)));
				if (firstChild != null && firstChild.getParentCount() > 1) {
					return true;
				}
			}
			
			return false;
		} catch (Exception e) {
			throw new GitOpsException(String.format("check merge status base on merge base failed %", e.getMessage()), e);
		} finally {
			if (revWalk != null) {
				revWalk.close();
			}
		}
	}
	
	
	private ObjectId resolveObjectId(String branchName) throws IOException {
		Repository repo = wcGit.getRepository();
		ObjectId objectId = repo.resolve("refs/remotes/origin/" + branchName);
		if (objectId == null && StringUtils.startsWith(branchName, "revert-")) {
			objectId = repo.resolve("refs/heads/" + branchName);
		}
		return objectId;
	}
	
	public Map<String, String> getMergeInfo(String srcBranch, String targetBranch, String mergeBase) throws GitOpsException {
		RevWalk revWalk = null;
		
		try {
			Repository repo = wcGit.getRepository();
			revWalk = new RevWalk(repo);
			
			ObjectId srcObjectId = resolveObjectId(srcBranch);
			ObjectId targetObjectId = resolveObjectId(targetBranch);
			
			if (srcObjectId == null) {
				throw new GitOpsException(String.format("srcBranch '%s' is not exist", srcBranch));
			}
			
			if (targetObjectId == null) {
				throw new GitOpsException(String.format("targetBranch '%s' is not exist", targetBranch));
			}
			
			RevCommit targetBranchHead = revWalk.parseCommit(targetObjectId);
			RevCommit srcBranchHead = revWalk.parseCommit(srcObjectId);
			
			Map<String, String> map = new HashMap<>();
			boolean isMerged = false;
			String newMergeBaseId = null;
			
			// 提供了 mergebase，则比较新老 mergebase 判断是否合并
			if (mergeBase != null && !mergeBase.equals("")) {
				ObjectId mergeBaseObjectId = repo.resolve(mergeBase);
				if (mergeBaseObjectId == null) {
					throw new GitOpsException(String.format("mergeBase '%s' is not exist", mergeBase));
				}
				
				newMergeBaseId = getMergeBaseCommand(srcBranchHead.getName(), targetBranchHead.getName());
				
				if (newMergeBaseId == null || newMergeBaseId.equals("")) {
					throw new GitOpsException(String.format("branch '%s' and '%s' has not merge base", srcBranch, targetBranch));
				}
				
				if (newMergeBaseId.equals(mergeBase)) { // 新 mergebase = 旧 mergebase, 没做过线下合并
					isMerged = false;
				} else { // 新mergebase在目标分支上的第一个child是一个merge commit，则已合并
					GitRepository gitRepo = new GitRepository(repoLocalDir);
					RevCommit firstChild = gitRepo.findFirstChild(targetBranchHead, revWalk.parseCommit(repo.resolve(newMergeBaseId)));
					if (firstChild != null && firstChild.getParentCount() > 1) {
						isMerged = true;
					}
				}
			} else {
				isMerged = revWalk.isMergedInto(srcBranchHead, targetBranchHead);
			}

			map.put("isMerged", String.valueOf(isMerged));
			if (isMerged) {
				revWalk.reset();
				revWalk.markStart(targetBranchHead);
				revWalk.setRevFilter(RevFilter.ONLY_MERGES);

				RevCommit mergeCommit = revWalk.iterator().next();
				map.put("targetStartCommit", mergeCommit.getParent(0).getName());
				map.put("srcStartCommit", srcBranchHead.getName());
				map.put("mergeCommitId", mergeCommit.getName());
				
				if (newMergeBaseId == null) {
					newMergeBaseId = getMergeBaseCommand(srcBranchHead.getName(), targetBranchHead.getName());
				}
				
				map.put("mergeBase", newMergeBaseId);
			}
			
			return map;
		} catch (Exception e) {
			throw new GitOpsException(String.format("check merge status base on merge base failed %s", e.getMessage()), e);
		} finally {
			if (revWalk != null) {
				revWalk.close();
			}
		}
	}
	
	public Map<String, String> getMergeInfo(String srcBranch, String targetBranch) throws GitOpsException {
		return getMergeInfo(srcBranch, targetBranch, null);
	}
	
	/**
	 * 获取指定 commit 的 parent commit。如果有多个，返回和 commit 在同一个分支的 parent commit。
	 *
	 * @param ref commit引用，例如："refs/remotes/origin/2.0.0" 或 commitId
	 * @return 存在 parent 则返回 parent commit id，否则返回 null
	 * @throws GitOpsException
	 */
	public String getParent(String ref) throws GitOpsException {
		RevWalk revWalk = null;
		try {
			Repository repo = wcGit.getRepository();
			revWalk = new RevWalk(repo);

			ObjectId commitObjId = repo.resolve(ref);
			RevCommit commit = revWalk.parseCommit(commitObjId);

			if (commit.getParentCount() > 0) {
				return commit.getParent(0).getName();
			}
			
			return null;
		} catch (Exception e) {
			throw new GitOpsException(e);
		}
	}
	
	/**
	 * 使用 git 命令获取mergebase，即使两个分支已经合并，也可以拿到（jgit的接口拿不到）<pre>git merge-base commit1 commit2</pre><br>
	 * 使用前先 update workingcopy
	 * 
	 * @param srcCommitId 源分支 commitId
	 * @param  dstCommitId 目标分支 commitId
	 * @return mergebase commitId
	 * @throws GitOpsException
	 */
	public String getMergeBaseCommand(String srcCommitId, String dstCommitId) throws GitOpsException {
		try {
			JSONObject retJsonObject = WorkingCopyUtils.executeCommand(repoLocalDir, "git", "merge-base", srcCommitId, dstCommitId);
			
			if ("failed".equals(retJsonObject.getString("status"))) {
				throw new GitOpsException(String.format("Invoke 'git merge-base %s %s' failed: %s", srcCommitId, dstCommitId, retJsonObject.getString("result")));
			}
			
			return retJsonObject.getString("result");
		} catch (Exception e) {		
			throw new GitOpsException(String.format("Invoke 'git merge-base %s %s' failed: %s", srcCommitId, dstCommitId, e.getMessage()), e);
		}
	}
	
	public List<String> gitRevListCommand(String... arguments) throws GitOpsException {
		if (arguments == null || arguments.length == 0) {
			return null;
		}
		
		String[] command = new String[arguments.length + 2];
		command[0] = "git";
		command[1] = "rev-list";
		
		for (int i = 0; i < arguments.length; i++) {
			command[i + 2] = arguments[i];
		}
		
		JSONObject retJsonObject = WorkingCopyUtils.executeCommand(repoLocalDir, command);
		
		if ("failed".equals(retJsonObject.getString("status"))) {
			throw new GitOpsException(String.format("Invoke '%s' failed: %s", String.join(" " , command), retJsonObject.getString("result")));
		}
		
		String ret = retJsonObject.getString("result");
		if (StringUtils.isNotBlank(ret)) {
			return Arrays.asList(ret.split("\\n"));
		}
		
		return null;
	}
	

    /**
     * 判断文件是否存在, 不存在则会抛异常
     *
     * @param commitId
     *            Commit number
     * @param filePath
     *            相对于仓库根的路径，譬如：branches/1.0.0/src/java/test/test.java
     * @return
     * @throws GitOpsException
     */
    public void isExistsFile(String commitId, String filePath) throws GitOpsException {
        Repository repo = wcGit.getRepository();

        RevWalk revWalk = null;
        TreeWalk treeWalk = null;

        try {
            ObjectId lastCommitId = repo.resolve(commitId);
            revWalk = new RevWalk(repo);
            RevCommit commit = revWalk.parseCommit(lastCommitId);

            RevTree tree = commit.getTree();
            treeWalk = new TreeWalk(repo);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathFilter.create(filePath));

            if (!treeWalk.next()) {
                throw new GitOpsException("File " + filePath + " not found");
            }

        } catch (Exception e) {
            throw new GitOpsException("Get file " + filePath + " failed, " + e.getMessage(), e);
        } finally {
            if (revWalk != null) {
                revWalk.close();
            }
            if (treeWalk != null) {
                treeWalk.close();
            }
        }
    }

	/**
	 * 获取某个目录下的LastCommit信息
	 *
	 * @param commitId commit的散列字串
	 * @param filePath 相对于工程根的目录路径
	 * @return
	 * @throws GitOpsException
	 */
	public CommitInfo getFilePathLastCommit(String commitId, String filePath) throws GitOpsException {
		GitRepository gitRepo = null;
		try {
			gitRepo = new GitRepository(repoLocalDir);
			return gitRepo.getFilePathLastCommit(commitId, filePath);
		} finally {
			if (gitRepo != null) {
				gitRepo.close();
			}
		}
	}
	
	/**
	 * 已经存在锁时, 报错抛出的信息, 上层调用时可以不用try catch
	 *
	 */
	public void lock() {
        try {
            lockFile = new File(repoLocalDir + ".lock");
            if (!lockFile.getParentFile().exists()) {
        		// 仓库目录不存在, 或者后面又被人删除了, 文件锁打开前需要保证仓库目录存在
				File parent = lockFile.getParentFile();
				if (parent == null) {
					return;
				}
				org.apache.commons.io.FileUtils.forceMkdir(parent);
			}
            randomAccessFile = new RandomAccessFile(lockFile, "rw");
            lockChannel = randomAccessFile.getChannel();
            buildLock = lockChannel.lock(0, 4096, true);

        } catch (IOException | OverlappingFileLockException e) {
            throw new LockFailedException(String.format("当前仓库 '%s' 存在未完成的同步或合并操作，请稍后重试", this.remoteUrl), e);
        }
	}
	
	public void tryLock() {
		lockFile = new File(repoLocalDir + ".lock");
		String lockedErrorMessage = String.format("当前仓库 '%s' 存在未完成的同步或合并操作，请稍后重试", this.remoteUrl);

		if (lockFile.exists()) {
			try {
				randomAccessFile = new RandomAccessFile(lockFile, "rw");
				lockChannel = randomAccessFile.getChannel();
				buildLock = lockChannel.tryLock(0, 4096, true);
				if (buildLock == null) {
					throw new LockFailedException(lockedErrorMessage);
				}
			} catch (IOException | OverlappingFileLockException e) {
				throw new LockFailedException(lockedErrorMessage, e);
			} finally {
				unlock();
			}
		}
	}
	
	public void unlock() {
		if (buildLock != null) {
			try {
				buildLock.release();
			} catch (IOException ignored) {
			}
		}
		if (lockChannel != null) {
			try {
				lockChannel.close();
			} catch (IOException ignored) {
			}
		}
		if (randomAccessFile != null) {
			try {
				randomAccessFile.close();
			} catch (IOException ignored) {
			}
		}       
		if (lockFile != null && buildLock != null) {
			lockFile.delete();
		}
	}
	

}
