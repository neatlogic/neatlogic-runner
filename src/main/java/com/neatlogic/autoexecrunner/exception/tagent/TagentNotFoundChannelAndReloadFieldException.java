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
