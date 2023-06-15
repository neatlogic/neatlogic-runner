package com.neatlogic.autoexecrunner.codehub.dto.merge;

import com.neatlogic.autoexecrunner.codehub.constvalue.MergeFileStatus;

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
public class MergeFileEntry {
	private String filePath;
	private MergeFileStatus mergeStatus;
	private String description = "";
	private boolean isBinary = false;
	private boolean isConflict = false;
	
	public MergeFileEntry(String filePath) {
		this.filePath = filePath;
	}
	
	public MergeFileEntry(String filePath, MergeFileStatus mergeStatus, boolean isBinary) {
		this.filePath = filePath;
		this.mergeStatus = mergeStatus;
		this.isBinary = isBinary;
		
		switch(mergeStatus) {
			case CONFLICTED:
				isConflict = true;
			case TREE_CONFLICTED:
				isConflict = true;
			case PROPERTY_CONFLICTED:
				isConflict = true;
			default:
				isConflict = false;
		}
	}
	
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public MergeFileStatus getMergeStatus() {
		return mergeStatus;
	}
	public void setMergeStatus(MergeFileStatus mergeStatus) {
		this.mergeStatus = mergeStatus;
	}
	public boolean isBinary() {
		return isBinary;
	}
	public void setBinary(boolean isBinary) {
		this.isBinary = isBinary;
	}
	public boolean isConflict() {
		return isConflict;
	}
	public void setConflict(boolean isConflict) {
		this.isConflict = isConflict;
	}
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
}
