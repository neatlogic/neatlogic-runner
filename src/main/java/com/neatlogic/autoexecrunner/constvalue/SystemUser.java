package com.neatlogic.autoexecrunner.constvalue;


import com.neatlogic.autoexecrunner.asynchronization.threadlocal.TenantContext;
import com.neatlogic.autoexecrunner.common.config.Config;
import com.neatlogic.autoexecrunner.dto.UserVo;

import java.util.Objects;

/**
 * @ClassName: SystemUser
 * @Description: sla转交策略的定时作业执行转交逻辑时，需要验证权限，system用户拥有流程流转的所有权限
 */
public enum SystemUser {
    SYSTEM("system", "system", "系统"),
    ANONYMOUS("anonymous", "anonymous", "匿名用户"),
    AUTOEXEC("autoexec", "autoexec", "自动化用户");
    private final String userId;
    private final String userUuid;
    private final String userName;
    private final String timezone = "+8:00";

    SystemUser(String userId, String userUuid, String userName) {
        this.userId = userId;
        this.userUuid = userUuid;
        this.userName = userName;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public String getUserName() {
        return userName;
    }

    public String getTimezone() {
        return timezone;
    }

    public UserVo getUserVo() {
        UserVo userVo = new UserVo();
        userVo.setUuid(userUuid);
        userVo.setUserId(userId);
        userVo.setUserName(userName);
        userVo.setIsDelete(0);
        userVo.setIsActive(1);
        userVo.setTenant(TenantContext.get() != null ? TenantContext.get().getTenantUuid() : null);
        return userVo;
    }

    public String getToken() {
        if (Objects.equals(userId, AUTOEXEC.getUserId())) {
            return Config.AUTOEXEC_TOKEN();
        }
        return null;
    }

    public static String getUserName(String userUuid) {
        for (SystemUser user : values()) {
            if (user.getUserUuid().equals(userUuid)) {
                return user.getUserName();
            }
        }
        return "";
    }
}
