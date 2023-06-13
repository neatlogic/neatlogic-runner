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
