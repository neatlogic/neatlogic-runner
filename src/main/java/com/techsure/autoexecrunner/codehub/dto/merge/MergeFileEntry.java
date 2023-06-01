package com.techsure.autoexecrunner.codehub.dto.merge;

import com.techsure.autoexecrunner.codehub.constvalue.MergeFileStatus;

/*
 * Copyright (C) 2020, wenhb@techsure.com.cn
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
