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
package com.neatlogic.autoexecrunner.codehub.svn;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.neatlogic.autoexecrunner.codehub.constvalue.MergeFileStatus;
import com.neatlogic.autoexecrunner.codehub.dto.merge.MergeFileEntry;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNStatusType;

public class MergeEventHandler implements ISVNEventHandler {

	private String workingPath = "";
	private int workingPathLen = 0;
	private List<MergeFileEntry> mergeEntryList = null;
	boolean hasConflict = false;
	
	public MergeEventHandler(String _workingPath) {
		this.mergeEntryList = new ArrayList<MergeFileEntry>();
		this.workingPath = _workingPath;
		
		if(this.workingPath.endsWith(File.separator)) {
			this.workingPathLen = this.workingPath.length();
		}else {
			this.workingPathLen = this.workingPath.length() + 1;
		}
		
		//System.out.println("DEBUG: " + workingPath + " " + workingPathLen);
	}
	
	
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
	public void checkCancelled() throws SVNCancelException {
		// TODO Auto-generated method stub
	}

	@Override
	public void handleEvent(SVNEvent event, double progress) throws SVNException {
		File file = event.getFile();
        String path = null;
        
        if (file != null) {
        	String filePath = file.getAbsolutePath();
        	if(filePath.length() > workingPathLen) {
        		path = filePath.substring(workingPathLen);
        	}
        }

        //StringBuffer buffer = new StringBuffer();
        if (event.getAction() == SVNEventAction.UPDATE_DELETE) {
            //buffer.append("D    " + path + "\n");
        	MergeFileEntry mergeEntry = new MergeFileEntry(path, MergeFileStatus.DELETED, false);
            mergeEntryList.add(mergeEntry);
        } else if (event.getAction() == SVNEventAction.UPDATE_BROKEN_LOCK) {
            //buffer.append("B    " + path + "\n");
        } else if (event.getAction() == SVNEventAction.UPDATE_REPLACE) {
            //buffer.append("R    " + path + "\n");
        	MergeFileEntry mergeEntry = new MergeFileEntry(path, MergeFileStatus.REPLACED, false);
        	mergeEntryList.add(mergeEntry);
        } else if (event.getAction() == SVNEventAction.UPDATE_ADD) {
            if (event.getContentsStatus() == SVNStatusType.CONFLICTED) {
                //buffer.append("C    " + path + "\n");
            	hasConflict = true;
            	MergeFileEntry mergeEntry = new MergeFileEntry(path, MergeFileStatus.CONFLICTED, false);
            	mergeEntry.setConflict(true);
            	mergeEntryList.add(mergeEntry);
            	
            } else {
                //buffer.append("A    " + path + "\n");
            	MergeFileEntry mergeEntry = new MergeFileEntry(path, MergeFileStatus.ADDED, false);
            	mergeEntryList.add(mergeEntry);
            }
        } else if (event.getAction() == SVNEventAction.UPDATE_EXISTS) {
            if (event.getContentsStatus() == SVNStatusType.CONFLICTED) {
                //buffer.append('C');
            	hasConflict = true;
            	MergeFileEntry mergeEntry = new MergeFileEntry(path, MergeFileStatus.CONFLICTED, false);
            	mergeEntry.setConflict(true);
            	mergeEntryList.add(mergeEntry);
            } else {
                //buffer.append('E');
            	hasConflict = true;
            	MergeFileEntry mergeEntry = new MergeFileEntry(path, MergeFileStatus.EXISTED, false);
            	mergeEntry.setConflict(true);
            	mergeEntryList.add(mergeEntry);
            }
            
            if (event.getPropertiesStatus() == SVNStatusType.CONFLICTED) {
                //buffer.append('C');
            	hasConflict = true;
            	MergeFileEntry mergeEntry = new MergeFileEntry(path, MergeFileStatus.PROPERTY_CONFLICTED, false);
            	mergeEntry.setConflict(true);
            	mergeEntryList.add(mergeEntry);
            } else if (event.getPropertiesStatus() == SVNStatusType.MERGED) {
                //buffer.append('G');
            	MergeFileEntry mergeEntry = new MergeFileEntry(path, MergeFileStatus.PROPERTY_MERGED, false);
                mergeEntryList.add(mergeEntry);
            } else {
                //buffer.append(' ');
            }
            //buffer.append("   " + path + "\n");
        } else if (event.getAction() == SVNEventAction.UPDATE_UPDATE || event.getAction() == SVNEventAction.MERGE_RECORD_INFO) {
            SVNStatusType propStatus = event.getPropertiesStatus();
            if (event.getNodeKind() == SVNNodeKind.DIR &&
                    (propStatus == SVNStatusType.INAPPLICABLE || propStatus == SVNStatusType.UNKNOWN || propStatus == SVNStatusType.UNCHANGED)) {
                return;
            }
            if (event.getNodeKind() == SVNNodeKind.FILE) {
                if (event.getContentsStatus() == SVNStatusType.CONFLICTED) {
                    //buffer.append('C');
                	hasConflict = true;
                	MergeFileEntry mergeEntry = new MergeFileEntry(path, MergeFileStatus.CONFLICTED, false);
                	mergeEntry.setConflict(true);
                	mergeEntryList.add(mergeEntry);
                } else if (event.getContentsStatus() == SVNStatusType.MERGED){
                    //buffer.append('G');
                	MergeFileEntry mergeEntry = new MergeFileEntry(path, MergeFileStatus.MERGED, false);
                    mergeEntryList.add(mergeEntry);
                } else if (event.getContentsStatus() == SVNStatusType.CHANGED){
                    //buffer.append('U');
                	MergeFileEntry mergeEntry = new MergeFileEntry(path, MergeFileStatus.UPDATED, false);
                	mergeEntryList.add(mergeEntry);
                } else {
                    //buffer.append(' ');
                }
            } else {
                //buffer.append(' ');
            }
            if (event.getPropertiesStatus() == SVNStatusType.CONFLICTED) {
                //buffer.append('C');
            	hasConflict = true;
            	MergeFileEntry mergeEntry = new MergeFileEntry(path, MergeFileStatus.PROPERTY_CONFLICTED, false);
            	mergeEntry.setConflict(true);
                mergeEntryList.add(mergeEntry);
            } else if (event.getPropertiesStatus() == SVNStatusType.MERGED){
                //buffer.append('G');
            	MergeFileEntry mergeEntry = new MergeFileEntry(path, MergeFileStatus.PROPERTY_MERGED, false);
            	mergeEntryList.add(mergeEntry);
            } else if (event.getPropertiesStatus() == SVNStatusType.CHANGED){
                //buffer.append('U');
            	MergeFileEntry mergeEntry = new MergeFileEntry(path, MergeFileStatus.PROPERTY_UPDATED, false);
            	mergeEntryList.add(mergeEntry);
            } else {
                //buffer.append(' ');
            }
            if (event.getLockStatus() == SVNStatusType.LOCK_UNLOCKED) {
                //buffer.append('B');
            } else {
                //buffer.append(' ');
            }
        } else if (event.getAction() == SVNEventAction.TREE_CONFLICT) {
        	hasConflict = true;
        	MergeFileEntry mergeEntry = new MergeFileEntry(path, MergeFileStatus.TREE_CONFLICTED, false);
        	mergeEntry.setConflict(true);
        	mergeEntryList.add(mergeEntry);
            //buffer.append("   C ");
            //buffer.append(path);
            //buffer.append("\n");
        } else if (event.getAction() == SVNEventAction.UPDATE_SHADOWED_ADD) {
            //buffer.append("   A ");
            //buffer.append(path);
            //buffer.append("\n");
        } else if (event.getAction() == SVNEventAction.UPDATE_SHADOWED_UPDATE) {
            //buffer.append("   U ");
            //buffer.append(path);
            //buffer.append("\n");
        } else if (event.getAction() == SVNEventAction.UPDATE_SHADOWED_DELETE) {
            //buffer.append("   D ");
            //buffer.append(path);
            //buffer.append("\n");
        } else if (event.getAction() == SVNEventAction.ADD || event.getAction() == SVNEventAction.COPY) {
            if (SVNProperty.isBinaryMimeType(event.getMimeType())) {
                //buffer.append("A  (bin)  " + path + "\n");
            	MergeFileEntry mergeEntry = new MergeFileEntry(path, MergeFileStatus.ADDED, true);
            	mergeEntryList.add(mergeEntry);
            } else {
                //buffer.append("A         " + path + "\n");
            	MergeFileEntry mergeEntry = new MergeFileEntry(path, MergeFileStatus.ADDED, true);
            	mergeEntryList.add(mergeEntry);
            }
        } else if (event.getAction() == SVNEventAction.DELETE) {
            //buffer.append("D         " + path + "\n");
        	MergeFileEntry mergeEntry = new MergeFileEntry(path, MergeFileStatus.DELETED, true);
        	mergeEntryList.add(mergeEntry);
        } else if (event.getAction() == SVNEventAction.CHANGELIST_SET) {
            //buffer.append("A [" + event.getChangelistName() + "] " + path + "\n");
        } else if (event.getAction() == SVNEventAction.CHANGELIST_CLEAR) {
            //buffer.append("D [" + event.getChangelistName() + "] " + path + "\n");
        } else if (event.getAction() == SVNEventAction.CHANGELIST_MOVED) {
            return;
        } else if (event.getAction() == SVNEventAction.PATCH) {

            if (event.getContentsStatus() == SVNStatusType.CONFLICTED) {
                //buffer.append('C');
            	hasConflict = true;
            	MergeFileEntry mergeEntry = new MergeFileEntry(path, MergeFileStatus.CONFLICTED, false);
            	mergeEntry.setConflict(true);
            	mergeEntryList.add(mergeEntry);
            } else if (event.getNodeKind() == SVNNodeKind.FILE) {
                if (event.getContentsStatus() == SVNStatusType.MERGED) {
                    //buffer.append('G');
                	MergeFileEntry mergeEntry = new MergeFileEntry(path, MergeFileStatus.MERGED, false);
                	mergeEntryList.add(mergeEntry);
                } else if (event.getContentsStatus() == SVNStatusType.CHANGED) {
                    //buffer.append('U');
                	MergeFileEntry mergeEntry = new MergeFileEntry(path, MergeFileStatus.UPDATED, false);
                	mergeEntryList.add(mergeEntry);
                }
            }

            if (event.getPropertiesStatus() == SVNStatusType.CONFLICTED) {
                //buffer.append('C');
            	hasConflict = true;
            	MergeFileEntry mergeEntry = new MergeFileEntry(path, MergeFileStatus.PROPERTY_CONFLICTED, false);
            	mergeEntry.setConflict(true);
                mergeEntryList.add(mergeEntry);
            } else if (event.getPropertiesStatus() == SVNStatusType.CHANGED) {
            	MergeFileEntry mergeEntry = new MergeFileEntry(path, MergeFileStatus.PROPERTY_UPDATED, false);
            	mergeEntryList.add(mergeEntry);
                //buffer.append('U');
            }
        } 
	}

}
