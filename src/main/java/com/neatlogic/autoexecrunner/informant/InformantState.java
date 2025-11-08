/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
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
