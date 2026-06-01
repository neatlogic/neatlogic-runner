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
