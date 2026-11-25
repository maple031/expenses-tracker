
package com.smartspend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DB {
    private DB() {}

    public static Connection getConnection() throws SQLException {
        String url = trim(System.getenv("SMARTSPEND_DB_URL"));
        String user = trim(System.getenv("SMARTSPEND_DB_USER"));
        String password = System.getenv("SMARTSPEND_DB_PASSWORD");

        if (url.isEmpty()) {
            throw configurationError("SMARTSPEND_DB_URL");
        }
        if (user.isEmpty()) {
            throw configurationError("SMARTSPEND_DB_USER");
        }
        if (password == null || password.isBlank()) {
            throw configurationError("SMARTSPEND_DB_PASSWORD");
        }
        if (!url.startsWith("jdbc:mysql://")) {
            throw new SQLException(
                    "Database configuration error: SMARTSPEND_DB_URL must be a valid MySQL JDBC URL.");
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            AppLogger.logUnexpectedError("Loading MySQL JDBC driver", e);
            throw new SQLException("Database driver is unavailable.", e);
        }

        return DriverManager.getConnection(url, user, password);
    }

    private static SQLException configurationError(String variableName) {
        return new SQLException(
                "Database configuration error: required environment variable "
                        + variableName + " is missing.");
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}

