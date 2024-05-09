package com.neatlogic.autoexecrunner.codehub.dto.merge;

import com.neatlogic.autoexecrunner.codehub.constvalue.MergeFileStatus;

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
