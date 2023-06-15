/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
