package com.smartspend;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter(urlPatterns = {
        "/dashboard", "/expenses", "/expenses/*", "/budgets", "/budgets/*",
        "/goals", "/goals/*", "/reminders", "/reminders/*",
        "/planner", "/planner/*", "/reports", "/reports/*",
        "/db-test"
})
public class AuthFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);

        if (session == null || session.getAttribute("loggedInUser") == null) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login?required=1");
            return;
        }
        chain.doFilter(request, response);
    }
}
