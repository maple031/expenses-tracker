package com.smartspend;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter(urlPatterns = {
        "/login", "/register", "/logout",
        "/expenses", "/expenses/*",
        "/budgets", "/budgets/*",
        "/goals", "/goals/*",
        "/reminders", "/reminders/*",
        "/planner", "/planner/*"
})
public class CsrfFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String method = httpRequest.getMethod();
        if ("POST".equalsIgnoreCase(method)) {
            if (!CsrfUtil.isValid(httpRequest)) {
                httpResponse.sendError(
                        HttpServletResponse.SC_FORBIDDEN,
                        "Invalid or missing CSRF token.");
                return;
            }
        } else if (!"GET".equalsIgnoreCase(method)
                && !"HEAD".equalsIgnoreCase(method)
                && !"OPTIONS".equalsIgnoreCase(method)) {
            httpResponse.setHeader("Allow", "GET, HEAD, POST, OPTIONS");
            httpResponse.sendError(
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    "Method not allowed.");
            return;
        }

        CsrfUtil.getToken(httpRequest);
        chain.doFilter(request, response);
    }
}
