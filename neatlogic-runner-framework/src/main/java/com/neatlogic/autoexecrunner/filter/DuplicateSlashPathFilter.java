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

package com.neatlogic.autoexecrunner.filter;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

public class DuplicateSlashPathFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest) || httpRequest.getDispatcherType() != DispatcherType.REQUEST) {
            chain.doFilter(request, response);
            return;
        }

        String requestUri = httpRequest.getRequestURI();
        String normalizedUri = requestUri.replaceAll("/{2,}", "/");
        if (requestUri.equals(normalizedUri)) {
            chain.doFilter(request, response);
            return;
        }

        String contextPath = httpRequest.getContextPath();
        String dispatchPath = normalizedUri;
        if (dispatchPath.startsWith(contextPath)) {
            dispatchPath = dispatchPath.substring(contextPath.length());
        }
        if (dispatchPath.isEmpty()) {
            dispatchPath = "/";
        }

        String queryString = httpRequest.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            dispatchPath = dispatchPath + "?" + queryString;
        }

        // Spring 6 不再匹配双斜杠路径，进入 MVC 映射前先兼容旧调用方拼出的 URL。
        request.getRequestDispatcher(dispatchPath).forward(request, response);
    }
}
