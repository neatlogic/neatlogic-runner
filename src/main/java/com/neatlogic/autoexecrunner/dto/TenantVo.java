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

package com.neatlogic.autoexecrunner.dto;


import com.neatlogic.autoexecrunner.constvalue.ApiParamType;
import com.neatlogic.autoexecrunner.restful.annotation.EntityField;

import java.util.Date;

public class TenantVo  {
    private static final long serialVersionUID = 5037087043421533431L;
    @EntityField(name = "uuid", type = ApiParamType.STRING)
    private String uuid;
    @EntityField(name = "common.name", type = ApiParamType.STRING)
    private String name;
    @EntityField(name = "common.isactive", type = ApiParamType.INTEGER)
    private Integer isActive;
    @EntityField(name = "common.description", type = ApiParamType.STRING)
    private String description;
    @EntityField(name = "nfd.tenantvo.expiredate", type = ApiParamType.LONG)
    private Date expireDate;
    @EntityField(name = "common.status", type = ApiParamType.STRING)
    private String status;
    @EntityField(name = "nfd.tenantvo.visittime", type = ApiParamType.LONG)
    private Date visitTime;
    @EntityField(name = "创建租户时是否需要demo数据", type = ApiParamType.INTEGER)
    private Integer isNeedDemo;

    public TenantVo(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(Date expireDate) {
        this.expireDate = expireDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getVisitTime() {
        return visitTime;
    }

    public void setVisitTime(Date visitTime) {
        this.visitTime = visitTime;
    }

    public Integer getIsNeedDemo() {
        return isNeedDemo;
    }

    public void setIsNeedDemo(Integer isNeedDemo) {
        this.isNeedDemo = isNeedDemo;
    }

}
