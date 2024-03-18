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
