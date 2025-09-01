/*
 * Copyright (C) 2025  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.neatlogic.autoexecrunner.informant;

import java.util.concurrent.TimeUnit;

public class InformantState {
    private final String uuid;
    private long expiredTime;
    private String tenant;

    public InformantState(String uuid, String tenant, long delay, TimeUnit unit) {
        this.uuid = uuid;
        this.tenant = tenant;
        this.expiredTime = System.currentTimeMillis() + unit.toMillis(delay);
    }

    public String getTenant() {
        return tenant;
    }

    public void setExpiredTime(long delay, TimeUnit unit) {
        this.expiredTime = System.currentTimeMillis() + unit.toMillis(delay);
    }

    public String getUuid() {
        return uuid;
    }


    public long getExpiredTime() {
        return expiredTime;
    }


}
