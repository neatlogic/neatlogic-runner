package com.techsure.autoexecproxy.dto;

import java.util.List;

/**
 * @author lvzk
 * @since 2021/4/21 17:27
 **/
public class CommandVo {
    private Long jobId;
    private String jobPhaseUk;
    private String execUser;
    List<String> commandList;
    Boolean isCancel;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getJobPhaseUk() {
        return jobPhaseUk;
    }

    public void setJobPhaseUk(String jobPhaseUk) {
        this.jobPhaseUk = jobPhaseUk;
    }

    public String getExecUser() {
        return execUser;
    }

    public void setExecUser(String execUser) {
        this.execUser = execUser;
    }

    public List<String> getCommandList() {
        return commandList;
    }

    public void setCommandList(List<String> commandList) {
        this.commandList = commandList;
    }

    public Boolean getCancel() {
        return isCancel;
    }

    public void setCancel(Boolean cancel) {
        isCancel = cancel;
    }
}
