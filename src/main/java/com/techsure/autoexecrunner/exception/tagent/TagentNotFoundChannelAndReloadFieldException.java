/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package com.techsure.autoexecrunner.exception.tagent;

import com.techsure.autoexecrunner.exception.core.ApiRuntimeException;

/**
 * @author longrf
 * @date 2022/10/31 14:43
 */

public class TagentNotFoundChannelAndReloadFieldException extends ApiRuntimeException {
    public TagentNotFoundChannelAndReloadFieldException(String tagentKey) {
        super("重启时" + "不存在 tagent：" + tagentKey + " 的心跳，因此无法通过心跳重启，尝试使用密码连接tagent重启，但也没有成功");
    }
}
