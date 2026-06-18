package com.neatlogic.autoexecrunner.filter;

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
        if (!(request instanceof HttpServletRequest httpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }

        String requestUri = httpServletRequest.getRequestURI();
        String normalizedUri = requestUri.replaceAll("/{2,}", "/");
        if (requestUri.equals(normalizedUri)) {
            chain.doFilter(request, response);
            return;
        }

        String contextPath = httpServletRequest.getContextPath();
        String dispatchPath = normalizedUri;
        if (dispatchPath.startsWith(contextPath)) {
            dispatchPath = dispatchPath.substring(contextPath.length());
        }
        if (dispatchPath.isEmpty()) {
            dispatchPath = "/";
        }

        String queryString = httpServletRequest.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            dispatchPath = dispatchPath + "?" + queryString;
        }

        // Spring 6 不再匹配双斜杠路径，进入 MVC 映射前先兼容旧调用方拼出的 URL。
        request.getRequestDispatcher(dispatchPath).forward(request, response);
    }
}
