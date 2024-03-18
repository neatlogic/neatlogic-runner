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

package com.neatlogic.autoexecrunner.exception;

import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

public class TenantNotFoundException extends ApiRuntimeException {

    public TenantNotFoundException(String tenant) {
        super("租户：" + tenant + "不存在");
    }

    public TenantNotFoundException() {
        super("检测不到租户信息，无法进行下一步操作");
    }

    public TenantNotFoundException(Long tenantId) {
        super("租户id：" + tenantId + "不存在");
    }
}
