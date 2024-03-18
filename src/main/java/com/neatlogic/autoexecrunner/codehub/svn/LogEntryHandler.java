/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
package com.neatlogic.autoexecrunner.codehub.svn;

import java.util.List;
import java.util.Map;

import com.neatlogic.autoexecrunner.codehub.dto.commit.CommitInfo;
import com.neatlogic.autoexecrunner.codehub.dto.diff.FileDiffInfo;
import org.apache.commons.lang3.StringUtils;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.wc.SVNRevision;
public class LogEntryHandler implements ISVNLogEntryHandler {

	private SVNWorkingCopy svnWc = null;
	private List<CommitInfo> commitInfoList = null;
	private boolean onlyChangeInfo = true;
	private boolean parseByEachChangedFile = false;
	private boolean fullBranchLog = false;
	private boolean logOnly = true;
	private SVNLogEntry preLogEntry = null;

	/**
	 * svn log调用输出SVNLogEntry的处理类，默认以整个commit执行一次diff，只做修改行数的统计，不存放具体修改的行数据
	 * 
	 * @param svnWc,          svn working copy的类实例
	 * @param commitInfoList, 用于存储返回的commit列表，commit里包括了所有的diff信息
	 */
	public LogEntryHandler(SVNWorkingCopy svnWc, List<CommitInfo> commitInfoList) {
		this.svnWc = svnWc;
		this.commitInfoList = commitInfoList;
	}

	/**
	 * svn log调用输出SVNLogEntry的处理类，默认以整个commit执行一次diff
	 * 
	 * @param svnWc,          svn working copy的类实例
	 * @param commitInfoList, 用于存储返回的commit列表，commit里包括了所有的diff信息
	 * @param onlyChangeInfo, true:只做修改行数的统计，不存放具体修改的行数据，
	 *                        false:获取diff结果中所有的修改行信息，包括修改的行和上下文行
	 */
	public LogEntryHandler(SVNWorkingCopy svnWc, List<CommitInfo> commitInfoList, boolean onlyChangeInfo) {
		this.svnWc = svnWc;
		this.commitInfoList = commitInfoList;
		this.onlyChangeInfo = onlyChangeInfo;
	}

	/**
	 * svn log调用输出SVNLogEntry的处理类
	 * 
	 * @param svnWc,                  svn working copy的类实例
	 * @param commitInfoList,         用于存储返回的commit列表，commit里包括了所有的diff信息
	 * @param onlyChangeInfo,         true:只做修改行数的统计，不存放具体修改的行数据，
	 *                                false:获取diff结果中所有的修改行信息，包括修改的行和上下文行
	 * @param parseByEachChangedFile, true:通过svnlog获取详细的log entry，然后逐个文件进行diff,
	 *                                false:默认以整个commit执行一次diff。
	 *                                通过commit获取所有变更的文件或目录路径，逐个进行diff，速度较慢，比整个commit进行diff慢3倍以上，这种diff会把空目录的新增和删除也能体现出来；
	 *                                而以整个commit为单位进行diff，速度较快，但是每个diff
	 *                                entry切分可能存在不稳定因素，而且，整个commit diff会忽略空目录项
	 */
	public LogEntryHandler(SVNWorkingCopy svnWc, List<CommitInfo> commitInfoList, boolean onlyChangeInfo, boolean parseByEachChangedFile) {
		this.svnWc = svnWc;
		this.commitInfoList = commitInfoList;
		this.onlyChangeInfo = onlyChangeInfo;
		this.parseByEachChangedFile = parseByEachChangedFile;
	}

	/**
	 * svn log调用输出SVNLogEntry的处理类
	 * 
	 * @param svnWc,                  svn working copy的类实例
	 * @param commitInfoList,         用于存储返回的commit列表，commit里包括了所有的diff信息
	 * @param onlyChangeInfo,         true:只做修改行数的统计，不存放具体修改的行数据，
	 *                                false:获取diff结果中所有的修改行信息，包括修改的行和上下文行
	 * @param parseByEachChangedFile, true:通过svnlog获取详细的log entry，然后逐个文件进行diff,
	 *                                false:默认以整个commit执行一次diff。
	 *                                通过commit获取所有变更的文件或目录路径，逐个进行diff，速度较慢，比整个commit进行diff慢3倍以上，这种diff会把空目录的新增和删除也能体现出来；
	 *                                而以整个commit为单位进行diff，速度较快，但是每个diff
	 *                                entry切分可能存在不稳定因素，而且，整个commit diff会忽略空目录项
	 * @param fullBranchLog           true:会忽略最后一个commit，因为分支的最后一个commit是copy
	 * @param logOnly                 true:只获取日志不分析commit修改的文件 to，比较没有意义
	 */
	public LogEntryHandler(SVNWorkingCopy svnWc, List<CommitInfo> commitInfoList, boolean onlyChangeInfo, boolean parseByEachChangedFile, boolean fullBranchLog, boolean logOnly) {
		this.svnWc = svnWc;
		this.commitInfoList = commitInfoList;
		this.onlyChangeInfo = onlyChangeInfo;
		this.parseByEachChangedFile = parseByEachChangedFile;
		this.fullBranchLog = fullBranchLog;
		this.logOnly = logOnly;
	}

