/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */
package com.neatlogic.autoexecrunner.codehub.svn;

import com.neatlogic.autoexecrunner.codehub.constvalue.MergeFileStatus;
import com.neatlogic.autoexecrunner.codehub.dto.merge.MergeFileEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNTreeConflictUtil;
import org.tmatesoft.svn.core.wc.ISVNStatusHandler;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusType;

import java.io.File;
import java.util.List;


public class WCStatusHandler implements ISVNStatusHandler {

	private List<MergeFileEntry> mergeEntryList = null;
	boolean hasConflict = false;
	
	
	public List<MergeFileEntry> getMergeEntryList() {
		return mergeEntryList;
	}

	public void setMergeEntryList(List<MergeFileEntry> mergeEntryList) {
		this.mergeEntryList = mergeEntryList;
	}

	public boolean isHasConflict() {
		return hasConflict;
	}

	public void setHasConflict(boolean hasConflict) {
		this.hasConflict = hasConflict;
	}

	@Override
	public void handleStatus(SVNStatus status) throws SVNException {
		if (status == null || (!(status.isVersioned() || status.getTreeConflict() != null || status.getNodeStatus() == SVNStatusType.STATUS_EXTERNAL)) || (status.getCombinedNodeAndContentsStatus() == SVNStatusType.STATUS_NONE && status.getRemoteContentsStatus() == SVNStatusType.STATUS_NONE)) {
			return;
		}

		String filePath = status.getFile().getAbsolutePath();
		MergeFileEntry mergeEntry = new MergeFileEntry(filePath);

		SVNStatusType statusType = status.getCombinedNodeAndContentsStatus();
		SVNStatusType propStatusType = status.getPropertiesStatus();
		
		if(statusType.equals(SVNStatusType.STATUS_ADDED)) {
			mergeEntry.setMergeStatus(MergeFileStatus.ADDED);
		} else if(statusType.equals(SVNStatusType.STATUS_CONFLICTED)) {
			mergeEntry.setMergeStatus(MergeFileStatus.CONFLICTED);
			mergeEntry.setConflict(true);
			hasConflict = true;
		} else if(statusType.equals(SVNStatusType.STATUS_DELETED)) {
			mergeEntry.setMergeStatus(MergeFileStatus.DELETED);
		} else if(statusType.equals(SVNStatusType.MERGED)) {
			mergeEntry.setMergeStatus(MergeFileStatus.MERGED);
		} else if(statusType.equals(SVNStatusType.STATUS_MODIFIED)) {
			mergeEntry.setMergeStatus(MergeFileStatus.UPDATED);
		} else if(statusType.equals(SVNStatusType.STATUS_NAME_CONFLICT)) {
			mergeEntry.setMergeStatus(MergeFileStatus.TREE_CONFLICTED);
			mergeEntry.setConflict(true);
			hasConflict = true;
		} else if(statusType.equals(SVNStatusType.STATUS_REPLACED)) {
			mergeEntry.setMergeStatus(MergeFileStatus.REPLACED);
		} else if(statusType.equals(SVNStatusType.STATUS_UNVERSIONED)) {
			mergeEntry.setMergeStatus(MergeFileStatus.UNTRACKED);
		} 
		
		if(propStatusType.equals(SVNStatusType.STATUS_CONFLICTED)) {
			mergeEntry.setMergeStatus(MergeFileStatus.PROPERTY_CONFLICTED);
			mergeEntry.setConflict(true);
			hasConflict = true;
		} else if(propStatusType.equals(SVNStatusType.STATUS_MODIFIED)) {
			mergeEntry.setMergeStatus(MergeFileStatus.PROPERTY_UPDATED);
		} else if(propStatusType.equals(SVNStatusType.MERGED)) {
			mergeEntry.setMergeStatus(MergeFileStatus.MERGED);
		} 
			
		if (status.getTreeConflict() != null) {
			String description = SVNTreeConflictUtil.getHumanReadableConflictDescription(status.getTreeConflict());
			mergeEntry.setMergeStatus(MergeFileStatus.TREE_CONFLICTED);
			mergeEntry.setDescription(description);
			mergeEntry.setConflict(true);
			hasConflict = true;
		} else {
			String movedFromLine = "";
			String movedToLine = "";

			if (status.getMovedFromPath() != null && status.getMovedToPath() != null && status.getMovedFromPath().equals(status.getMovedToPath())) {
				movedFromLine = String.format("swapped places with %s", getRelativePath(status.getMovedFromPath()));

				mergeEntry.setDescription(movedFromLine);
			} else if (status.getMovedFromPath() != null || status.getMovedToPath() != null) {
				if (status.getMovedFromPath() != null) {
					movedFromLine = String.format("from %s", getRelativePath(status.getMovedFromPath()));
				}
				if (status.getMovedToPath() != null) {
					movedToLine = String.format("to %s", getRelativePath(status.getMovedToPath()));
				}

				mergeEntry.setDescription(String.format("move % %", movedFromLine, movedToLine));
			}
		}
		
		mergeEntryList.add(mergeEntry);
		//result.append(status.isLocked() ? 'L' : ' ');
		//result.append(status.getLocalLock() != null ? 'K' : ' ');
	}

	private String getRelativePath(File file) {
		return "";
	}
}
