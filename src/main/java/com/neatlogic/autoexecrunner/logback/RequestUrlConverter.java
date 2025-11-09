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

package com.neatlogic.autoexecrunner.logback;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.neatlogic.autoexecrunner.asynchronization.threadlocal.RequestContext;

public class RequestUrlConverter extends ClassicConverter {
    /**
     * The convert method is responsible for extracting data from the event and
     * storing it for later use by the write method.
     *
     * @param event
     */
    @Override
    public String convert(ILoggingEvent event) {
        RequestContext requestContext = RequestContext.get();
        if (requestContext != null) {
            return requestContext.getUrl();
        }
        return "";
    }
}
