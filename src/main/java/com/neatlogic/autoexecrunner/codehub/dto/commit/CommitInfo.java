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
package com.neatlogic.autoexecrunner.codehub.dto.commit;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.neatlogic.autoexecrunner.codehub.dto.diff.FileDiffInfo;

public class CommitInfo {
	private String id = null;
	private String commitId = null;
	private String comment = null;
	private String message = null;
	private String branch = null;

	/** 一个commitVo只关联一个需求，方便上层处理，如果确实有关联多个的情况，就生成多个commitVo，每个commitVo分别关联不同issueNo */
	private String issueNo;
	private List<String> bugfixList = new LinkedList<String>();
	private List<FileDiffInfo> diffInfoList = new LinkedList<FileDiffInfo>();

	String author = null;
	String authorEmail = "";
	Date authorDate = null;
	String committer = null;
	String committerEmail = "";
	Date committerDate = null;
	String mergeStatus = "open";

	Long authorDateTimestamp = null;
	Long committerDateTimestamp = null;

	private Integer fileAddCount;
	private Integer fileDeleteCount;
	private Integer fileModifyCount;
	private Integer lineAddCount;
	private Integer lineDeleteCount;

	public String getMergeStatus() {
		return mergeStatus;
	}

	public void setMergeStatus(String mergeStatus) {
		this.mergeStatus = mergeStatus;
	}

	public CommitInfo() {

	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
		this.commitId = id;
	}

	public String getIssueNo() {
		return issueNo;
	}

	public void setIssueNo(String issueNo) {
		this.issueNo = issueNo;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
		this.message = comment;
	}

	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getAuthorEmail() {
		return authorEmail;
	}

	public void setAuthorEmail(String authorEmail) {
		this.authorEmail = authorEmail;
	}

	public Date getAuthorDate() {
		return authorDate;
	}

	public void setAuthorDate(Date authorDate) {
		this.authorDate = authorDate;
	}

	public String getCommitter() {
		return committer;
	}

	public void setCommitter(String committer) {
		this.committer = committer;
	}

	public String getCommitterEmail() {
		return committerEmail;
	}

	public void setCommitterEmail(String committerEmail) {
		this.committerEmail = committerEmail;
	}

	public Date getCommitterDate() {
		return committerDate;
	}

	public void setCommitterDate(Date committerDate) {
		this.committerDate = committerDate;
	}

	public List<String> getBugfixList() {
		return bugfixList;
	}

	public void setBugfixList(List<String> bugfixList) {
		this.bugfixList = bugfixList;
	}

	public void addBugfix(String bugfix) {
		this.bugfixList.add(bugfix);
	}
	
	public List<FileDiffInfo> getDiffInfoList() {
		return diffInfoList;
	}

	public void setDiffInfoList(List<FileDiffInfo> diffInfoList) {
		this.diffInfoList = diffInfoList;
	}

	public void addDiffInfo(FileDiffInfo diffInfo) {
		this.diffInfoList.add(diffInfo);
	}

	@Override
	public String toString() {
		String content = "";
		content = content + "commit: " + id + "\n";
		//content = content + "issueNoList: " + issueNoList + "\n";
		content = content + "probaly branch: " + branch + "\n";
		content = content + "Author:\t" + author + " " + authorEmail + "\n";
		content = content + "Author Date:\t" + authorDate + "\n";
		content = content + "committer:\t" + committer + " " + committerEmail + "\n";
		content = content + "Commit Date:\t" + committerDate + "\n";
		content = content + comment + "\n";
		
		for(FileDiffInfo diffInfo:diffInfoList) {
			content = content + diffInfo + "\n";
		}
		
		return content;
	}

	// 兼容写法
	public String getCommitId() {
		return commitId;
	}

	public String getMessage() {
		return message;
	}
	
	public Long getAuthorDateTimestamp() {
		if (authorDate == null) {
			return null;
		}
		
		return authorDate.getTime();
	}
	
	public Long getCommitterDateTimestamp() {
		if (committerDate == null) {
			return null;
		}
		
		return committerDate.getTime();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == null) {
			return false;
		}
		
		if (this.getClass() != other.getClass()) {
			return false;
		}
		
		return this.id.equals(((CommitInfo)other).getId());
	}

	public Integer getFileAddCount() {
		return fileAddCount;
	}

	public void setFileAddCount(Integer fileAddCount) {
		this.fileAddCount = fileAddCount;
	}

	public Integer getFileDeleteCount() {
		return fileDeleteCount;
	}

	public void setFileDeleteCount(Integer fileDeleteCount) {
		this.fileDeleteCount = fileDeleteCount;
	}

	public Integer getLineAddCount() {
		return lineAddCount;
	}

	public void setLineAddCount(Integer lineAddCount) {
		this.lineAddCount = lineAddCount;
	}

	public Integer getLineDeleteCount() {
		return lineDeleteCount;
	}

	public void setLineDeleteCount(Integer lineDeleteCount) {
		this.lineDeleteCount = lineDeleteCount;
	}

	public Integer getFileModifyCount() {
		return fileModifyCount;
	}

	public void setFileModifyCount(Integer fileModifyCount) {
		this.fileModifyCount = fileModifyCount;
	}
}
