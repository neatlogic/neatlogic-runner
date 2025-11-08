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
package com.neatlogic.autoexecrunner.codehub.dto.merge;

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
