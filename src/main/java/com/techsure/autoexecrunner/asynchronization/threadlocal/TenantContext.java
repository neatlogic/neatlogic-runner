/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package com.techsure.autoexecrunner.asynchronization.threadlocal;


import com.techsure.autoexecrunner.common.RootConfiguration;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@RootConfiguration
public class TenantContext implements Serializable {
    private static final long serialVersionUID = -5977938340288247600L;
    private static final ThreadLocal<TenantContext> instance = new ThreadLocal<>();
    private String tenantUuid;
    private Boolean useDefaultDatasource = false;
    private Boolean isData = false;

    public static TenantContext init() {
        TenantContext context = new TenantContext();
        instance.set(context);
        return context;
    }

    public static TenantContext init(TenantContext _tenantContext) {
        TenantContext context = new TenantContext();
        if (_tenantContext != null) {
            context.setTenantUuid(_tenantContext.getTenantUuid());
        }
        instance.set(context);
        return context;
    }

    public static TenantContext init(String _tenantUuid) {
        TenantContext context = new TenantContext(_tenantUuid);
        instance.set(context);
        return context;
    }

    public TenantContext() {

    }

    private TenantContext(String _tenantUuid) {
        this.tenantUuid = _tenantUuid;
    }

    public String getTenantUuid() {
        if (useDefaultDatasource) {
            return null;
        } else {
            return tenantUuid;
        }
    }

    public String getDataDbName() {
        return "neatlogic_" + tenantUuid + "_data";
    }

    public String getDbName() {
        return "neatlogic_" + tenantUuid;
    }

    private TenantContext setTenantUuid(String tenantUuid) {
        this.tenantUuid = tenantUuid;
        return this;
    }

    public void switchDataDatabase() {
        this.isData = true;
    }

    public void switchDefaultDatabase() {
        this.isData = false;
    }

    public Boolean isData() {
        return this.isData;
    }

    public static TenantContext get() {
        return instance.get();
    }

    public void release() {
        instance.remove();
    }

    public Boolean getUseDefaultDatasource() {
        return useDefaultDatasource;
    }

    public void setUseDefaultDatasource(Boolean useDefaultDatasource) {
        this.useDefaultDatasource = useDefaultDatasource;
    }
    public void switchTenant(String tenantUuid) {
        if (StringUtils.isNotBlank(tenantUuid)) {
            this.tenantUuid = tenantUuid;
        }
    }
}
