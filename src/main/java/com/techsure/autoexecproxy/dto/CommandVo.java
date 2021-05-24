package com.techsure.autoexecproxy.dto;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @author lvzk
 * @since 2021/4/21 17:27
 **/
public class CommandVo {
    private String jobId;
    private String execUser;
    private String action;
    private String tenant;
    private String config;

    List<String> commandList;
    Boolean isCancel;

    public CommandVo() {
    }

    public CommandVo(JSONObject jsonObj) {
        this.jobId = jsonObj.getString("jobId");
        this.tenant = jsonObj.getString("tenant");
        this.execUser = jsonObj.getString("execUser");
        this.config = jsonObj.toJSONString();
    }

    public String getJobId() {
        if(StringUtils.isNotBlank(config)){
            JSONObject configJson = JSONObject.parseObject(config);
            return  configJson.getString("jobId");
        }
        return jobId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }
}
