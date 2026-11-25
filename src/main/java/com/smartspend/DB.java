
package com.smartspend;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DB {
    private DB() {}

    public static Connection getConnection() throws SQLException {
        String url = System.getenv().getOrDefault("SMARTSPEND_DB_URL",
                "jdbc:mysql://localhost:3306/smartspend?useSSL=false&serverTimezone=Asia/Kolkata");
        String user = System.getenv().getOrDefault("SMARTSPEND_DB_USER", "root");
        String password = System.getenv().getOrDefault("SMARTSPEND_DB_PASSWORD", "");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found!", e);
        }

        return DriverManager.getConnection(url, user, password);
    }
}

