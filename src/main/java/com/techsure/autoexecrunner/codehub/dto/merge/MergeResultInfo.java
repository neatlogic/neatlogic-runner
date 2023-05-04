/*
 * Copyright (C) 2020, wenhb@techsure.com.cn
 */
package com.techsure.autoexecrunner.codehub.dto.merge;

import java.util.List;

public class MergeResultInfo {
    private boolean isConflict = false;
    private List<MergeFileEntry> mergeFileEntrys = null;
    private String error;
    private String summary;
    
    
    public boolean isConflict() {
        return isConflict;
    }

    public void setConflict(boolean isConflict) {
        this.isConflict = isConflict;
    }

    public List<MergeFileEntry> getMergeFileEntrys() {
        return mergeFileEntrys;
    }

    public void setMergeFileEntrys(List<MergeFileEntry> mergeFileEntrys) {
        this.mergeFileEntrys = mergeFileEntrys;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
}
