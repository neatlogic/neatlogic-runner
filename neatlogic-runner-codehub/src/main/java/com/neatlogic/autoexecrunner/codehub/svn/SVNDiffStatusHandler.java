package com.neatlogic.autoexecrunner.codehub.svn;

import java.util.List;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.wc.ISVNDiffStatusHandler;
import org.tmatesoft.svn.core.wc.SVNDiffStatus;

public class SVNDiffStatusHandler implements ISVNDiffStatusHandler {

	private List<SVNDiffStatus> diffStatusList;
	
	public SVNDiffStatusHandler(List<SVNDiffStatus> diffStatusList) {
		this.diffStatusList = diffStatusList;
	}
	
	@Override
	public void handleDiffStatus(SVNDiffStatus diffStatus) throws SVNException {
		if (diffStatus.getPath() != null && !diffStatus.getPath().equals("")
				&& diffStatus.getKind().equals(SVNNodeKind.FILE)) {
			this.diffStatusList.add(diffStatus);
		}
	}

}
