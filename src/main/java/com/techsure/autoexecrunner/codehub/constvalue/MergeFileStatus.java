/*
 * Copyright (C) 2020, wenhb@techsure.com.cn
 */
package com.techsure.autoexecrunner.codehub.constvalue;

public enum MergeFileStatus {
	UNTRACKED,
	ADDED,
	DELETED,
	UPDATED,
	REPLACED,
	MERGED,
	EXISTED,
	PROPERTY_UPDATED,
	PROPERTY_MERGED,
	CONFLICTED, 
	TREE_CONFLICTED,
	PROPERTY_CONFLICTED;
}
