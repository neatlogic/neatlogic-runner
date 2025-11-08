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
package com.neatlogic.autoexecrunner.exception.tagent;

import com.neatlogic.autoexecrunner.exception.core.ApiRuntimeException;

/**
 * @author longrf
 * @date 2022/10/31 14:43
 */

public class TagentNotFoundChannelAndReloadFieldException extends ApiRuntimeException {
    public TagentNotFoundChannelAndReloadFieldException(String tagentKey) {
        super("重启时" + "不存在 tagent：" + tagentKey + " 的心跳，因此无法通过心跳重启，尝试使用密码连接tagent重启，但也没有成功");
    }
}
