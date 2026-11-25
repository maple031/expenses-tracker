package com.smartspend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public final class UserRepository {
    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    public Optional<User> createUser(String fullName, String email, String passwordHash) throws SQLException {
        String sql = "INSERT INTO users (full_name, email, password_hash) VALUES (?, ?, ?)";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, fullName);
            statement.setString(2, email);
            statement.setString(3, passwordHash);
            int affected = statement.executeUpdate();
            if (affected != 1) {
                return Optional.empty();
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return Optional.of(new User(keys.getLong(1), fullName, email));
                }
            }
            return Optional.empty();
        }
    }

    public Optional<LoginDetails> findByEmail(String email) throws SQLException {
        String sql = "SELECT user_id, full_name, email, password_hash FROM users WHERE email = ?";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                User user = new User(
                        result.getLong("user_id"),
                        result.getString("full_name"),
                        result.getString("email"));
                return Optional.of(new LoginDetails(user, result.getString("password_hash")));
            }
        }
    }

    public record LoginDetails(User user, String passwordHash) {}
}