	@Override
	public void handleLogEntry(SVNLogEntry logEntry) throws SVNException {
		CommitInfo commitInfo = null;

		if (this.logOnly) {
			commitInfo = new CommitInfo();
		} else {
			if (parseByEachChangedFile) {
				// 对commit中修改的文件项逐个进行diff，速度较慢，比整个commit进行diff慢3倍以上，这种diff会把空目录的新增和删除也能体现出来
				commitInfo = diffByFile(logEntry);
			} else {
				// 以整个commit为单位进行diff，速度较快，但是每个diff entry切分可能存在不稳定因素，而且，整个commit diff会忽略空目录项
				if (fullBranchLog) {
					if (preLogEntry != null) {
						commitInfo = diffByCommit(preLogEntry);
					}

					preLogEntry = logEntry;
				} else {
					commitInfo = diffByCommit(logEntry);
				}
			}
		}

		// 修复错误的输出, 排除掉svn -1的情况
		if (commitInfo != null && SVNRevision.isValidRevisionNumber(logEntry.getRevision())) {
			commitInfo.setId(Long.toString(logEntry.getRevision()));
			commitInfo.setAuthor(logEntry.getAuthor());
			commitInfo.setAuthorDate(logEntry.getDate());
			commitInfo.setCommitter(logEntry.getAuthor());
			commitInfo.setCommitterDate(logEntry.getDate());
			commitInfo.setComment(logEntry.getMessage());

			//TODO: 根据commit comment进行需求关联的处理
			commitInfoList.add(commitInfo);
		}
	}

	/**
	 * 对commit中修改的文件项逐个进行diff，速度较慢，比整个commit进行diff慢3倍以上，这种diff会把空目录的新增和删除也能体现出来
	 * 
	 * @param logEntry
	 * @return 返回Commit的详细信息，包括修改的文件信息
	 * @throws SVNException
	 */
	private CommitInfo diffByFile(SVNLogEntry logEntry) throws SVNException {
		CommitInfo commitInfo = new CommitInfo();

		// 因为仅仅依靠diff的输出信息，不足以判断文件类型和修改类型，所以需要通过SVNLogEntryPath对信息进行补充
		Map<String, SVNLogEntryPath> changedPaths = logEntry.getChangedPaths();
		for (SVNLogEntryPath changedPath : changedPaths.values()) {
			SVNNodeKind nodeKind = changedPath.getKind();
			String filePath = changedPath.getPath();

			String startPath = "";
			String branch = null;

			if (filePath != null) {
				if (StringUtils.isNotBlank(svnWc.getBranchesPath()) && filePath.startsWith(svnWc.getBranchesPath())) {
					int pos = filePath.indexOf('/', svnWc.getBranchesPathLen() + 1);
					branch = filePath.substring(svnWc.getBranchesPathLen() + 1, pos);
					startPath = filePath.substring(0, pos);
					filePath = filePath.substring(pos + 1);
				} else if (StringUtils.isNotBlank(svnWc.getTagsPath()) && filePath.startsWith(svnWc.getTagsPath())) {
					int pos = filePath.indexOf('/', svnWc.getTagsPathLen() + 1);
					startPath = filePath.substring(0, pos);
					filePath = filePath.substring(pos + 1);
				} else if (StringUtils.isNotBlank(svnWc.getTrunkPath()) && filePath.startsWith(svnWc.getTrunkPath())) {
					int pos = filePath.indexOf('/', svnWc.getTrunkPathLen() + 1);
					branch = svnWc.getTrunkPath();
					startPath = filePath.substring(0, pos);
					filePath = filePath.substring(pos + 1);
				}

				if (commitInfo.getBranch() == null) {
					commitInfo.setBranch(branch);
				}
			}

			FileDiffInfo diffInfo = null;
			if (nodeKind.equals(SVNNodeKind.FILE)) {
				if (onlyChangeInfo) {
					diffInfo = svnWc.getChangeInfo(startPath, filePath, logEntry.getRevision());
				} else {
					diffInfo = svnWc.getDiffInfo(startPath, filePath, logEntry.getRevision());
				}

				diffInfo.setFileType('F');
				diffInfo.setFromFileName(filePath);
				diffInfo.setToFileName(filePath);
				diffInfo.setModifiedType(changedPath.getType());
				commitInfo.addDiffInfo(diffInfo);
			} else if (nodeKind.equals(SVNNodeKind.DIR)) {
				diffInfo = new FileDiffInfo();
				diffInfo.setFileType('D');
				diffInfo.setModifiedType(changedPath.getType());
			}

			if (diffInfo != null) {
				if (changedPath.getType() == 'A') {
					diffInfo.setToFileName(filePath);
					diffInfo.setFromFileName("/dev/null");
					commitInfo.addDiffInfo(diffInfo);
				} else if (changedPath.getType() == 'D') {
					diffInfo.setFromFileName(filePath);
					diffInfo.setToFileName("/dev/null");
					commitInfo.addDiffInfo(diffInfo);
				}

				if (diffInfo.getInsertedCount() > 0 && diffInfo.getDeletedCount() == 0) {
					diffInfo.setModifiedType('A');
				} else if (diffInfo.getInsertedCount() == 0 && diffInfo.getDeletedCount() > 0) {
					diffInfo.setModifiedType('D');
				}
			}
		}

		return commitInfo;
	}

