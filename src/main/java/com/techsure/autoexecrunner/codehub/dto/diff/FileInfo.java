/*
 * Copyright (C) 2020, wenhb@techsure.com.cn
 */
package com.techsure.autoexecrunner.codehub.dto.diff;

import java.util.Date;

public class FileInfo {
	private String path = "";
	private char type = 'F';
	private String lastAuthor = "";
	private Date lastChangeDate = null;
	private String lastCommitMessage = "";
	private boolean isLock = false;
	private String lockOwner = "";
	
	private String lastCommitId = "";
	
	private boolean isBinary = false;

	public FileInfo() {
		
	}
	
	public FileInfo(String path, char type) {
		this.path = path;
		this.type = type;
	}
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public char getType() {
		return type;
	}

	public void setType(char type) {
		this.type = type;
	}

	public String getLastAuthor() {
		return lastAuthor;
	}

	public void setLastAuthor(String lastAuthor) {
		this.lastAuthor = lastAuthor;
	}

	public Date getLastChangeDate() {
		return lastChangeDate;
	}

	public void setLastChangeDate(Date lastChangeDate) {
		this.lastChangeDate = lastChangeDate;
	}

	public String getLastCommitMessage() {
		return lastCommitMessage;
	}

	public void setLastCommitMessage(String lastCommitMessage) {
		this.lastCommitMessage = lastCommitMessage;
	}

	public boolean isLock() {
		return isLock;
	}

	public void setLock(boolean isLock) {
		this.isLock = isLock;
	}

	public String getLockOwner() {
		return lockOwner;
	}

	public void setLockOwner(String lockOwner) {
		this.lockOwner = lockOwner;
	}
	
	public String getLastCommitId() {
		return lastCommitId;
	}

	public void setLastCommitId(String lastCommitId) {
		this.lastCommitId = lastCommitId;
	}
	
	public String toString() {
		return type + " " + path + " " + lastAuthor + " " + lastChangeDate + " " + lastCommitMessage; 
	}

	public boolean isBinary() {
		return isBinary;
	}

	public void setBinary(boolean isBinary) {
		this.isBinary = isBinary;
	}
}
