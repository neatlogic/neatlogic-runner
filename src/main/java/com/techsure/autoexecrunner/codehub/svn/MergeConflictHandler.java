/*
 * Copyright (C) 2020, wenhb@techsure.com.cn
 */
package com.techsure.autoexecrunner.codehub.svn;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.ISVNConflictHandler;
import org.tmatesoft.svn.core.wc.SVNConflictDescription;
import org.tmatesoft.svn.core.wc.SVNConflictReason;
import org.tmatesoft.svn.core.wc.SVNConflictResult;
import org.tmatesoft.svn.core.wc.SVNMergeFileSet;

public class MergeConflictHandler implements ISVNConflictHandler {

	@Override
	public SVNConflictResult handleConflict(SVNConflictDescription conflictDescription) throws SVNException {
		SVNConflictReason reason = conflictDescription.getConflictReason();
		SVNMergeFileSet mergeFiles = conflictDescription.getMergeFiles();
		System.out.println("Conflict discovered in:" + mergeFiles.getWCPath());
		return null;
	}

}
