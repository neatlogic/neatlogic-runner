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
package com.neatlogic.autoexecrunner.codehub.dto.diff;

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