	/**
	 * 以整个commit为单位进行diff，速度较快，但是每个diff entry切分可能存在不稳定因素，而且，整个commit diff会忽略空目录项
	 * 
	 * @param logEntry
	 * @return 返回Commit的详细信息，包括修改的文件信息
	 * @throws SVNException
	 */
	private CommitInfo diffByCommit(SVNLogEntry logEntry) throws SVNException {
		CommitInfo commitInfo = new CommitInfo();

		List<FileDiffInfo> diffInfoList = null;
		if (onlyChangeInfo) {
			diffInfoList = svnWc.getChangeInfo("", logEntry.getRevision());
		} else {
			diffInfoList = svnWc.getDiffInfo("", logEntry.getRevision(), -1);
		}

		// 因为仅仅依靠diff的输出信息，不足以判断文件类型和修改类型，所以需要通过SVNLogEntryPath对信息进行补充
		Map<String, SVNLogEntryPath> changedPaths = logEntry.getChangedPaths();
		for (FileDiffInfo diffInfo : diffInfoList) {

			// SVNDiffParser已经判断过文件类型了, 是根据(nonexistent)来判断的, 这里可以没必要处理了
			if (diffInfo.getModifiedType() == 'U') {
				if (diffInfo.getInsertedCount() > 0 && diffInfo.getDeletedCount() == 0) {
					diffInfo.setModifiedType('A');
				} else if (diffInfo.getInsertedCount() == 0 && diffInfo.getDeletedCount() > 0) {
					diffInfo.setModifiedType('D');
				}
			}

			// 通过commit的getChangedPaths返回的路径会带有‘/’，但是diff的输出却没有开头的‘/’
			SVNLogEntryPath entryPath = changedPaths.get("/" + diffInfo.getToFileName());
			if (entryPath == null) {
				entryPath = changedPaths.get("/" + diffInfo.getFromFileName());
			}
					
			if (entryPath != null) {
				diffInfo.setFileType(entryPath.getType());
				
				// fix bug: 这里是fileType
				if (entryPath.getKind() == SVNNodeKind.FILE) {
					diffInfo.setFileType('F');
				} else if (entryPath.getKind() == SVNNodeKind.DIR) {
					diffInfo.setFileType('D');
				}
			}

			String branch = null;
			String toFileName = diffInfo.getToFileName();
			if (toFileName != null) {
				if (StringUtils.isNotBlank(svnWc.getBranchesPath()) && toFileName.startsWith(svnWc.getBranchesPath())) {
					int pos = toFileName.indexOf('/', svnWc.getBranchesPathLen() + 1);
					toFileName = toFileName.substring(pos + 1);
				} else if (StringUtils.isNotBlank(svnWc.getTagsPath()) && toFileName.startsWith(svnWc.getTagsPath())) {
					int pos = toFileName.indexOf('/', svnWc.getTagsPathLen() + 1);
					toFileName = toFileName.substring(pos + 1);
				} else if (StringUtils.isNotBlank(svnWc.getTrunkPath()) && toFileName.startsWith(svnWc.getTrunkPath())) {
					int pos = toFileName.indexOf('/', svnWc.getTrunkPathLen() + 1);
					toFileName = toFileName.substring(pos + 1);
				}
			}
			diffInfo.setToFileName(toFileName);

			String fromFileName = diffInfo.getFromFileName();
			if (fromFileName != null) {
				if (StringUtils.isNotBlank(svnWc.getBranchesPath()) && fromFileName.startsWith(svnWc.getBranchesPath())) {
					int pos = fromFileName.indexOf('/', svnWc.getBranchesPathLen() + 1);
					branch = fromFileName.substring(svnWc.getBranchesPathLen() + 1, pos);
					fromFileName = fromFileName.substring(pos + 1);
				} else if (StringUtils.isNotBlank(svnWc.getTagsPath()) && fromFileName.startsWith(svnWc.getTagsPath())) {
					int pos = fromFileName.indexOf('/', svnWc.getTagsPathLen() + 1);
					fromFileName = fromFileName.substring(pos + 1);
				} else if (StringUtils.isNotBlank(svnWc.getTrunkPath()) && fromFileName.startsWith(svnWc.getTrunkPath())) {
					branch = svnWc.getTrunkPath();
					int pos = fromFileName.indexOf('/', svnWc.getTrunkPathLen() + 1);
					fromFileName = fromFileName.substring(pos + 1);
				}
			}
			diffInfo.setFromFileName(fromFileName);

			if (commitInfo.getBranch() == null) {
				commitInfo.setBranch(branch);
			}
		}

		commitInfo.setDiffInfoList(diffInfoList);

		return commitInfo;
	}
}
