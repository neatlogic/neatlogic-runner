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

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public class InformantItem implements Delayed {
    private final long expiredTime;
    private final InformantState informantState;

    public InformantItem(InformantState informantState) {
        this.expiredTime = informantState.getExpiredTime();
        this.informantState = informantState;
    }

    public InformantState getInformantState() {
        return informantState;
    }


    @Override
    public long getDelay(TimeUnit unit) {
        long diff = expiredTime - System.currentTimeMillis();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        return Long.compare(this.expiredTime, ((InformantItem) o).expiredTime);
    }

}
