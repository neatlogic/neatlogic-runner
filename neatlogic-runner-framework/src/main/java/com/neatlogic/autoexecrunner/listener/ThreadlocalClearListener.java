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

package com.neatlogic.autoexecrunner.listener;


import com.neatlogic.autoexecrunner.asynchronization.threadlocal.RequestContext;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.TenantContext;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.UserContext;

import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.annotation.WebListener;

/** 在容器复用线程前释放本次请求的上下文。 */
@WebListener
public class ThreadlocalClearListener implements ServletRequestListener {
    /** 按请求生命周期清理语言、租户和用户信息。 */
    @Override
    public void requestDestroyed(ServletRequestEvent event) {
        if (RequestContext.get() != null) {
            RequestContext.get().release();
        }
        if (TenantContext.get() != null) {
            TenantContext.get().release();
        }
        if (UserContext.get() != null) {
            UserContext.get().release();
        }
    }

    @Override
    public void requestInitialized(ServletRequestEvent event) {
        //必须实现方法 否则遇到低版本的javaee-api 会报错
    }
}
