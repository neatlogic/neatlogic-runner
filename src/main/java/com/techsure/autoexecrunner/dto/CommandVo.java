package com.techsure.autoexecrunner.dto;

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
    private Boolean isFirstFire;//执行判断是不是执行第一个phase
    private Boolean noFireNext;//执行判断是不是 执行完当前phase后 无需激活下一个phase
    private Integer exitValue;//执行结果
    private JSONObject passThroughEnv;

    List<String> commandList;
    Boolean isCancel;

    public CommandVo() {
    }

    public CommandVo(JSONObject jsonObj) {
        this.jobId = jsonObj.getString("jobId");
        this.tenant = jsonObj.getString("tenant");
        this.execUser = jsonObj.getString("execUser");
        Integer isFirstFireTmp = jsonObj.getInteger("isFirstFire");
        if (isFirstFireTmp != null) {
            this.isFirstFire = isFirstFireTmp == 1;
        }
        Integer noFireNextTmp = jsonObj.getInteger("noFireNext");
        if (noFireNextTmp != null) {
            this.noFireNext = noFireNextTmp == 1;
        }
        this.passThroughEnv = jsonObj.getJSONObject("passThroughEnv");
        this.config = jsonObj.toJSONString();
    }

    public String getJobId() {
        if (StringUtils.isNotBlank(config)) {
            JSONObject configJson = JSONObject.parseObject(config);
            return configJson.getString("jobId");
        }
        return jobId;
    }

    public Boolean getFirstFire() {
        return isFirstFire;
    }

    public Boolean getNoFireNext() {
        return noFireNext;
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

    public Integer getExitValue() {
        return exitValue;
    }

    public void setExitValue(Integer exitValue) {
        this.exitValue = exitValue;
    }

    public JSONObject getPassThroughEnv() {
        return passThroughEnv;
    }

    public void setPassThroughEnv(JSONObject passThroughEnv) {
        this.passThroughEnv = passThroughEnv;
    }
}