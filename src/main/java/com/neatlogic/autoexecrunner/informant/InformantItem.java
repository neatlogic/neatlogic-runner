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
