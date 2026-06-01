package com.neatlogic.autoexecrunner.dto;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.UserContext;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.util.JobUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;

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
    private JSONObject passThroughEnv;//web端传到runner贯穿autoexec 回调web端会携带该变量
    private List<String> jobPhaseNameList;//需要执行的phaseNameList
    private List<Long> jobPhaseResourceIdList;//需要执行的resourceIdList
    private List<Integer> jobGroupSortList;//需要执行的组
    private JSONArray jobPhaseNodeSqlList;
    private JSONObject environment;//设置环境变量
    private Date fcd;

    private String consoleLogPath;

    List<String> commandList;
    Boolean isCancel;

    private String execid; //

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }

    public CommandVo() {
    }

    public CommandVo(JSONObject jsonObj) {
        this.jobId = jsonObj.getString("jobId");
        this.tenant = jsonObj.getString("tenant");
        this.execUser = UserContext.get().getUserUuid();
        Integer isFirstFireTmp = jsonObj.getInteger("isFirstFire");
        if (isFirstFireTmp != null) {
            this.isFirstFire = isFirstFireTmp == 1;
        }
        Integer noFireNextTmp = jsonObj.getInteger("isNoFireNext");
        if (noFireNextTmp != null) {
            this.noFireNext = noFireNextTmp == 1;
        }
        this.passThroughEnv = jsonObj.getJSONObject("passThroughEnv");
        this.config = jsonObj.toJSONString();

        JSONArray jobPhaseNameArray = jsonObj.getJSONArray("jobPhaseNameList");
        if (CollectionUtils.isNotEmpty(jobPhaseNameArray)) {
            this.jobPhaseNameList = jobPhaseNameArray.toJavaList(String.class);
        }

        JSONArray jobPhaseResourceIdArray = jsonObj.getJSONArray("jobPhaseResourceIdList");
        if (CollectionUtils.isNotEmpty(jobPhaseResourceIdArray)) {
            this.jobPhaseResourceIdList = jobPhaseResourceIdArray.toJavaList(Long.class);
        }
        JSONArray jobGroupIdArray = jsonObj.getJSONArray("jobGroupSortList");
        if (CollectionUtils.isNotEmpty(jobGroupIdArray)) {
            this.jobGroupSortList = jobGroupIdArray.toJavaList(Integer.class);
        }

        JSONArray jobPhaseNodeSqlList = jsonObj.getJSONArray("jobPhaseNodeSqlList");
        if (CollectionUtils.isNotEmpty(jobPhaseNodeSqlList)) {
            List<String> needKeys = Arrays.asList("sqlFile", "nodeName", "nodeType", "resourceId", "host", "port", "accessEndpoint", "userName");
            for (int i = 0; i < jobPhaseNodeSqlList.size(); i++) {
                JSONObject jobPhaseNodeSql = jobPhaseNodeSqlList.getJSONObject(i);
                Iterator<Map.Entry<String, Object>> iterator = jobPhaseNodeSql.entrySet().iterator();
                while (iterator.hasNext()) {
                    String key = iterator.next().getKey();
                    if (!needKeys.contains(key)) {
                        iterator.remove();
                    }
                }
            }
            this.jobPhaseNodeSqlList = jobPhaseNodeSqlList;
        }

        this.execid = jsonObj.getString("execid");
    }

    public String getExecid() {
        return execid;
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

    public List<String> getJobPhaseNameList() {
        return jobPhaseNameList;
    }

    public List<Long> getJobPhaseResourceIdList() {
        return jobPhaseResourceIdList;
    }

    public List<Integer> getJobGroupSortList() {
        return jobGroupSortList;
    }

    public JSONArray getJobPhaseNodeSqlList() {
        if (CollectionUtils.isNotEmpty(jobPhaseNodeSqlList)) {
            for (int i = 0; i < jobPhaseNodeSqlList.size(); i++) {
                JSONObject jobPhaseNodeSql = jobPhaseNodeSqlList.getJSONObject(i);
                jobPhaseNodeSql.put("username", jobPhaseNodeSql.getString("userName"));
            }
        }
        return jobPhaseNodeSqlList;
    }

    public void setJobPhaseNodeSqlList(JSONArray jobPhaseNodeSqlList) {
        this.jobPhaseNodeSqlList = jobPhaseNodeSqlList;
    }

    public JSONObject getEnvironment() {
        return environment;
    }

    public void setEnvironment(JSONObject environment) {
        this.environment = environment;
    }

    public String getConsoleLogPath() {
        this.consoleLogPath = Config.AUTOEXEC_HOME() + File.separator + JobUtil.getJobPath(getJobId(), new StringBuilder()) + File.separator + "log" + File.separator + "console.txt";
        return consoleLogPath;
    }

    public Date getFcd() {
        return fcd;
    }

    public void setFcd(Date fcd) {
        this.fcd = fcd;
    }

    private String getFilteredCommandString() {
        if (commandList == null) return "";
        List<String> filtered = new ArrayList<>();
        Iterator<String> iterator = commandList.iterator();
        while (iterator.hasNext()) {
            String item = iterator.next();
            if ("--execid".equals(item)) {
                // 跳过 "--execid" 和它后面的那个参数
                if (iterator.hasNext()) iterator.next();
                continue;
            }
            filtered.add(item);
        }
        return String.join(",", filtered);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommandVo)) return false;
        CommandVo that = (CommandVo) o;
        return Objects.equals(getFilteredCommandString(), that.getFilteredCommandString());
    }

    @Override
    public int hashCode() {
        return getFilteredCommandString().hashCode();
    }
}
