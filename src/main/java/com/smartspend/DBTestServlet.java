package com.smartspend;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet("/db-test")
public class DBTestServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        try (Connection connection = DB.getConnection()) {
            response.getWriter().println("SUCCESS: SmartSpend connected to MySQL.");
        } catch (SQLException e) {
            AppLogger.logDatabaseError("Database diagnostic connection", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("Database connection failed. Check MySQL and your settings.");
        }
    }
}
