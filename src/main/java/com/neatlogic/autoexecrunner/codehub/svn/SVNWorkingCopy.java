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
package com.neatlogic.autoexecrunner.codehub.svn;

import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.codehub.dto.commit.CommitInfo;
import com.neatlogic.autoexecrunner.codehub.dto.diff.FileDiffInfo;
import com.neatlogic.autoexecrunner.codehub.dto.diff.FileInfo;
import com.neatlogic.autoexecrunner.codehub.dto.merge.MergeResultInfo;
import com.neatlogic.autoexecrunner.codehub.exception.LockFailedException;
import com.neatlogic.autoexecrunner.codehub.exception.SVNOpsException;
import com.neatlogic.autoexecrunner.codehub.utils.RegexUtils;
import com.neatlogic.autoexecrunner.codehub.utils.WorkingCopyUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.*;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnRemoteDelete;
import org.tmatesoft.svn.core.wc2.SvnTarget;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SVNWorkingCopy {
	Logger logger = LoggerFactory.getLogger(SVNWorkingCopy.class);

	private String remoteUrl;
	private SVNURL repoUrl;
	private ISVNAuthenticationManager authManager = null;
	private SVNRepository repo = null;
	private String branchesPath = "branches";
	private int branchesPathLen = branchesPath.length();

	private String trunkPath = "trunk";
	private int trunkPathLen = trunkPath.length();

	private String tagsPath = "tags";
	private int tagsPathLen = tagsPath.length();

	private String startPath = "";

	private String repoLocalDir = "";
	private String username = "";
	private String password = "";
	
	private String repositoryRoot = "";
	
	private SVNCommand command;

	
	private File lockFile = null;
	private RandomAccessFile randomAccessFile = null;
	private FileChannel lockChannel = null;
	private FileLock buildLock = null;
	
	
	/**
	 * 构造函数，不checkout代码仓库
	 */
	public SVNWorkingCopy(String repoLocalDir, String remoteUrl, String username, String password, String trunkPath, String branchesPath, String tagsPath) throws SVNOpsException {
		this(repoLocalDir, remoteUrl, username, password, trunkPath, branchesPath, tagsPath, false);
	}

	/**
	 * 构造函数，如果本地目录不存在，则通过svn的URL checkout主干
	 * 
	 * @param repoLocalDir 本地的Working Copy目录
	 * @param remoteUrl    SVN的仓库的远程URL（不包括子目录）
	 * @param username     用户名
	 * @param password     密码
	 * @throws SVNOpsException
	 */
	public SVNWorkingCopy(String repoLocalDir, String remoteUrl, String username, String password, String trunkPath, String branchesPath, String tagsPath, boolean checkout) throws SVNOpsException {
		this.remoteUrl = remoteUrl;
		this.username = username;
		this.password = password;

		this.trunkPath = trunkPath;
		this.branchesPath = branchesPath;
		this.tagsPath = tagsPath;

		File localPath = null;
		
		try {
			localPath = new File(repoLocalDir);
			this.repoLocalDir = localPath.getCanonicalPath();
			
			this.repoUrl = SVNURL.parseURIEncoded(remoteUrl);

			this.repo = SVNRepositoryFactory.create(repoUrl);

			authManager = SVNWCUtil.createDefaultAuthenticationManager(username, password.toCharArray());
			repo.setAuthenticationManager(authManager);
			
			// repo.getRepositoryRoot 这个方法中会调用 Connection 链接 remote repository，所以不用再重复调 repo.testConnection()
			this.repositoryRoot = this.repo.getRepositoryRoot(true).toString();
			
			// 测试连接，可以校验用户名密码是否正确，url是否正确
			// repo.testConnection();
			
			command = new SVNCommand(username, password);
		} catch (Exception e) {
			throw new SVNOpsException("Open svn url:" + remoteUrl + " failed, " + e.getMessage(), e);
		}

		if (checkout) {
			if (!localPath.exists()) {
				this.checkout(null);
			}
		}
	}
	
	public String getRepositoryRoot() {
		return this.repositoryRoot;
	}
	
	public String getBranchesPath() {
		return branchesPath;
	}

	public void setBranchesPath(String branchesPath) {
		this.branchesPath = branchesPath;
		this.branchesPathLen = branchesPath.length();
	}

	public String getTrunkPath() {
		return trunkPath;
	}

	public void setTrunkPath(String trunkPath) {
		this.trunkPath = trunkPath;
		this.trunkPathLen = trunkPath.length();
	}

	public String getTagsPath() {
		return tagsPath;
	}

	public void setTagsPath(String tagsPath) {
		this.tagsPath = tagsPath;
		this.tagsPathLen = tagsPath.length();
	}

	public String getStartPath() {
		return startPath;
	}

	public void setStartPath(String startPath) {
		if (startPath.startsWith("/")) {
			startPath = startPath.substring(1);
		}
		this.startPath = startPath;
	}

	public void locateToBranch(String branch) {
		if (StringUtils.isNotBlank(trunkPath) && trunkPath.equals(branch)) {
			this.startPath = trunkPath;
		} else {
			this.startPath = branchesPath + "/" + branch;
		}
	}

	public void locateToTag(String tag) {
		this.startPath = tagsPath + "/" + tag;
	}

	public int getBranchesPathLen() {
		return branchesPathLen;
	}

	public int getTrunkPathLen() {
		return trunkPathLen;
	}

	public int getTagsPathLen() {
		return tagsPathLen;
	}

	/**
	 * 关闭SVN远程仓库的实例，需要主动关闭，防止资源泄漏
	 */
	public void close() {
		if (repo != null) {
			repo.closeSession();
		}
	}

	/**
	 * 更新当前checkout的分支的内容
	 * 
	 * @throws SVNOpsException
	 */
	public void update() throws SVNOpsException {
		// prepare a new folder for the cloned repository
		File localPath = new File(repoLocalDir);

		DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
		SVNClientManager ourClientManager = SVNClientManager.newInstance(options, authManager);

		SVNUpdateClient updateClient = ourClientManager.getUpdateClient();

		try {
			boolean ignoreExternals = false;
			updateClient.setIgnoreExternals(ignoreExternals);

			if (localPath.exists()) {
				updateClient.doUpdate(localPath, SVNRevision.HEAD, SVNDepth.fromRecurse(true), true, false);
			}
		} catch (SVNException e) {
			throw new SVNOpsException("clean up " + repoLocalDir + " failed, " + e.getMessage(), e);
		} finally {
			// FIX: https://issues.tmatesoft.com/issue/SVNKIT-670
			ourClientManager.dispose();
		}

	}

	/**
	 * 重置当前Working Copy并通过远程仓库更新本地目录
	 * 
	 * @throws SVNOpsException
	 */
	public void resetAndUpdate() throws SVNOpsException {
		// prepare a new folder for the cloned repository
		File localPath = new File(repoLocalDir);

		DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
		SVNClientManager ourClientManager = SVNClientManager.newInstance(options, authManager);

		SVNUpdateClient updateClient = ourClientManager.getUpdateClient();

		try {
			boolean ignoreExternals = false;
			updateClient.setIgnoreExternals(ignoreExternals);

			if (localPath.exists()) {
				// SVNWCClient wcClient = ourClientManager.getWCClient();
				// wcClient.doCleanup(localPath, false, true, true, true, true, ignoreExternals);
				// wcClient.doRevert(new File[] { localPath }, SVNDepth.fromRecurse(true), null);
				// updateClient.doUpdate(localPath, SVNRevision.HEAD, SVNDepth.fromRecurse(true), true, false);
				JSONObject ret = WorkingCopyUtils.executeCommand(repoLocalDir, this.command.cleanup().end());
				logger.debug("svn cleanup:" + ret);
				if (!StringUtils.equals("success", ret.getString("status"))) {
					throw new SVNOpsException(ret.getString("result"));
				}
			}
		} catch (Exception e) {
			throw new SVNOpsException("clean up " + repoLocalDir + " and update with svn url:" + remoteUrl + " failed, " + e.getMessage(), e);
		} finally {
			// FIX: https://issues.tmatesoft.com/issue/SVNKIT-670
			ourClientManager.dispose();
		}

	}

	/**
	 * revert working copy, use svn command
	 *
	 * @throws SVNOpsException
	 */
	public void revertCommand() throws SVNOpsException {
		File localPath = new File(repoLocalDir);

		if (localPath.exists()) {
			JSONObject obj;

			long start = System.currentTimeMillis();
			logger.info("command: svn clean up start...");
			obj = WorkingCopyUtils.executeCommand(repoLocalDir, this.command.cleanup().end());
			logger.info("command: svn cleanup end, time: " + (System.currentTimeMillis() - start) / 1000 + "s, ret is " + obj.toString());
			if (!StringUtils.equals(obj.getString("status"), "success")) {
				throw new SVNOpsException(obj.getString("result"));
			}

			start = System.currentTimeMillis();
			logger.info("command: svn revert start...");
			obj = WorkingCopyUtils.executeCommand(repoLocalDir, this.command.revert().setOptions(repoLocalDir, "-R").end());
			logger.info("command: svn revert end, time: " + (System.currentTimeMillis() - start) / 1000 + "s, ret is " + obj.toString());
			if (!StringUtils.equals(obj.getString("status"), "success")) {
				throw new SVNOpsException(obj.getString("result"));
			}
		}
	}

	/**
	 * revert working copy
	 *
	 * @throws SVNOpsException
	 */
	public void revert() throws SVNOpsException {
		File localPath = new File(repoLocalDir);

		DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);

		SVNClientManager ourClientManager = SVNClientManager.newInstance(options, authManager);

		SVNWCClient wcClient = ourClientManager.getWCClient();

		try {
			boolean ignoreExternals = false;

			if (localPath.exists()) {
				long start = System.currentTimeMillis();
				logger.info("svn cleanup start...");
				wcClient.doCleanup(localPath, false, true, true, true, true, ignoreExternals);
				logger.info("svn cleanup end, time: " + (System.currentTimeMillis() - start) / 1000 + "s");

				start = System.currentTimeMillis();
				logger.info("revert start...");

				wcClient.doRevert(new File[] {localPath}, SVNDepth.INFINITY, null);
				logger.info("svn revert end, time: " + (System.currentTimeMillis() - start) / 1000 + "s");
			}
		} catch (Exception e) {
			throw new SVNOpsException("revert " + repoLocalDir + " failed, " + e.getMessage(), e);
		} finally {
			// FIX: https://issues.tmatesoft.com/issue/SVNKIT-670
			ourClientManager.dispose();
		}
	}
	
	/**
	 * 假设当前WorkingCopy有如下冲突列表， src4: 将在目标分支被删除, src3: 下有文件冲突, src5: 将在目标分支被添加
	 * <pre>
	 * [src4, src3/uddi-address.xml, src5, src5/uddi-address.xml, uddi-address.xml]
	 * </pre>
	 * revert 时递归处理 src4, src3, src5, 再用 FILES 方式处理 workingcopy
	 * @throws SVNOpsException
	 */
	public void revert(List<String> fileList) throws SVNOpsException {
		List<File> paths = new ArrayList<>();
		
		if (!CollectionUtils.isEmpty(fileList)) {
			Set<String> revertSet = new HashSet<>();
			for (int i = 0; i < fileList.size(); i++) {
				String[] arr = fileList.get(i).split("[\\\\/]");
				revertSet.add(arr[0]);
			}
			
			for (String filePath: revertSet) {
				File file = new File(repoLocalDir + File.separator + filePath);
				if (file.isDirectory()) {
					paths.add(file);
				}
			}
		}

		File localPath = new File(repoLocalDir);

		DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);

		SVNClientManager ourClientManager = SVNClientManager.newInstance(options, authManager);

		SVNWCClient wcClient = ourClientManager.getWCClient();

		try {
			boolean ignoreExternals = false;

			if (localPath.exists()) {
				long start = System.currentTimeMillis();
				logger.info("svn cleanup start...");
				wcClient.doCleanup(localPath, false, true, true, true, true, ignoreExternals);
				logger.info("svn cleanup end, time: " + (System.currentTimeMillis() - start) / 1000 + "s");

				start = System.currentTimeMillis();
				logger.info("revert start...");

				if (paths.size() > 0) {
					wcClient.doRevert(paths.toArray(new File[0]), SVNDepth.INFINITY, null);
				}
				wcClient.doRevert(new File[] {localPath}, SVNDepth.FILES, null);
				logger.info("svn revert end, time: " + (System.currentTimeMillis() - start) / 1000 + "s");
			}
		} catch (Exception e) {
			throw new SVNOpsException("revert " + repoLocalDir + " failed, " + e.getMessage(), e);
		} finally {
			// FIX: https://issues.tmatesoft.com/issue/SVNKIT-670
			ourClientManager.dispose();
		}
	}

	/**
	 * 获取某个commit的简要的commit信息
	 *
	 * @param rev
	 * @return
	 * @throws SVNOpsException
	 */
	public CommitInfo getCommit(long rev) throws SVNOpsException {
		List<CommitInfo> commitInfoList = new LinkedList<CommitInfo>();

		try {
			repo.log(new String[] { "" }, rev, rev - 1, true, true, 1, new LogEntryHandler(this, commitInfoList, true, false, false, true));
			CommitInfo commitInfo = null;
			if (commitInfoList.size() > 0) {
				commitInfo = commitInfoList.get(0);
			}
			return commitInfo;
		} catch (SVNException e) {
			throw new SVNOpsException("get commit detail from url:" + remoteUrl + " failed, " + e.getMessage(), e);
		}
	}

	/**
	 * 获取某个commit的详细的commit信息，包括diff
	 *
	 * @param rev
	 * @return
	 * @throws SVNOpsException
	 */
	public CommitInfo getCommitDetail(long rev) throws SVNOpsException {
		List<CommitInfo> commitInfoList = new LinkedList<CommitInfo>();

		try {
			repo.log(new String[] { "" }, rev, rev - 1, true, true, 1, new LogEntryHandler(this, commitInfoList, true, false, false, false));
			CommitInfo commitInfo = null;
			if (commitInfoList.size() > 0) {
				commitInfo = commitInfoList.get(0);
			}
			return commitInfo;
		} catch (SVNException e) {
			throw new SVNOpsException("get commit detail from url:" + remoteUrl + " failed, " + e.getMessage(), e);
		}
	}

	/**
	 * 按照时间倒序获取从startCommitId开始的，最近不超过maxCount，不早于startTime的提交日志
	 *
	 * @param startCommitId 获取日志的启始commit id(不包含此commit)， null代表从头部开始
	 * @param maxCount      最多获取的commit数，0:不限制，否则最多获取maxCount条记录
	 * @param startTime     最早的commit的时间（epochTime），0:不限制，否则获取的commit的时间要大于等于endTime
	 * @param logOnly       true:只获取日志不分析commit修改的文件
	 * @throws SVNOpsException
	 */
	public List<CommitInfo> getCommits(String startCommitId, int maxCount, long startTime, boolean logOnly) throws SVNOpsException {
		List<CommitInfo> commitInfoList = new LinkedList<CommitInfo>();

		try {
			long startRevision = 0;
			if (startCommitId != null) {
				startRevision = Long.parseLong(startCommitId);
			}
			if (startRevision == 0) {
				startRevision = repo.getLatestRevision();
			}

			long endRevision = 1;
			if (startTime > 0) {
				Date startDate = new Date(startTime * 1000);
				endRevision = repo.getDatedRevision(startDate);
			}
			repo.log(new String[] { "" }, startRevision, endRevision, true, true, maxCount>0?maxCount + 1:maxCount, new LogEntryHandler(this, commitInfoList, true, false, false, logOnly));

			if (startCommitId != null && commitInfoList.size() > 0 && commitInfoList.get(0).getId().equals(startCommitId)) {
				commitInfoList.remove(0);
			}

			return commitInfoList;
		} catch (SVNException e) {
			throw new SVNOpsException("get log from url:" + remoteUrl + " failed, " + e.getMessage(), e);
		}
	}

	/**
	 * 按照提交时间倒序获取某个被删除分支上从startCommitId开始，到endCommitId结束的commit列表
	 *
	 * @param branch 被删除分支名称
	 * @param startCommitId 获取日志的启始commit id(包含此commit)，必传
	 * @param endCommitId 获取日志的结束commit id(包含此commit)，必传
	 * @param maxCount      最多获取的commit数，0:不限制，否则最多获取maxCount条记录
	 * @param logOnly       true:只获取日志不分析commit修改的文件
	 * @throws SVNOpsException
	 */
	public List<CommitInfo> getBranchDeletedCommitsByCommitIdRange(String branch, String startCommitId, String endCommitId, int maxCount, boolean logOnly) throws SVNOpsException {
		if (startCommitId == null || startCommitId.equals("")) {
			throw new SVNOpsException("argument 'startCommitId' is undefined");
		}

		if (endCommitId == null || endCommitId.equals("")) {
			throw new SVNOpsException("argument 'endCommitId' is undefined");
		}
		
        List<CommitInfo> commitInfoList = new LinkedList<CommitInfo>();

		try {
			long startRevision = Long.parseLong(startCommitId);
			long endRevision = Long.parseLong(endCommitId);
			
			branch = getRealBranchPath(branch);

			repo.log(new String[] { branch }, startRevision, endRevision, true, true, maxCount, new LogEntryHandler(this, commitInfoList, true, false, false, logOnly));

			return commitInfoList;
		} catch (SVNException e) {
			throw new SVNOpsException("get log from url:" + remoteUrl + " failed, " + e.getMessage(), e);
		}
	}

	/**
	 * 获取某个分支从startCommitId开始，到endCommitId结束，最近不超过maxCount的commit log。 对
	 * startCommitId 和 endCommitId 的定义：
	 * <p/>
	 * 
	 * <pre>
	 *     history-commits                                     lastest-commit
	 *           |                                                   |
	 * branch ------------------------------------------------------>HEAD
	 *           ^          << scan direction <<         ^
	 *        endCommitId                           startCommitId
	 * </pre>
	 * 
	 * 
	 * @param branch        分支名称
	 * @param startCommitId 获取日志的启始commit id(不包含此commit)， null代表从HEAD开始
	 * @param endCommitId   获取日志的结束commit id(不包含此commit)， null代表不限制结束点
	 * @param maxCount      最多获取的commit数，0:不限制，否则最多获取maxCount条记录。
	 * @param logOnly       true:只获取日志不分析commit修改的文件
	 * @throws SVNOpsException
	 */
	public List<CommitInfo> getCommitsForBranchByCommitIdRange(String branch, String startCommitId, String endCommitId, int maxCount, boolean logOnly) throws SVNOpsException {
		locateToBranch(branch);
		List<CommitInfo> commitInfoList = new LinkedList<CommitInfo>();
		
		DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
		SVNClientManager ourClientManager = SVNClientManager.newInstance(options, authManager);
		// XXXClient classes should be always constructed via SVNClientManager, it would allow to avoid such problems.

		SVNLogClient logClient = ourClientManager.getLogClient();
//		SVNLogClient logClient = new SVNLogClient(authManager, options);

		try {
			SVNRevision pegRevision = SVNRevision.HEAD;
			SVNRevision startRevision = SVNRevision.HEAD;

			if (StringUtils.isNotEmpty(startCommitId)) {
				// fixbug: 避免startCommitId为空时报错
				long startCommit = Long.parseLong(startCommitId);
				pegRevision = SVNRevision.create(startCommit);
				startRevision = SVNRevision.create(startCommit);
			}

			SVNRevision endRevision = SVNRevision.create(1);
			if (StringUtils.isNotEmpty(endCommitId)) {
				long endCommit = Long.parseLong(endCommitId);
				// fixbug: endCommit不能+1, 如果传入endCommit为最新head的commitId, 则+1后结果会报错, 找不到revision
				endRevision = SVNRevision.create(endCommit);
			}

			// fixbug: SVNRevision.HEAD==-1 会比 endRevision==1还小
			if (startRevision.getNumber() != -1 && startRevision.getNumber() < endRevision.getNumber()) {
				throw new SVNOpsException(String.format("commit range invalid, startCommitId must greater then endCommitId, startCommitId: %s, endCommitId: %s.", startCommitId, endCommitId));
			}

			// 修复当分支是trunk时路径错误
			SVNURL effectiveUrl = repoUrl.appendPath(getRealBranchPath(branch), true);

			int realMaxCount = maxCount;
			if (maxCount > 0) {
				maxCount += 2;
			}
			// 得到的 commitInfoList 为按 revision id 降序排列
			logClient.doLog(effectiveUrl, null, pegRevision, startRevision, endRevision, true, true, false, maxCount, null, new LogEntryHandler(this, commitInfoList, true, false, false, logOnly));

			// 修复分页导致丢数据问题
			return filterCommitInfoList(commitInfoList, startCommitId, endCommitId, realMaxCount);
		} catch (SVNException e) {
			throw new SVNOpsException("get log from url:" + remoteUrl + " failed, " + e.getMessage(), e);
		} finally {
			ourClientManager.dispose();
		}
	}

	/**
	 * 获取某个分支从startCommitId开始，到endCommitId结束，最近不超过maxCount的commit log。
	 * 对 startCommitId 和 endCommitId 的定义：<p/>
	 * <pre>
	 *     history-commits                                     lastest-commit
	 *           |                                                   |
	 * branch ------------------------------------------------------>HEAD
	 *           ^          << scan direction <<         ^
	 *        endCommitId                           startCommitId
	 * </pre>
	 * @param branch        分支名称
	 * @param startCommitId 获取日志的启始commit id(包含此commit)， null代表从HEAD开始
	 * @param endCommitId 获取日志的结束commit id(包含此commit)， null代表不限制结束点
	 * @param maxCount      最多获取的commit数，0:不限制，否则最多获取maxCount条记录
	 * @param logOnly       true:只获取日志不分析commit修改的文件
	 * @throws SVNOpsException
	 */
	public List<CommitInfo> getCommitsContainStartEndForBranchByCommitIdRange(String branch, String startCommitId, String endCommitId, int maxCount, boolean logOnly) throws SVNOpsException {
		locateToBranch(branch);
		List<CommitInfo> commitInfoList = new LinkedList<CommitInfo>();
		
		DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
		SVNClientManager ourClientManager = SVNClientManager.newInstance(options, authManager);
		
		try {
			SVNLogClient logClient = ourClientManager.getLogClient();
			SVNRevision pegRevision = SVNRevision.HEAD;
			SVNRevision startRevision = SVNRevision.HEAD;

			if (StringUtils.isNotEmpty(startCommitId)) {
				// fixbug: 避免startCommitId为空时报错
				long startCommit = Long.parseLong(startCommitId);
				pegRevision = SVNRevision.create(startCommit);
				startRevision = SVNRevision.create(startCommit);
			}

			SVNRevision endRevision = SVNRevision.create(1);
			if (StringUtils.isNotEmpty(endCommitId)) {
				long endCommit = Long.parseLong(endCommitId);
				// fixbug: endCommit不能+1, 如果传入endCommit为最新head的commitId, 则+1后结果会报错, 找不到revision
				endRevision = SVNRevision.create(endCommit);
			}

			// fixbug: SVNRevision.HEAD==-1 会比 endRevision==1还小
			if (startRevision.getNumber() != -1 && startRevision.getNumber() < endRevision.getNumber()) {
				throw new SVNOpsException(String.format("commit range invalid, startCommitId must greater then endCommitId, startCommitId: %s, endCommitId: %s.", startCommitId, endCommitId));
			}

			// 修复当分支是trunk时路径错误
			SVNURL effectiveUrl = repoUrl.appendPath(getRealBranchPath(branch), true);

			// fixbug: 原本这里是maxCount+1, 会导致记录总多一条, 而且传入maxCount==0的时候并不是无限制, 而是只取一条.
			logClient.doLog(effectiveUrl, null, pegRevision, startRevision, endRevision, true, true, false, maxCount, null, new LogEntryHandler(this, commitInfoList, true, false, false, logOnly));

			return commitInfoList;
		} catch (SVNException e) {
			throw new SVNOpsException("get log from url:" + remoteUrl + " failed, " + e.getMessage(), e);
		} finally {
			ourClientManager.dispose();
		}
	}


	/**
	 * 获取从某个分支从startCommitId开始，最近不超过maxCount，不早于startTime的commit log
	 *
	 * @param branch        分支名称
	 * @param startCommitId 获取日志的启始commit id(不包含此commit)， null代表从头部开始
	 * @param maxCount      最多获取的commit数，0:不限制，否则最多获取maxCount条记录
	 * @param startTime     最早的commit的时间（epochTime），0:不限制，否则获取的commit的时间要大于等于endTime
	 * @param logOnly       true:只获取日志不分析commit修改的文件
	 * @throws SVNOpsException
	 */
	public List<CommitInfo> getCommitsForBranch(String branch, String startCommitId, int maxCount, long startTime, boolean logOnly) throws SVNOpsException {

		try {
			String endCommitId = null;
			if (startTime > 0) {
				Date startDate = new Date(startTime * 1000);
				SVNRevision endRevision = SVNRevision.create(repo.getDatedRevision(startDate));
				endCommitId = String.valueOf(endRevision.getNumber());
			}
			return getCommitsForBranchByCommitIdRange(branch, startCommitId, endCommitId, maxCount, logOnly);

		} catch (SVNException e) {
			throw new SVNOpsException("get log from url:" + remoteUrl + " failed, " + e.getMessage(), e);
		}
	}

	/**
	 * 获取从某个tag从startCommitId开始，最近不超过maxCount，不早于startTime的commit log
	 *
	 * @param tag           标签名称
	 * @param startCommitId 获取日志的启始commit id(不包含此commit)， null代表从头部开始
	 * @param maxCount      最多获取的commit数，0:不限制，否则最多获取maxCount条记录
	 * @param startTime     最早的commit的时间（epochTime），0:不限制，否则获取的commit的时间要大于等于endTime
	 * @param logOnly       true:只获取日志不分析commit修改的文件
	 * @throws SVNOpsException
	 */
	public List<CommitInfo> getCommitsForTag(String tag, String startCommitId, int maxCount, long startTime, boolean logOnly) throws SVNOpsException {
		this.locateToTag(tag);
		List<CommitInfo> commitInfoList = new LinkedList<CommitInfo>();
		
		DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
		SVNClientManager ourClientManager = SVNClientManager.newInstance(options, authManager);
		SVNLogClient logClient = ourClientManager.getLogClient();

		try {
			SVNRevision pegRevision = SVNRevision.HEAD;
			SVNRevision startRevision = SVNRevision.HEAD;
			if (startCommitId != null) {
				pegRevision = SVNRevision.create(Long.parseLong(startCommitId));
				startRevision = SVNRevision.create(Long.parseLong(startCommitId));
			}
			

			SVNRevision endRevision = SVNRevision.create(1);
			if (startTime > 0) {
				Date startDate = new Date(startTime * 1000);
				endRevision = SVNRevision.create(repo.getDatedRevision(startDate));
			}

			int realMaxCount = maxCount;
			if (maxCount > 0) {
				maxCount += 1;
			}
			
			logClient.doLog(repoUrl.appendPath(tagsPath + "/" + tag, true), null, pegRevision, startRevision, endRevision, true, true, false, maxCount, null, new LogEntryHandler(this, commitInfoList, true, true, false, true));

			return filterCommitInfoList(commitInfoList, startCommitId, null, realMaxCount);
		} catch (SVNException e) {
			throw new SVNOpsException("get log from url:" + remoteUrl + " failed, " + e.getMessage(), e);
		} finally {
			ourClientManager.dispose();
		}
	}
	
	/** 判断 startCommitId、endCommitId 是否在 commitInfoList 里面，已存在则移除。超过 realMaxCount 则截取 */
	private List<CommitInfo> filterCommitInfoList(List<CommitInfo> commitInfoList, String startCommitId, String endCommitId, int realMaxCount) {
		if (commitInfoList == null || commitInfoList.isEmpty()) {
			return commitInfoList;
		}
		
		CommitInfo startCommit = commitInfoList.get(0);
		CommitInfo endCommit = commitInfoList.get(commitInfoList.size() - 1);
		
		boolean containStart = startCommit.getCommitId().equals(startCommitId);
		// 确保 start 和 end 不是同一个 commit
		boolean containEnd = !StringUtils.equals(startCommitId, endCommitId) && endCommit.getCommitId().equals(endCommitId);
		
		// startCommitId 包含在返回值中，去掉
		if (containStart) {
			commitInfoList.remove(0);
		}
		
		// endCommitId 包含在返回值中，去掉
		if (containEnd) {
			commitInfoList.remove(commitInfoList.size() - 1);
		}
		
		if (realMaxCount > 0 && commitInfoList.size() > realMaxCount) {
			return commitInfoList.subList(0, realMaxCount);
		}
		
		return commitInfoList;
	}

    /**
	 * 按照时间倒序获取某个分支(支持删除掉的分支)上从startCommitId开始，到endCommitId结束的commit列表
	 * <pre>
	 *     history-commits                                     lastest-commit
	 *           |                                                   |
	 * branch ------------------------------------------------------>HEAD
	 *           ^          << scan direction <<         ^
	 *        endCommitId                           startCommitId
	 * </pre>
     * @param branch 分支名称（被删除的分支也支持）
     * @param startCommitId 获取日志的启始commit id(包含此commit)， null代表从头部开始
     * @param endCommitId 获取日志的结束commit id(包含此commit)， null代表不限制结束点
     * @return commitInfoList
     * @throws SVNOpsException
     */
    public List<CommitInfo> getBranchCommitListByCommitIdRange(String branch, String startCommitId, String endCommitId, int maxCount) throws SVNOpsException {
        List<CommitInfo> commitInfoList = new LinkedList<CommitInfo>();
        //如果分支存在，则直接从分支上去取commit列表
        if(hasBranch(branch)){
            commitInfoList = getCommitsContainStartEndForBranchByCommitIdRange(branch, startCommitId, endCommitId, maxCount, true);
        }
        else{
            //如果分支不存在，则从上层获取日志并用分支名称来过滤出这个被删除分支的commit列表
			commitInfoList = getBranchDeletedCommitsByCommitIdRange(branch, startCommitId, endCommitId, maxCount, true);
        }
        return commitInfoList;
    }

	/**
	 * checkout
	 * 当前startPath，配合locateToBranch和localteToTag使用，只有执行merge时才需要checkout，其他操作时直接连接远程仓库不需要checkout
	 *
	 * @throws SVNOpsException
	 */
	public void checkout() throws SVNOpsException {
		SVNURL effectiveUrl = null;

		// prepare a new folder for the cloned repository
		File localPath = new File(repoLocalDir);

		DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
		SVNClientManager ourClientManager = SVNClientManager.newInstance(options, authManager);

		SVNUpdateClient updateClient = ourClientManager.getUpdateClient();
		SVNWCClient wcClient = ourClientManager.getWCClient();

		try {
			effectiveUrl = repoUrl.appendPath(startPath, true);

			boolean ignoreExternals = false;
			updateClient.setIgnoreExternals(ignoreExternals);

			if (localPath.exists()) {
				wcClient.doCleanup(localPath, false, true, true, true, true, ignoreExternals);
				updateClient.doSwitch(localPath, effectiveUrl, SVNRevision.UNDEFINED, SVNRevision.HEAD, SVNDepth.getInfinityOrFilesDepth(true), false, false);
				// switch是update的超集
				// updateClient.doUpdate(localPath, SVNRevision.HEAD,
				// SVNDepth.fromRecurse(true), false, false);
			} else {
				updateClient.doCheckout(effectiveUrl, localPath, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.fromRecurse(true), false);
			}
		} catch (SVNException e) {
			throw new SVNOpsException("checkout svn url:" + remoteUrl + " failed, " + e.getMessage(), e);
		} finally {
			ourClientManager.dispose();
		}
	}

	/**
	 * 提交当前working的修改
	 *
	 * @param comment
	 * @return 提交的commit id
	 * @throws SVNOpsException
	 */
	public String commit(String comment) throws SVNOpsException {
		DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
		SVNClientManager ourClientManager = SVNClientManager.newInstance(options, authManager);
		try {
			File localPath = new File(repoLocalDir);
			
			SVNCommitClient commitClient = ourClientManager.getCommitClient();

			SVNCommitInfo commitInfo = commitClient.doCommit(new File[] { localPath }, false, comment, null, null, false, false, SVNDepth.INFINITY);

			String commitId = String.valueOf(commitInfo.getNewRevision());

			return commitId;
		} catch (SVNException e) {
			throw new SVNOpsException("commit " + repoLocalDir + " failed, " + e.getMessage(), e);
		} finally {
			ourClientManager.dispose();
		}

	}

	/**
	 * 使用svn命令检出分支， 只有执行merge时才需要checkout，其他操作时直接连接远程仓库不需要checkout。
	 * 如果 wc 已存在，就执行 cleanup + update 操作，否则执行 checkout 操作。
	 * @param branch
	 * @throws SVNOpsException
	 */
	public void checkoutCommand(String branch) throws SVNOpsException {
		File localPath = new File(repoLocalDir);
		SVNURL effectiveUrl = null;

		try {
			effectiveUrl = repoUrl.appendPath(getRealBranchPath(branch), true);

		} catch (Exception ex) {
			throw new SVNOpsException("use svn command checkout branch " + effectiveUrl.toDecodedString() + " failed", ex);
		}

		if (localPath.exists()) {
			long start = System.currentTimeMillis();
			logger.debug("command: svn clean up start...");
			JSONObject ret = WorkingCopyUtils.executeCommand(repoLocalDir, this.command.cleanup().end());
			logger.debug("svn cleanup end, time: " + (System.currentTimeMillis() - start) / 1000 + "s, ret: " + ret);
			
			start = System.currentTimeMillis();
			
			if (!StringUtils.equals("success", ret.getString("status"))) {
				throw new SVNOpsException("use svn command cleanup branch " + effectiveUrl.toDecodedString() + " failed, " + ret.getString("result"));
			}

			logger.debug("command: svn update start...");
			start = System.currentTimeMillis();
			ret = WorkingCopyUtils.executeCommand(repoLocalDir, this.command.update().setOptions(repoLocalDir).end());
			logger.debug("svn update end, time: " + (System.currentTimeMillis() - start) / 1000 + "s, ret: " + ret.toString());
			
			if (!StringUtils.equals("success", ret.getString("status"))) {
				throw new SVNOpsException("use svn command update branch " + effectiveUrl.toDecodedString() + " failed, " + ret.getString("result"));
			}
		} else {
			if (!localPath.mkdirs()) {
				throw new SVNOpsException("use svn command checkout branch " + effectiveUrl.toDecodedString() + " failed, can not create path " + repoLocalDir);
			}

			long start = System.currentTimeMillis();
			logger.debug("command: svn checkout start...");
			JSONObject ret = WorkingCopyUtils.executeCommand(repoLocalDir, this.command.checkout().setOptions(effectiveUrl.toString(), repoLocalDir).end());
			logger.debug("svn checkout end, time: " + (System.currentTimeMillis() - start) / 1000 + "s, ret: " + ret.toString());
			
			if (!StringUtils.equals("success", ret.getString("status"))) {
				throw new SVNOpsException("use svn command checkout branch " + effectiveUrl.toDecodedString() + " failed, " + ret.getString("result"));
			}
		}
	}

	/**
	 * 检出分支， 只有执行merge时才需要checkout，其他操作时直接连接远程仓库不需要checkout
	 *
	 * @param branch
	 * @throws SVNOpsException
	 */
	public void checkout(String branch) throws SVNOpsException {
		SVNURL effectiveUrl = null;

		// prepare a new folder for the cloned repository
		File localPath = new File(repoLocalDir);

		DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
		SVNClientManager ourClientManager = SVNClientManager.newInstance(options, authManager);

		SVNUpdateClient updateClient = ourClientManager.getUpdateClient();
		//SVNWCClient wcClient = ourClientManager.getWCClient();

		try {
			effectiveUrl = repoUrl.appendPath(getRealBranchPath(branch), true);

			boolean ignoreExternals = false;
			updateClient.setIgnoreExternals(ignoreExternals);
			// 处理目录存在, 但是.svn没有的情况, 需要使用checkout
			if (localPath.exists() && new File(localPath, ".svn").exists()) {
				long start = System.currentTimeMillis();
				
				// 【代码中心】SVN需求合并报错 https://www.tapd.cn/54247054/bugtrace/bugs/view?bug_id=1154247054001007995
				// 分拆 update 和 cleanup 的逻辑，后者很慢，根据需要决定 update 前要不要 cleanup，手动调用
				/*logger.debug("clean up start...");
				wcClient.doCleanup(localPath, false, true, true, true, true, ignoreExternals);
				logger.debug("svn cleanup end, time: " + (System.currentTimeMillis() - start) / 1000 + "s");
				start = System.currentTimeMillis();*/

				logger.info("update start...");
				start = System.currentTimeMillis();
				// updateClient.doSwitch(localPath, effectiveUrl, SVNRevision.UNDEFINED,
				// SVNRevision.HEAD, SVNDepth.getInfinityOrFilesDepth(true), false, false);
				updateClient.doUpdate(localPath, SVNRevision.HEAD, SVNDepth.getInfinityOrFilesDepth(true), false, false);
				logger.info("svn update end, time: " + (System.currentTimeMillis() - start) / 1000 + "s");
			} else {
				updateClient.doCheckout(effectiveUrl, localPath, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.fromRecurse(true), false);
			}
		} catch (SVNException e) {
			throw new SVNOpsException("checkout svn url:" + remoteUrl + " failed, " + e.getMessage(), e);
		} finally {
			ourClientManager.dispose();
		}
	}

	/**
	 * 获取远程仓库某个子目录下的文件
	 *
	 * @param subDir 基于仓库根的子目录路径
	 * @return
	 * @throws SVNOpsException
	 */
	private List<String> getFilesInDir(String subDir, String matcher) throws SVNOpsException {
		List<String> nameList = new ArrayList<String>();

		try {
			Collection<?> entries = repo.getDir(subDir, -1, null, (Collection<?>) null);
			Iterator<?> iterator = entries.iterator();

			while (iterator.hasNext()) {
				SVNDirEntry entry = (SVNDirEntry) iterator.next();
				if (entry.getKind().equals(SVNNodeKind.DIR)) {
					String branchName = entry.getName();
					if (matcher != null) {
						if (RegexUtils.wildcardMatch(matcher, branchName)) {
							nameList.add(entry.getName());
						}
					} else {
						nameList.add(entry.getName());
					}
				}
			}
		} catch (SVNException e) {
			throw new SVNOpsException("Get branche list for " + remoteUrl + " failed, " + e.getMessage(), e);
		}

		return nameList;
	}

	/**
	 * 获取某个子目录下的文件数量
	 *
	 * @param subDir 基于仓库根的子目录路径
	 * @return
	 * @throws SVNOpsException
	 */
	private int getFilesCountInDir(String subDir) throws SVNOpsException {
		int filesCount = 0;

		try {
			Collection<?> entries = repo.getDir(subDir, -1, null, (Collection<?>) null);

			filesCount = entries.size();
		} catch (SVNException e) {
			throw new SVNOpsException("Get branche list for " + remoteUrl + " failed, " + e.getMessage(), e);
		}

		return filesCount;
	}

	/**
	 * 某个目录parentDir下是否存在子目录childDir
	 *
	 * @param parentDir 基于仓库根的目录路径
	 * @param childDir  基于仓库根的目录路径
	 * @return
	 * @throws SVNOpsException
	 */
	private boolean hasFile(String parentDir, String childDir) throws SVNOpsException {
		String dir = "";
		if (startPath != null && !startPath.equals("")) {
			if (StringUtils.isNotEmpty(parentDir)) {
				dir = startPath + "/" + parentDir + "/" + childDir;
			} else {
				dir = startPath + "/" + childDir;
			}
		} else {
			if (StringUtils.isNotEmpty(parentDir)) {
				dir = parentDir + "/" + childDir;
			} else {
				dir = childDir;
			}
		}

		boolean isExists = false;
		try {
			//Collection<?> entries = repo.getDir(dir, -1, null, (Collection<?>) null);
			//if (entries.size() > 0) {
			//			isExists = true;
			//}
			
			repo.getDir(dir, -1, null, (Collection<?>) null);
			isExists = true;
		} catch (SVNException e) {
			isExists = false;
			//throw new SVNOpsException("Get branche list for " + remoteUrl + " failed, " + e.getMessage(), e);
		}
		
		return isExists;
	}

	/**
	 * 创建分支的基础函数，创建链接
	 *
	 * @param fromPath 源路径，基于仓库根的子目录路径
	 * @param toPath   目标路径，基于仓库根的子目录路径
	 * @throws SVNOpsException
	 */
	private void copyTo(String fromPath, String toPath) throws SVNOpsException {

		ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
		SVNClientManager ourClientManager = SVNClientManager.newInstance(options, authManager);
		try {
			SVNURL fromUrl = repoUrl.appendPath(fromPath, true);
			SVNURL toUrl = repoUrl.appendPath(toPath, true);

			SVNCopyClient copyClient = ourClientManager.getCopyClient();
			SVNCopySource copySource = new SVNCopySource(SVNRevision.UNDEFINED, SVNRevision.HEAD, fromUrl);
			copySource.setCopyContents(false);

			copyClient.doCopy(new SVNCopySource[] { copySource }, toUrl, false, // isMove
					true, // make parents
					true, // failWhenDstExists
					"Create branch " + toPath, // commit message
					null); // SVNProperties
		} catch (SVNException e) {
			throw new SVNOpsException("Create branch " + toPath + " from " + fromPath + " failed, " + e.getMessage(), e);
		}  finally {
			ourClientManager.dispose();
		}
	}

	/**
	 * 删除某个路径
	 *
	 * @param path 基于仓库根的子目录路径
	 * @throws SVNOpsException
	 */
	public void deleteFile(String path) throws SVNOpsException {
		if (startPath != null && !startPath.equals("")) {
			path = startPath + "/" + path;
		}

		final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();

		try {
			svnOperationFactory.setAuthenticationManager(authManager);

			SVNURL fileUrl = repoUrl.appendPath(path, true);
			final SvnRemoteDelete remoteDelete = svnOperationFactory.createRemoteDelete();

			remoteDelete.setSingleTarget(SvnTarget.fromURL(fileUrl));
			remoteDelete.setCommitMessage("Delete a file from the repository");
			remoteDelete.run();

			/*
			 * final SVNCommitInfo commitInfo = remoteDelete.run(); if (commitInfo != null)
			 * { final long newRevision = commitInfo.getNewRevision();
			 * logger.debug("Removed a file, revision " + newRevision + " created"); }
			 */
		} catch (SVNException e) {
			throw new SVNOpsException("delete dir " + path + " failed, " + e.getMessage(), e);
		} finally {
			svnOperationFactory.dispose();
		}
	}

	/**
	 * 获取所有的分支名称列表
	 *
	 * @return
	 * @throws SVNOpsException
	 */
	public List<String> getRemoteBrancheList() throws SVNOpsException {
		List<String> branchList = getFilesInDir(branchesPath, null);
		if (StringUtils.isNotEmpty(trunkPath) && !branchList.contains(trunkPath) && hasBranch(trunkPath)) {
			branchList.add(trunkPath);
		}
		return branchList;
	}

	/**
	 * 获取所有的分支名称列表
	 *
	 * @param matcher 通配符匹配，仅返回符合条件的branch
	 * @return
	 * @throws SVNOpsException
	 */
	public List<String> getRemoteBrancheList(String matcher) throws SVNOpsException {
		List<String> branchList = getFilesInDir(branchesPath, matcher);
		if (StringUtils.isNotEmpty(trunkPath) && !branchList.contains(trunkPath) && hasBranch(trunkPath)) {
			if (matcher == null || RegexUtils.wildcardMatch(matcher, trunkPath)) {
				branchList.add(trunkPath);
			}
		}
		return branchList;
	}

	/**
	 * 获取分支目录的最新 revision number
	 *
	 * @param branch branches目录的子目录、trunk目录或trunk目录的子目录
	 * @return
	 * @throws SVNOpsException
	 */
	public long resolveBranch(String branch) throws SVNOpsException {
		String branchDir;

		if (startPath != null && !startPath.equals("")) {
			branchDir = startPath + "/" + branch;
		} else {
			if (StringUtils.isNotEmpty(trunkPath) && (trunkPath.equals(branch) || StringUtils.startsWithIgnoreCase(branch, trunkPath + "/"))) {
				branchDir = branch;
			} else {
				branchDir = branchesPath + "/" + branch;
			}
		}

		try {
			SVNDirEntry entry = repo.info(branchDir, -1);
			if (entry != null) {
				return entry.getRevision();
			} else {
				return -1;
			}
		} catch (SVNException e) {
			throw new SVNOpsException("Resolve branch " + branchDir + " for " + remoteUrl + " failed, " + e.getMessage(), e);
		}
	}

	/**
	 * 是否存在分支
	 *
	 * @param branch 分支名称
	 * @return
	 * @throws SVNOpsException
	 */
	public boolean hasBranch(String branch) throws SVNOpsException {
		if (StringUtils.isNotEmpty(trunkPath) && trunkPath.equals(branch)) {
			return hasFile(trunkPath, "");
		}
		return hasFile(branchesPath, branch);
	}

	/**
	 * 获取所有分支的数量
	 *
	 * @return
	 * @throws SVNOpsException
	 */
	public int getBranchCount() throws SVNOpsException {
		return getFilesCountInDir(branchesPath);
	}

	/**
	 * 创建分支
	 *
	 * @param branchName      分支名称
	 * @param startBranchName 源头分支名称，如果null或者是“trunk”，或者“master”则代表主干
	 * @throws SVNOpsException
	 */
	public void createBranch(String branchName, String startBranchName) throws SVNOpsException {
		String fromPath = getRealBranchPath(startBranchName);
		String toPath = getRealBranchPath(branchName);

		copyTo(fromPath, toPath);

		return;
	}

	/**
	 * 删除分支
	 *
	 * @param branchName 分支名称
	 * @throws SVNOpsException
	 */
	public void deleteBranch(String branchName) throws SVNOpsException {
		deleteFile(branchesPath + "/" + branchName);
	}

	/**
	 * 获取所有标签名称列表
	 *
	 * @return
	 * @throws SVNOpsException
	 */
	public List<String> getRemoteTagList() throws SVNOpsException {
		return getFilesInDir(tagsPath, null);
	}

	/**
	 * 获取所有标签名称列表
	 *
	 * @param matcher 通配符匹配，仅返回符合条件的tag
	 * @return
	 * @throws SVNOpsException
	 */
	public List<String> getRemoteTagList(String matcher) throws SVNOpsException {
		return getFilesInDir(tagsPath, matcher);
	}

	/**
	 * 获取tag目录的最新 revision number
	 *
	 * @param tag 相对于tags目录的子目录，tags目录为空则相对仓库根目录
	 * @return
	 * @throws SVNOpsException
	 */
	public long resolveTag(String tag) throws SVNOpsException {
		String parentDir = tagsPath;
		if (startPath != null && !startPath.equals("")) {
			parentDir = startPath + "/" + tagsPath;
		}

		String tagDir = parentDir + "/" + tag;
		try {
			SVNDirEntry entry = repo.info(tagDir, -1);
			if (entry != null) {
				return entry.getRevision();
			} else {
				return -1;
			}
		} catch (SVNException e) {
			throw new SVNOpsException("Resolve tag " + tagDir + " for " + remoteUrl + " failed, " + e.getMessage(), e);
		}
	}

	/**
	 * 获取指定目录的最新 revision number
	 *
	 * @param path 则相对仓库根目录的路径
	 * @return
	 * @throws SVNOpsException
	 */
	public long resolvePath(String path) throws SVNOpsException {
		if (StringUtils.isBlank(path)) {
			return -1;
		}

		try {
			SVNDirEntry entry = repo.info(path, -1);
			if (entry != null) {
				return entry.getRevision();
			} else {
				return -1;
			}
		} catch (SVNException e) {
			throw new SVNOpsException("Resolve path " + path + " for " + remoteUrl + " failed, " + e.getMessage(), e);
		}
	}

	/**
	 * 是否存在标签
	 *
	 * @param tagName 标签名称
	 * @return
	 * @throws SVNOpsException
	 */
	public boolean hasTag(String tagName) throws SVNOpsException {
		return hasFile(tagsPath, tagName);
	}

	/**
	 * 获取所有标签数量
	 *
	 * @return
	 * @throws SVNOpsException
	 */
	public int getTagsCount() throws SVNOpsException {
		return getFilesCountInDir(tagsPath);
	}

	/**
	 * 创建标签
	 *
	 * @param tagName    标签名称
	 * @param branchName 源头分支名称，如果null或者是“trunk”，或者“master”则代表主干
	 * @throws SVNOpsException
	 */
	public void createTag(String tagName, String branchName) throws SVNOpsException {
		String fromPath = getRealBranchPath(branchName);
		String toPath = tagsPath + "/" + tagName;

		copyTo(fromPath, toPath);

		return;
	}

	/**
	 * 删除标签
	 *
	 * @param branchName
	 * @throws SVNOpsException
	 */
	public void deleteTag(String branchName) throws SVNOpsException {
		deleteFile(tagsPath + "/" + branchName);
	}

	/**
	 * 获取某个文件路径某个commit的修改信息，仅统计inserted和deleted的行数，不记录详细的修改行
	 *
	 * @param startPath 启始路径，对应于tag后者branch，trunk的子路径
	 * @param rev       revision号
	 * @return 返回DiffInfo列表，对应于commit中所有文件的修改信息，但不包括空目录
	 * @throws SVNException
	 */
	public List<FileDiffInfo> getChangeInfo(String startPath, long rev) throws SVNException {
		SVNURL effectiveUrl;
		if (startPath != null && !startPath.equals("")) {
			effectiveUrl = repoUrl.appendPath(startPath, true);
		} else {
			effectiveUrl = repoUrl;
		}

		ByteArrayOutputStream out = null;
		ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
		SVNClientManager ourClientManager = SVNClientManager.newInstance(options, authManager);

		SVNDiffClient diffClient = ourClientManager.getDiffClient();
		
		List<FileDiffInfo> diffInfoList;
		try {
			out = new ByteArrayOutputStream();
			
			diffClient.setGitDiffFormat(true);
			
			// fix bug: 这里rev写反了, 先小后大
			diffClient.doDiff(effectiveUrl, SVNRevision.create(rev - 1), effectiveUrl, SVNRevision.create(rev), SVNDepth.INFINITY, true, out);
			
			diffInfoList = FileDiffInfo.parseSvnDiffLog(out, true);
			if (diffInfoList == null) {
				diffInfoList = new LinkedList<FileDiffInfo>();
			}
		} finally {
			ourClientManager.dispose();
		}
		
		return diffInfoList;
	}

	/**
	 * 获取某个文件路径某个commit的修改信息，仅统计inserted和deleted的行数，不记录详细的修改行
	 *
	 * @param startPath 启始路径，对应于tag后者branch，trunk的子路径
	 * @param filePath  文件相对于仓库的相对路径, 譬如：branches/1.0.0/src/java/test/test.java
	 * @param rev       revision号
	 * @return 返回单个DiffInfo实例，对应于单个文件或空目录的修改信息
	 * @throws SVNException
	 */
	public FileDiffInfo getChangeInfo(String startPath, String filePath, long rev) throws SVNException {
		if (startPath != null && !startPath.equals("")) {
			filePath = startPath + "/" + filePath;
		}

		ByteArrayOutputStream out = null;
		SVNURL fileUrl = repoUrl.appendPath(filePath, false);
		ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
		SVNClientManager ourClientManager = SVNClientManager.newInstance(options, authManager);
		
		FileDiffInfo diffInfo;
		try {
			SVNDiffClient diffClient = ourClientManager.getDiffClient();
			
			out = new ByteArrayOutputStream();
			
			diffClient.setGitDiffFormat(true);
			diffClient.doDiff(fileUrl, SVNRevision.create(rev - 1), fileUrl, SVNRevision.create(rev), SVNDepth.INFINITY, true, out);
			
			diffInfo = FileDiffInfo.parseSvnSingleDiffLog(out, true);
			if (diffInfo == null) {
				diffInfo = new FileDiffInfo();
			}
		} finally {
			ourClientManager.dispose();
		}
		return diffInfo;
	}

	/**
	 * 获取某个commit下的所有修改文件的修改信息，记录详细的修改行以及其上下若干行的内容
	 *
	 * @param startPath 启始路径，对应于tag后者branch，trunk的子路径
	 * @param rev       revision号
	 * @param maxChangeCount       diff中最大的增删行量, 如果设置-1则表示不过滤折叠, 将全部diff信息获取到, 否则只会取头部基本数据
	 * @return 返回DiffInfo列表，对应于commit中所有文件的修改信息，但不包括空目录
	 * @throws SVNException
	 */
	public List<FileDiffInfo> getDiffInfo(String startPath, long rev, int maxChangeCount) throws SVNException {
		return getDiffInfo(startPath, rev - 1, rev, maxChangeCount);
	}
	
	/**
	 * 获取两个commit的文件修改信息，记录详细的修改行以及其上下若干行的内容
	 *
	 * @param startPath 启始路径，对应于tag后者branch，trunk的子路径
	 * @param rN       rN
	 * @param rM       rM
	 * @param maxChangeCount       diff中最大的增删行量, 如果设置-1则表示不过滤折叠, 将全部diff信息获取到, 否则只会取头部基本数据
	 * @return 返回DiffInfo列表，对应于commit中所有文件的修改信息，但不包括空目录
	 * @throws SVNException
	 */
	public List<FileDiffInfo> getDiffInfo(String startPath, long rN, long rM, int maxChangeCount) throws SVNException {
		SVNURL effectiveUrl;
		if (startPath != null && !startPath.equals("")) {
			effectiveUrl = repoUrl.appendPath(startPath, true);
		} else {
			effectiveUrl = repoUrl;
		}

		ByteArrayOutputStream out = null;

		ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
		SVNClientManager ourClientManager = SVNClientManager.newInstance(options, authManager);
		
		List<FileDiffInfo> diffInfoList;
		try {
			SVNDiffClient diffClient = ourClientManager.getDiffClient();
			
			out = new ByteArrayOutputStream();
			
			diffClient.setGitDiffFormat(true);
			diffClient.doDiff(effectiveUrl, SVNRevision.create(rN), effectiveUrl, SVNRevision.create(rM), SVNDepth.INFINITY, true, out);
			
			
			diffInfoList = FileDiffInfo.parseSvnDiffLog(out, false, "", maxChangeCount);
			if (diffInfoList == null) {
				diffInfoList = new LinkedList<FileDiffInfo>();
			} else {
				// 当指定filePath时候, 返回来的from to Path中只有文件名, 没有文件路径
				if (StringUtils.isNotBlank(startPath)) {
					String fromFileName = diffInfoList.get(0).getFromFileName();
					String toFileName = diffInfoList.get(0).getToFileName();
					if (StringUtils.endsWith(startPath, fromFileName) && !fromFileName.equals("/dev/null")) {
						diffInfoList.get(0).setFromFileName(startPath);
					}
					if (StringUtils.endsWith(startPath, toFileName) && !toFileName.equals("/dev/null")) {
						diffInfoList.get(0).setToFileName(startPath);
					}
				}
			}
		} finally {
			ourClientManager.dispose();
		}
		return diffInfoList;
	}

	/**
	 * 比较任何两个分支或者子目录，譬如“branches/1.0.0", "trunk"; "branches/2.0.0", "tags/1.0.0"
	 *
	 * @param newPath
	 * @param oldPath
	 * @param filePath 指定的文件diff, 不传默认取所有文件的diff
	 * @param maxChangeCount 最大获取的changge数量限制, -1表示没有限制, 0表示只取diff基本数据
	 * @return
	 * @throws SVNException
	 */
	public List<FileDiffInfo> getDiffInfo(String newPath, String oldPath, String filePath, int maxChangeCount) throws SVNException {

		ByteArrayOutputStream out = null;

		ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
		SVNClientManager ourClientManager = SVNClientManager.newInstance(options, authManager);
		
		List<FileDiffInfo> diffInfoList;
		try {
			SVNDiffClient diffClient = ourClientManager.getDiffClient();
			
			out = new ByteArrayOutputStream();
			diffClient.setGitDiffFormat(true);
			diffClient.doDiff(repoUrl.appendPath(oldPath, true), SVNRevision.HEAD, repoUrl.appendPath(newPath, true), SVNRevision.HEAD, SVNDepth.INFINITY, true, out);
			
			diffInfoList = FileDiffInfo.parseSvnDiffLog(out, false, filePath, maxChangeCount);
			if (diffInfoList == null) {
				diffInfoList = new LinkedList<FileDiffInfo>();
			}
		} finally {
			ourClientManager.dispose();
		}
		
		// TODO !!GIT的有根据路径过滤diff的方法, 而svn这里暂时没发现有类似功能, 可能得在FileDiffInfo中过滤路径
		
		return diffInfoList;
	}

	/**
	 * 获取某个文件路径某个commit的修改信息，记录详细的修改行以及其上下若干行的内容
	 *
	 * @param startPath 启始路径，对应于tag后者branch，trunk的子路径
	 * @param filePath  文件相对于仓库的相对路径, 譬如：branches/1.0.0/src/java/test/test.java
	 * @param rev       revision号
	 * @return 返回单个DiffInfo实例，对应于单个文件或空目录的修改信息
	 * @throws SVNException
	 */
	public FileDiffInfo getDiffInfo(String startPath, String filePath, long rev) throws SVNException {
		if (startPath != null && !startPath.equals("")) {
			filePath = startPath + "/" + filePath;
		}

		ByteArrayOutputStream out = null;
		SVNURL fileUrl = repoUrl.appendPath(filePath, false);
		ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
		SVNClientManager ourClientManager = SVNClientManager.newInstance(options, authManager);
		FileDiffInfo diffInfo;
		try {
			SVNDiffClient diffClient = ourClientManager.getDiffClient();
			
			out = new ByteArrayOutputStream();
			
			diffClient.setGitDiffFormat(true);
			diffClient.doDiff(fileUrl, SVNRevision.create(rev - 1), fileUrl, SVNRevision.create(rev), SVNDepth.INFINITY, true, out);
			
			diffInfo = FileDiffInfo.parseSvnSingleDiffLog(out, false);
			if (diffInfo == null) {
				diffInfo = new FileDiffInfo();
			}
		} finally {
			ourClientManager.dispose();
		}
		return diffInfo;
	}

	/**
	 * 下载文件
	 *
	 * @param rev      revision号
	 * @param filePath 文件路径（相对于仓库根）譬如：branches/1.0.0/src/java/test/test.java
	 * @param out      文件数据输出的流
	 * @throws SVNOpsException
	 */
	public void downloadFile(long rev, String filePath, OutputStream out) throws SVNOpsException {
		try {
			if (startPath != null && !startPath.equals("")) {
				filePath = startPath + "/" + filePath;
			}

			SVNNodeKind nodeKind = repo.checkPath(filePath, rev);

			if (nodeKind == SVNNodeKind.NONE) {
				throw new SVNOpsException("File " + filePath + " not found");
			} else if (nodeKind == SVNNodeKind.DIR) {
				throw new SVNOpsException("File " + filePath + " is directory");
			}

			repo.getFile(filePath, rev, null, out);
		} catch (SVNException e) {
			throw new SVNOpsException("Download " + filePath + " failed, " + e.getMessage(), e);
		}
	}

	/**
	 * 获取文件内容，最大不超过2M
	 *
	 * @param rev      revision number
	 * @param filePath 相对于仓库根的路径，譬如：branches/1.0.0/src/java/test/test.java
	 * @return
	 * @throws SVNOpsException
	 */
	public String getFileContent(long rev, String filePath, int maxSize) throws SVNOpsException {
		try {
			if (startPath != null && !startPath.equals("")) {
				filePath = startPath + "/" + filePath;
			}

			SVNDirEntry fileInfo = repo.info(filePath, rev);

			SVNNodeKind nodeKind = fileInfo.getKind();
			if (nodeKind == SVNNodeKind.NONE) {
				throw new SVNOpsException("File " + filePath + " not found");
			} else if (nodeKind == SVNNodeKind.DIR) {
				throw new SVNOpsException("File " + filePath + " is directory");
			}

			if (maxSize <= 0) {
				maxSize = 2 * 1024 * 1024;
			}

			if (fileInfo.getSize() > maxSize) {
				throw new SVNOpsException(filePath + " is bigger than " + maxSize + " bytes");
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			repo.getFile(filePath, rev, null, out);

			return out.toString();
		} catch (SVNException e) {
			throw new SVNOpsException("Download " + filePath + " failed, " + e.getMessage(), e);
		}
	}

	/**
	 * 按行获取文件内容
	 * @param rev      revision number
	 * @param filePath 相对于仓库根的路径，譬如：branches/1.0.0/src/java/test/test.java
     * @param startLine 开始行号
     * @param lineCount 行数，如果是正向获取则为正数，如果是负向获取则为负数
	 * @return
	 * @throws SVNOpsException
	 */
	public List<String> getFileLines(long rev, String filePath, int startLine, int lineCount) throws SVNOpsException, IOException {
		List<String> content = new ArrayList<String>();
		BufferedReader reader = null;
		ByteArrayOutputStream out = null;
		ByteArrayInputStream in = null;
		
		try {
			if (startPath != null && !startPath.equals("")) {
				filePath = startPath + "/" + filePath;
			}

			SVNDirEntry fileInfo = repo.info(filePath, rev);

			if (fileInfo == null) {
				throw new SVNOpsException("File " + filePath + " not found");
			}
			
			SVNNodeKind nodeKind = fileInfo.getKind();
			if (nodeKind == SVNNodeKind.NONE) {
				throw new SVNOpsException("File " + filePath + " not found");
			} else if (nodeKind == SVNNodeKind.DIR) {
				throw new SVNOpsException("File " + filePath + " is directory");
			}

			out = new ByteArrayOutputStream();
			SVNProperties prop = new SVNProperties();
			
			repo.getFile(filePath, rev, prop, out);
			
			String mimeType = prop.getStringValue(SVNProperty.MIME_TYPE);
			boolean isTextType = SVNProperty.isTextMimeType(mimeType);

			int start = 0;
			int end = 0;

			if(lineCount > 0) {
				start = startLine;
				end = startLine + lineCount;
			}else {
				start = startLine + lineCount;
				end = startLine;
			}

			if(start < 1 ) {
				start = 1;
			}

			in = new ByteArrayInputStream(out.toByteArray(), 0, out.toByteArray().length);
			reader = new BufferedReader(new InputStreamReader(in));

			String line = reader.readLine();


			if (!isTextType && !isText(line)) {
				// 读取第一行进行判断, 确保是真的不是文本文件
				throw new SVNOpsException("File " + filePath + " is not text file.");
			}

			if ( start == 1 && line != null) {
				content.add(line);
				start = start+1;
			}

			//略过不需要的行
			for (int i = 2; i < start && line != null; i++) {
				line = reader.readLine();
			}

			for (int i = start; i < end && (line = reader.readLine())!= null; i++) {
				content.add(line);
			}

		} catch (SVNException e) {
			throw new SVNOpsException("Download " + filePath + " failed, " + e.getMessage(), e);
		}finally {
			if(reader != null) {
				reader.close();
			}
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}
		}
		
		return content;
	}

	public boolean isText(String str) {
		if (str == null) {
			return true;
		}
		int sz = str.length();
		for (int i = 0; i < sz; i++) {
			char c = (str.charAt(i));
			if (CharUtils.isAsciiControl(c) && c != '\n' && c != '\r') {
				return false;
			}
		}
		return true;
	}

	/**
	 * 获取某个目录下的所有文件和子目录
	 *
	 * @param rev  revision number
	 * @param path 相对于仓库根的路径，譬如：branches/1.0.0/src/java/test/test.java
	 * @return
	 * @throws SVNOpsException
	 */
	public List<FileInfo> listFolder(long rev, String path) throws SVNOpsException {
		return listFolder(rev, path, -1, -1);
	}

	/**
	 * 获取某个目录下的所有文件和子目录(支持分页)
	 *
	 * @param rev        revision number
	 * @param path       相对于仓库根的路径，譬如：branches/1.0.0/src/java/test/test.java
	 * @param skipCount  略过的数量，当skipCount=0时，代表不略过
	 * @param limitCount 查询的最大数量，当limitCount=0是，代表查询全部
	 * @author fengt
	 * @throws SVNOpsException
	 */
	public List<FileInfo> listFolder(long rev, String path, int skipCount, int limitCount) throws SVNOpsException {
		List<FileInfo> fileInfoList = new LinkedList<FileInfo>();

		try {
			if (startPath != null && !startPath.equals("")) {
				path = startPath + "/" + path;
			}

			if (path.startsWith("/")) {
				path = path.substring(1);
			}
			if (path.endsWith("/")) {
				path = path.substring(0, path.length() - 1);
			}

			int count = 0;
			if (skipCount < 0) {
				skipCount = 0;
			}

			Collection<?> entries = repo.getDir(path, rev, null, (Collection<?>) null);
			int entryCount = entries.size();
			if (entryCount > 0) {
				Iterator<?> iterator = entries.iterator();
				while (iterator.hasNext()) {
					SVNDirEntry dirEntry = (SVNDirEntry) iterator.next();
					String aPath = dirEntry.getName();
					FileInfo fileInfo = null;
					SVNNodeKind nodeKind = dirEntry.getKind();
					if (nodeKind == SVNNodeKind.DIR) {
						fileInfo = new FileInfo(aPath, 'D');
					} else if (nodeKind == SVNNodeKind.FILE) {
						fileInfo = new FileInfo(aPath, 'F');
					}

					if (fileInfo != null) {
						count++;
						if (limitCount > 0) {
							int maxCount = skipCount + limitCount;
							if (count <= skipCount) {
								continue;
							}
							if (count > maxCount) {
								break;
							}
						}
						fileInfo.setLastChangeDate(dirEntry.getDate());
						fileInfo.setLastAuthor(dirEntry.getAuthor());
						String commentMsg = dirEntry.getCommitMessage();
						if (commentMsg != null) {
							commentMsg = commentMsg.substring(0, commentMsg.indexOf("\n"));
							if (commentMsg.length() > 128) {
								commentMsg.substring(0, 128);
							}
							fileInfo.setLastCommitMessage(commentMsg);
						}
						SVNLock svnLock = dirEntry.getLock();
						if (svnLock != null) {
							fileInfo.setLock(true);
							fileInfo.setLockOwner(svnLock.getOwner());
						}

						// 注意：不能使用 dirEntry.getRevision()得到的revision取文件内容，如果是整个目录 copy from 其他目录，或者目录被重命名过，
						// getRevision 拿到的是原始目录下的 revision。但是原始目录可能已经不存在，用这个revision取文件内容就会404
						fileInfo.setLastCommitId(String.valueOf(dirEntry.getRevision()));
						fileInfoList.add(fileInfo);
					}
				}
			}
			return fileInfoList;
		} catch (SVNException e) {
			throw new SVNOpsException("list " + path + " failed, " + e.getMessage(), e);
		}
	}

	/**
	 * 内部函数，用于递归遍历目录并zip到zip输出流中
	 *
	 * @param rev  revision number
	 * @param path 相对于仓库根的子路径，譬如：branches/1.0.0/src
	 * @param zos
	 * @throws SVNOpsException
	 */
	private void zipFolderInRepo(long rev, String path, int startPathLen, ZipOutputStream zos) throws SVNOpsException {
		try {
			Collection<?> entries = repo.getDir(path, rev, null, (Collection<?>) null);
			int entryCount = entries.size();
			if (entryCount > 0) {
				Iterator<?> iterator = entries.iterator();
				while (iterator.hasNext()) {
					SVNDirEntry svnDirEntry = (SVNDirEntry) iterator.next();
					String aPath = (path.equals("") ? svnDirEntry.getName() : path + "/" + svnDirEntry.getName());

					SVNNodeKind nodeKind = svnDirEntry.getKind();
					if (nodeKind == SVNNodeKind.DIR) {
						zipFolderInRepo(rev, aPath, startPathLen, zos);
					} else if (nodeKind == SVNNodeKind.FILE) {
						ZipEntry ze = new ZipEntry(aPath.substring(startPathLen));
						zos.putNextEntry(ze);
						repo.getFile(aPath, rev, null, zos);
						zos.closeEntry();
					}
				}
			} else {
				ZipEntry ze = new ZipEntry(path);
				zos.putNextEntry(ze);
				zos.closeEntry();
			}
		} catch (SVNException | IOException e) {
			throw new SVNOpsException("Download " + path + " failed, " + e.getMessage(), e);
		}
	}

	/**
	 * zip格式打包下载某个子目录到输出流中
	 *
	 * @param rev  revision number
	 * @param path 相对于仓库根的子路径，譬如：branches/1.0.0/src
	 * @param out
	 * @throws SVNOpsException
	 */
	public void downloadDirArchive(long rev, String path, OutputStream out) throws SVNOpsException {
		ZipOutputStream zos = null;
		try {
			if (startPath != null && !startPath.equals("")) {
				path = startPath + "/" + path;
			}

			if (path.startsWith("/")) {
				path = path.substring(1);
			}

			int startPathLen = path.length();
			if (path.endsWith("/")) {
				path = path.substring(0, startPathLen - 1);
			} else {
				startPathLen = startPathLen + 1;
			}

			zos = new ZipOutputStream(out);
			zipFolderInRepo(rev, path, startPathLen, zos);
		} finally {
			if (zos != null) {
				try {
					zos.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * 获取working copy的当前状态，列出文件修改和冲突的清单
	 *
	 * @return
	 * @throws
	 */
	public MergeResultInfo getStatus(String branch) throws SVNOpsException {
		DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
		SVNClientManager ourClientManager = SVNClientManager.newInstance(options, authManager);
		
		MergeResultInfo mergeResultInfo;
		try {
			WCStatusHandler handler = new WCStatusHandler();
			
			File localPath = new File(repoLocalDir);
			try {
				ourClientManager.getStatusClient().doStatus(localPath, SVNRevision.HEAD, SVNDepth.fromRecurse(true), false, false, false, false, handler, null);
			} catch (SVNException e) {
				throw new SVNOpsException("Get status for branch " + branch + " for " + remoteUrl + " failed, " + e.getMessage(), e);
			}
			
			mergeResultInfo = new MergeResultInfo();
			mergeResultInfo.setMergeFileEntrys(handler.getMergeEntryList());
			mergeResultInfo.setConflict(handler.isHasConflict());
		} finally {
			ourClientManager.dispose();
		}
		
		return mergeResultInfo;
	}

	/**
	 * 合并某个分支到当前working copy所在分支，合并前需要确定当前working
	 * copy处在正确的分支，合并前需要进行分支checkout和reset，合并后需要另外主动进行commit和push
	 *
	 * @param srcBranch   合并的源Branch
	 * @param targetBranch 目标分支branch
	 * @param dryRun       是否检测运行
	 * @return
	 * @throws SVNOpsException
	 */
	public MergeResultInfo BranchMerge(String srcBranch, String targetBranch, Boolean dryRun) throws SVNOpsException {
		
		DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
		SVNClientManager ourClientManager = SVNClientManager.newInstance(options, authManager);
		// XXXClient classes should be always constructed via SVNClientManager, it would allow to avoid such problems.

		try {
			if (srcBranch == null) {
				srcBranch = this.trunkPath;
			}
			if (targetBranch == null) {
				targetBranch = this.trunkPath;
			}

			SVNURL targetUrl = repoUrl.appendPath(getRealBranchPath(targetBranch), true);
			SVNURL srcUrl = repoUrl.appendPath(getRealBranchPath(srcBranch), true);

			ISVNConflictHandler callback = new MergeConflictHandler();
			options.setConflictHandler(callback);
			
			SVNDiffClient diffClient = ourClientManager.getDiffClient();
			diffClient.setIgnoreExternals(false);
			MergeEventHandler mergeEventHandler = new MergeEventHandler(repoLocalDir);

			// 修复这块的无法识别冲突的bug, mergeEventHandler应该传入变量, 不能是new出来的
			diffClient.setEventHandler(mergeEventHandler);

			File repoLocalFile = new File(repoLocalDir);

			// 将 svn diff target_branch src_branch 的结果应用到 working copy
//			diffClient.doMerge(targetUrl, SVNRevision.HEAD, srcUrl, SVNRevision.HEAD, repoLocalFile, SVNDepth.INFINITY, true, false, dryRun, false);
			//diffClient.doMergeReIntegrate(srcUrl, SVNRevision.HEAD, repoLocalFile, dryRun);
			
			SVNCommand svnCommand = this.command.merge().setOptions("--config-dir", SVNWCUtil.getDefaultConfigurationDirectory().toString(),
							targetUrl.toString() , srcUrl.toString(), repoLocalDir);
			if (dryRun) {
				svnCommand.setOptions("--dry-run");
			}
			String[] cmd = svnCommand.end();
			
			String cmdStr = StringUtils.join(cmd, " ").replaceFirst("--password\\s+.+?\\s+", "");
			logger.info("svn branch merge: {}",  cmdStr);
			
			JSONObject ret = WorkingCopyUtils.executeCommand(repoLocalDir, cmd);
			logger.info(ret.getString("result"));
			
			String data = ret.getString("result");
			MergeResultInfo mergeResultInfo = new SVNMergeParser().parse(data);
			if (!mergeResultInfo.isConflict()) {
				if (StringUtils.isNotBlank(mergeResultInfo.getError())) {
					throw new SVNOpsException(mergeResultInfo.getError());
				}
				if (!StringUtils.equals("success", ret.getString("status"))) {
					throw new SVNOpsException("execute '" +  cmdStr + "' command failed," + ret.getString("result"));
				}
			}
			
//			MergeResultInfo mergeResultInfo = new MergeResultInfo();
//			mergeResultInfo.setMergeFileEntrys(mergeEventHandler.getMergeEntryList());
//			mergeResultInfo.setConflict(mergeEventHandler.isHasConflict());

			return mergeResultInfo;
		} catch (SVNException e) {
			throw new SVNOpsException("Merge failed " + e.getMessage(), e);
		} finally {
			ourClientManager.dispose();
		}
	}

	/**
	 * 合并或回退某个commit到当前working copy所在分支，合并前需要确定当前working
	 * copy处在正确的分支，合并前需要进行分支checkout和reset，合并后需要另外主动进行commit和push
	 *
	 * @param srcBranch   合并的源Branch
	 * @param targetBranch 合并的目标branch
	 * @param revisions    多个commit号
	 * @param isRevert     true：代表回退commit， false：代表合入commit
	 * @param dryRun       是否检测运行
	 * @return
	 * @throws SVNOpsException
	 */
	public MergeResultInfo CherryPick(String srcBranch, String targetBranch, String[] revisions, Boolean isRevert, Boolean dryRun) throws SVNOpsException {
		// E155035: Attempt to add tree conflict that already exists at
		// 目录不对导致的错误，一般错误指向了父目录导致的错误
		
		
		DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
		SVNClientManager ourClientManager = SVNClientManager.newInstance(options, authManager);
		// XXXClient classes should be always constructed via SVNClientManager, it would allow to avoid such problems.

		try {
			if (srcBranch == null) {
				srcBranch = this.trunkPath;
			}
			if (targetBranch == null) {
				targetBranch = this.trunkPath;
			}

			//SVNURL targetUrl = repoUrl.appendPath(getRealBranchPath(targetBranch), true);
			SVNURL srcUrl = repoUrl.appendPath(getRealBranchPath(srcBranch), true);

			ISVNConflictHandler callback = new MergeConflictHandler();

			options.setConflictHandler(callback);
			SVNDiffClient diffClient = ourClientManager.getDiffClient();
			diffClient.setIgnoreExternals(false);
			MergeEventHandler mergeEventHandler = new MergeEventHandler(repoLocalDir);

			// 修复这块的无法识别冲突的bug, mergeEventHandler应该传入变量, 不能是new出来的
			diffClient.setEventHandler(mergeEventHandler);

			Collection<SVNRevisionRange> rangesToMerge = new ArrayList<SVNRevisionRange>();

			// 把revision号从字串数组转换为long数组，并排序
			int revisionsCount = revisions.length;
			Long[] revs = new Long[revisionsCount];
			
			for (int i = 0; i < revisionsCount; i++) {
				revs[i] = Long.parseLong(revisions[i]);
			}
			
			List<Long[]> revsionRangeList = getRevsionRange(revs, isRevert);
			String rangesToMergeString = "";
			for (Long[] revsionRange: revsionRangeList) {
				rangesToMergeString = String.format("%s %s:%s ", rangesToMergeString, revsionRange[0], revsionRange[1]);
				
				SVNRevisionRange range = new SVNRevisionRange(SVNRevision.create(revsionRange[0]), SVNRevision.create(revsionRange[1]));
				rangesToMerge.add(range);
			}

			logger.info("svn {} start, revision range: {}...", isRevert ? "revert" : "merge", rangesToMergeString);
			
			long startTime = System.currentTimeMillis();
			
//			File repoLocalFile = new File(repoLocalDir);
			
//			diffClient.doMerge(srcUrl, SVNRevision.HEAD, rangesToMerge, repoLocalFile, SVNDepth.INFINITY, true, false, dryRun, false);
			
			SVNCommand svnCommand = this.command.merge().setOptions("--config-dir", SVNWCUtil.getDefaultConfigurationDirectory().toString(),
							srcUrl.toString() , repoLocalDir);
			for (Long[] revisionRange: revsionRangeList) {
				rangesToMergeString = String.format("%s:%s", revisionRange[0], revisionRange[1]);
				svnCommand.setOptions("-r", rangesToMergeString);
			}
			if (dryRun) {
				svnCommand.setOptions("--dry-run");
			}
			String[] cmd = svnCommand.end();
			
			String cmdStr = StringUtils.join(cmd, " ").replaceFirst("--password\\s+.+?\\s+", "");
			logger.info("svn cherry pick merge: {}",  cmdStr);
			
			JSONObject ret = WorkingCopyUtils.executeCommand(repoLocalDir, cmd);
			logger.info(ret.getString("result"));

			
			String data = ret.getString("result");
			MergeResultInfo mergeResultInfo = new SVNMergeParser().parse(data);
			if (!mergeResultInfo.isConflict()) {
				if (StringUtils.isNotBlank(mergeResultInfo.getError())) {
					throw new SVNOpsException(mergeResultInfo.getError());
				}
				if (!StringUtils.equals("success", ret.getString("status"))) {
					throw new SVNOpsException("execute '" +  cmdStr + "' command failed," + ret.getString("result"));
				}
			}
			
			
//			MergeResultInfo mergeResultInfo = new MergeResultInfo();
//			mergeResultInfo.setMergeFileEntrys(mergeEventHandler.getMergeEntryList());
//			mergeResultInfo.setConflict(mergeEventHandler.isHasConflict());

			// diffClient.doMerge 执行撤销时有 bug, 可能会导致mergeinfo没有修改, 所以用命令方式修改mergeinfo属性。 see https://www.tapd.cn/54247054/s/1137999
//			if (isRevert && !mergeResultInfo.isConflict()) {
//				String[] cmd = this.command.merge()
//						.setOptions("--config-dir", FileUtils.getUserDirectoryPath() + "/.subversion", "--record-only", "-c", "-" + StringUtils.join(revisions, ",-"), srcUrl.toString(), repoLocalDir)
//						.end();
//				
//				String cmdStr = StringUtils.join(cmd, " ").replaceFirst("--password\\s+.+?\\s+", "");
//				logger.info("svn revert revision range {} success, execute svn command '{}'...", rangesToMergeString, cmdStr);
//				
//				JSONObject ret = WorkingCopyUtils.executeCommand(repoLocalDir, cmd);
//				if (!StringUtils.equals("success", ret.optString("status"))) {
//					throw new SVNOpsException("execute '" +  cmdStr + "' command failed," + ret.optString("result"));
//				}
//			}
			
			logger.info("svn {} end, revision range: {}, time: {}s", new String[] {isRevert ? "revert" : "merge", rangesToMergeString, ("" + (System.currentTimeMillis() - startTime) / 1000)});
			
			return mergeResultInfo;
		} catch (SVNException e) {
			throw new SVNOpsException("Merge failed " + e.getMessage(), e);
		} finally {
			ourClientManager.dispose();
		}
	}
	
	/**
	 * 计算合并区间，将输入的一个个 revision 改造为 svn 合并时使用的“-”连接的区间格式，输入输出示例:<br>
	 * [66]                     -> [[65, 66]]<br>
	 * [66, 67, 68, 69]         -> [[65, 69]]<br>
	 * [66, 69, 70, 71, 72, 80] -> [[65, 66], [68, 72], [79, 80]]<br>
	 * [66, 70, 72, 80, 100]    -> [[65, 66], [69, 70], [71, 72], [79, 80], [99, 100]]<br>
	 * [75, 80, 81]             -> [[74, 75], [79, 81]]<br>
 	 */
	private List<Long[]> getRevsionRange(Long[] revs, boolean isRevert) {
		Arrays.sort(revs);
		
		int revisionsCount = revs.length;
		long start = revs[0] - 1;
		List<Long[]> retList = new ArrayList<>();
		
		for (int i = 0; i < revisionsCount - 1; i++) {
			if (revs[i] + 1 < revs[i + 1]) {
				retList.add(new Long[] {start, revs[i]});
				
				start = revs[i + 1] - 1;
			}
		}
		
		if (start + 1 <= revs[revisionsCount - 1] || start + 1 == revs[0]) {
			retList.add(new Long[] {start, revs[revisionsCount - 1]});
		}
		
		// revert 时，range 和 merge 相反
		if (isRevert && !retList.isEmpty()) {
			for (Long[] range: retList) {
				Long tmp = range[0];
				range[0] = range[1];
				range[1] = tmp;
			}
			// 【SVN命令撤销合并时Revision的区间应当按从大到小排序】https://www.tapd.cn/54247054/bugtrace/bugs/view?bug_id=1154247054001009029
			Collections.reverse(retList);
		}
		
		return retList;
	}

	/**
	 * 合并某个commit到当前working copy所在分支，合并前需要确定当前working
	 * copy处在正确的分支，合并前需要进行分支checkout和reset，合并后需要另外主动进行commit和push
	 * 
	 * @param srcBranch    合并的源Branch
	 * @param targetBranch 合并的目标branch
	 * @param revision     commit号
	 * @param dryRun       是否检测运行
	 * @return
	 * @throws SVNOpsException
	 */
	public MergeResultInfo CherryPick(String srcBranch, String targetBranch, String revision, Boolean dryRun) throws SVNOpsException {
		return CherryPick(srcBranch, targetBranch, new String[] { revision }, false, dryRun);
	}

	/**
	 * 合并某个commit到当前working copy所在分支，合并前需要确定当前working
	 * copy处在正确的分支，合并前需要进行分支checkout和reset，合并后需要另外主动进行commit和push
	 * 
	 * @param srcBranch    合并的源Branch
	 * @param targetBranch 合并的目标branch
	 * @param revisions    多个commit号
	 * @param dryRun       是否检测运行
	 * @return
	 * @throws SVNOpsException
	 */
	public MergeResultInfo CherryPick(String srcBranch, String targetBranch, String[] revisions, Boolean dryRun) throws SVNOpsException {
		return CherryPick(srcBranch, targetBranch, revisions, false, dryRun);
	}

	/**
	 * revert某个commit到当前working copy所在分支，合并前需要确定当前working
	 * copy处在正确的分支，合并前需要进行分支checkout和reset，合并后需要另外主动进行commit和push
	 * 
	 * @param srcBranch    合并的源Branch
	 * @param targetBranch 合并的目标branch
	 * @param revisions    多个commit号
	 * @param dryRun       是否检测运行
	 * @return
	 * @throws SVNOpsException
	 */
	public MergeResultInfo revertCommit(String srcBranch, String targetBranch, String[] revisions, Boolean dryRun) throws SVNOpsException {
		return CherryPick(srcBranch, targetBranch, revisions, true, dryRun);
	}

	/**
	 * revert某个commit到当前working copy所在分支，合并前需要确定当前working
	 * copy处在正确的分支，合并前需要进行分支checkout和reset，合并后需要另外主动进行commit和push
	 * 
	 * @param srcBranch    合并的源Branch
	 * @param targetBranch 合并的目标branch
	 * @param revision     commit号
	 * @param dryRun       是否检测运行
	 * @return
	 * @throws SVNOpsException
	 */
	public MergeResultInfo revertCommit(String srcBranch, String targetBranch, String revision, Boolean dryRun) throws SVNOpsException {
		return CherryPick(srcBranch, targetBranch, new String[] { revision }, true, dryRun);
	}

	/**
	 * 获取目标分支上的svn:mergeinfo信息
	 * 
	 * @param targetBranch
	 * @return repo.getMergeInfo返回:
	 *         {/branches/branch-3=/branches/branch-3={/branches/branch-1=7-36,40,
	 *         /trunk=51-55}}
	 * @throws SVNOpsException
	 */
	public Map<String, SVNMergeRangeList> getMergeInfo(String targetBranch) throws SVNOpsException {
		try {
			long latestRevision = repo.getLatestRevision();
			Map<String, SVNMergeInfo> mergeInfo = repo.getMergeInfo(new String[] { targetBranch }, latestRevision, SVNMergeInfoInheritance.EXPLICIT, false);
			if (mergeInfo != null) {
				// 返回值中的分支路径相对于 repository 的根目录
				SVNMergeInfo svnMergeInfo = mergeInfo.get(repo.getRepositoryPath(targetBranch));
				if (svnMergeInfo != null) {
					return svnMergeInfo.getMergeSourcesToMergeLists();
				}
			}
			return null;
		} catch (SVNException e) {
			throw new SVNOpsException("getMergeInfo failed " + e.getMessage(), e);
		}
	}

	/**
	 * 获取源分支的 MergeRangeList
	 * 
	 * @param targetBranch 目标分支
	 * @param srcBranch    源分支
	 * @throws SVNOpsException
	 */
	public SVNMergeRangeList getMergeRangeList(String srcBranch, String targetBranch) throws SVNOpsException {
		try {
			srcBranch = getRealBranchPath(srcBranch);
			targetBranch = getRealBranchPath(targetBranch);

			Map<String, SVNMergeRangeList> map = getMergeInfo(targetBranch);
			if (map != null) {
				// 返回值中的分支路径相对于 repository 的根目录
				return map.get(repo.getRepositoryPath(srcBranch));
			}

			return null;
		} catch (SVNException e) {
			throw new SVNOpsException("getMergeInfo failed " + e.getMessage(), e);
		}
	}
	
	/**
	 * 获取源分支在目标分支上已合并的 commit 列表
	 * 
	 * @param targetBranch 目标分支
	 * @param srcBranch    源分支
	 * @throws SVNOpsException
	 */
	public List<Long> getMergedCommitListFromMergeInfo(String srcBranch, String targetBranch) throws SVNOpsException {
		SVNMergeRangeList mergeRangeList = getMergeRangeList(srcBranch, targetBranch);
		
		List<Long> commits = new ArrayList<>();
		
		for (SVNMergeRange mergeRange: mergeRangeList.getRanges()) {
			long start = mergeRange.getStartRevision();
			long end = mergeRange.getEndRevision();
			
			while (start + 1 <= end) {
				commits.add(start + 1);
				start = start + 1;
			}
		}
		
		return commits;
	}

	/**
	 * 转换分支路径，获取分支相对working copy的真实路径
	 * @param branchName 分支名称
	 * @return
	 */
	public String getRealBranchPath(String branchName) {
		branchName = StringUtils.trim(branchName);
		if (StringUtils.isEmpty(branchName)) {
			return branchName;
		}

		if (StringUtils.isNotEmpty(trunkPath) && trunkPath.equals(branchName)) {
			return branchName;
		}

		String path = StringUtils.stripStart(this.branchesPath, "/");
		if (StringUtils.isNotEmpty(path) && !branchName.startsWith(branchesPath)) {
			branchName = branchesPath + "/" + branchName;
		}

		return branchName;
	}
	
	/**
	 * 转换标签路径，获取标签相对working copy的真实路径
	 * @param tagName 标签名称
	 * @return
	 */
	public String getRealTagPath(String tagName) {
		
		tagName = StringUtils.trim(tagName);
		if (StringUtils.isNotBlank(tagsPath)) {
			return tagsPath + "/" + tagName;
		}

		return tagName;
	}
	
	/**
	 * ========需求型使用========<br>
	 * 获取分支(working copy目录下的第一层子目录，或分支目录下的第一层子目录)在指定 revision 上的文件变更数量，
	 * 即使分支被删除也可以获取到正确的数量
	 */
	public int getFileChangedCount(String branch, long commit) throws Exception {
		SVNURL url = repoUrl;
		if (hasBranch(branch)) {
			url = repoUrl.appendPath(getRealBranchPath(branch), false);
		}
		
		ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
		SVNClientManager ourClientManager = SVNClientManager.newInstance(options, authManager);
		
		List<SVNDiffStatus> diffStatusList;
		try {
			diffStatusList = new LinkedList<>();
			ourClientManager.getDiffClient().doDiffStatus(
				url,
				SVNRevision.create(commit - 1),
				SVNRevision.create(commit),
				SVNRevision.HEAD,
				SVNDepth.INFINITY,
				true,
				new SVNDiffStatusHandler(diffStatusList)
			);
		} finally {
			ourClientManager.dispose();
		}
		
		return diffStatusList.size();
	}
	
	/**
	 * ========分支型使用========<br>
	 * 获取分支(working copy目录下的第一层子目录，或分支目录下的第一层子目录)在指定 revision 上的文件变更数量，
	 * 即使分支被删除也可以获取到正确的数量
	 */
	public int getFileChangedCount(String branch, long[] commits) throws Exception {
		SVNURL url = repoUrl;
		if (hasBranch(branch)) {
			url = repoUrl.appendPath(getRealBranchPath(branch), false);
		}
		
		ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
		SVNClientManager ourClientManager = SVNClientManager.newInstance(options, authManager);
		
		
		List<SVNDiffStatus> diffStatusList;
		try {
			diffStatusList = new LinkedList<>();
			
			long start = Long.MAX_VALUE;
			long end = 0;
			for (long commit: commits) {
				if (commit < start) {
					start = commit;
				}
				
				if (commit > end) {
					end = commit;
				}
			}
			
			ourClientManager.getDiffClient().doDiffStatus(
				url,
				SVNRevision.create(start - 1),
				SVNRevision.create(end),
				SVNRevision.HEAD,
				SVNDepth.INFINITY,
				true,
				new SVNDiffStatusHandler(diffStatusList)
			);
		} finally {
			ourClientManager.dispose();
		}
		
		return diffStatusList.size();
	}
	
	public SVNStatus statusCheck(String branch) throws SVNOpsException {
		ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
		SVNClientManager ourClientManager = SVNClientManager.newInstance(options, authManager);
		
		try {
			SVNStatusClient statusClient = ourClientManager.getStatusClient();
			File file = new File(repoLocalDir);
			return statusClient.doStatus(file, true);
		} catch (Exception e) {
			throw new SVNOpsException(e.getMessage(), e);
		} finally {
			ourClientManager.dispose();
		}
	}
	
	/** 
	 * 获取远程仓库指定分支的最新提交的信息<br>
	 * 只包含 commitId, author, committerData 几个信息
	 **/
	public CommitInfo getHeadCommit(String branch) throws SVNOpsException {
		DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
		SVNClientManager ourClientManager = SVNClientManager.newInstance(options, authManager);
		
		CommitInfo commitInfo = null;
		try {
			SVNWCClient wcClient = ourClientManager.getWCClient();
			
			SVNInfo svnInfo = null;
			svnInfo = wcClient.doInfo(repoUrl.appendPath(getRealBranchPath(branch), true), SVNRevision.HEAD, SVNRevision.HEAD);
			
			if (svnInfo != null) {
				commitInfo = new CommitInfo();
				commitInfo.setId(String.valueOf(svnInfo.getCommittedRevision().getNumber()));
				commitInfo.setAuthor(svnInfo.getAuthor());
				commitInfo.setAuthorDate(svnInfo.getCommittedDate());
				commitInfo.setCommitter(svnInfo.getAuthor());
				commitInfo.setCommitterDate(svnInfo.getCommittedDate());
			}
			
		} catch (SVNException e) {
			throw new SVNOpsException(e.getMessage(), e);
		} finally {
			ourClientManager.dispose();
		}
		
		return commitInfo;
	}
	
	/**
	 * 获取指定 commit 的 parent commit。如果有多个，返回和 commit 在同一个分支的 parent commit。
	 *
	 * @param commitId
	 * @return commit id 合法(>0)则返回 commitId - 1，否则返回 null
	 * @throws SVNOpsException
	 */
	public String getParent(String commitId) throws SVNOpsException {
		long rev = -1;
		try {
			if (StringUtils.isNumeric(commitId)) {
				rev = Long.parseLong(commitId);
				
				if (SVNRevision.isValidRevisionNumber(rev - 1)) {
					return (rev -1) + "";
				}
			}
			
			return null;
		} catch (Exception e) {
			throw new SVNOpsException(e);
		}
	}

	/**
	 * 创建目录（不支持多级目录）
	 *
	 * @param path   目录名称
	 * @throws SVNOpsException
	 */
	public void mkDir(String path) throws SVNOpsException {
		DefaultSVNOptions options = SVNWCUtil.createDefaultOptions(true);
		SVNClientManager ourClientManager = SVNClientManager.newInstance(options, authManager);
		try {
			SVNURL toUrl = repoUrl.appendPath(path, true);
			ourClientManager.getCommitClient().doMkDir(new SVNURL[] { toUrl },"create directory " + path);
		} catch (SVNException e) {
			logger.error("Create diretory " + path + " failed, " + e.getMessage(),e);
			throw new SVNOpsException("Create diretory " + path + " failed, " + e.getMessage(), e);
		} finally {
			ourClientManager.dispose();
		}
	}
	
	
	/**
	 * 获取到copy from时的revision, 即是新分支被创建出来的copy commit 注意: 如果分支不是copy出来的那么就会取到分支上最早的一个commit
	 * 
	 * doLog指定顺序 从1到HEAD查询, 限制结果数量为1条 , 从最老的commit的到最新
	 * @param branch 
	 * @return
	 * @throws SVNOpsException
	 */
	public long getCopyRevision(String branch) throws SVNOpsException {
		List<CommitInfo> commitInfoList = new LinkedList<CommitInfo>();
		ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
		SVNClientManager ourClientManager = SVNClientManager.newInstance(options, authManager);
		
		try {
			SVNLogClient logClient = ourClientManager.getLogClient();
			SVNRevision pegRevision = SVNRevision.HEAD;
			SVNRevision startRevision = SVNRevision.create(1);
			SVNRevision endRevision = SVNRevision.HEAD;
			SVNURL effectiveUrl = repoUrl.appendPath(getRealBranchPath(branch), true);
			
			logClient.doLog(effectiveUrl, null, pegRevision, startRevision,  endRevision, true, true, false, 1, null, new LogEntryHandler(this, commitInfoList, true, false, false, true));
			if (CollectionUtils.isNotEmpty(commitInfoList)) {
				return Long.parseLong(commitInfoList.get(0).getId());
			}
			
			return -1;
		} catch (SVNException e) {
			throw new SVNOpsException("getCopyFromRevision url:" + remoteUrl + " failed, " + e.getMessage(), e);
		} finally {
			ourClientManager.dispose();
		}
	}
	
	/**
	 * 使用 svn mergeinfo 命令获取源分支上已经合并到目标分支的 commit<br>
	 * @param srcBranch 源分支
	 * @param srcEndCommit 通过使用 `-r srcEndCommit:HEAD`选项，限制获取到的 mergeinfo 中  commit 数量。是否包含 srcEndCommit ?
	 * @param targetBranch 目标分支，在此分支上获取 srcBranch 的 mergeinfo
	 * @param targetRev 用于 targetBranch@targetRev，控制 targetBranch 上获取 mergeinfo 的起点
	 **/
	public String[] getSrcBranchMergedCommitsFromMergeInfo(String srcBranch, String srcEndCommit, String targetBranch, String targetRev) throws SVNOpsException {
		SVNURL srcUrl = null;
		SVNURL targetUrl = null;
		try {
			srcUrl = repoUrl.appendPath(getRealBranchPath(srcBranch), true);
			targetUrl = repoUrl.appendPath(getRealBranchPath(targetBranch), true);
		} catch (Exception ex) {
			throw new SVNOpsException(ex);
		}
		
		if (StringUtils.isBlank(targetRev)) {
			targetRev = "HEAD";
		}
		
		SVNCommand mergeinfo = this.command.mergeinfo().setOptions("--config-dir", SVNWCUtil.getDefaultConfigurationDirectory().toString(),
				"--show-revs=merged", srcUrl.toString(), targetUrl.toString() + "@" + targetRev);
		if (StringUtils.isNotBlank(srcEndCommit)) {
			// TODO 验证是 srcEndCommit 还是 srcEndCommit - 1
			mergeinfo.setOptions("-r", srcEndCommit + ":HEAD");
		}
		
		JSONObject obj = WorkingCopyUtils.executeCommand(repoLocalDir, mergeinfo.end());
		String result = null;
		if (obj.containsKey("result")) {
			result = obj.getString("result");
		}
		
		if (!StringUtils.equals(obj.getString("status"), "success")) {
			throw new SVNOpsException(StringUtils.isBlank(result) ? "execute svn mergeinfo command failed." : result);
		}

		String[] revs = new String[] {};
		if (StringUtils.isNotBlank(result)) {
			// windows下会多余一个\r字符
			revs = result.replace("r", "").replace("\r", "").split("\n");
		}
		
		return revs;
	}
	
    /**
     * 使用文件锁来限制仓库读写并发操作
     */
    public void lock() {
        // SVN的checkout特殊, repoLocalDir本身已经是仓库分支的路径
        lockFile = new File(repoLocalDir + ".lock");
        String lockedErrorMessage = String.format("当前仓库 '%s' 在分支 '%s' 存在未完成的同步或合并操作，请稍后重试", remoteUrl, FilenameUtils.getName(repoLocalDir));
        try {
        	if (!lockFile.getParentFile().exists()) {
        		// 仓库目录不存在, 或者后面又被人删除了, 文件锁打开前需要保证仓库目录存在
				File parent = lockFile.getParentFile();
				FileUtils.forceMkdir(parent);
			}
            randomAccessFile = new RandomAccessFile(lockFile, "rw");
            lockChannel = randomAccessFile.getChannel();
            buildLock = lockChannel.lock(0, 4096, true);

        } catch (IOException | OverlappingFileLockException e) {
            throw new LockFailedException(lockedErrorMessage, e);
        }
    }

    /**
     * 尝试是否能获取到文件锁, 判断目录有没有其他人正在操作, 不需要调用unlock
     */
    public void tryLock() {
        lockFile = new File(repoLocalDir + ".lock");
        String lockedErrorMessage = String.format("当前仓库 '%s' 在分支 '%s' 存在未完成的同步或合并操作，请稍后重试", remoteUrl, FilenameUtils.getName(repoLocalDir));

        if (lockFile.exists()) {
            try {
                randomAccessFile = new RandomAccessFile(lockFile, "rw");
                lockChannel = randomAccessFile.getChannel();
                buildLock = lockChannel.tryLock(0, 4096, false);
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
