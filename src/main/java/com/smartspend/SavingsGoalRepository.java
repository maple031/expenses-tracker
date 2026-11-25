package com.smartspend;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SavingsGoalRepository {
    public SavingsGoalRepository() throws SQLException {
        ensureGoalColumns();
    }

    public boolean create(long userId, String name, BigDecimal targetAmount,
                          BigDecimal initialSaved, LocalDate targetDate,
                          String description, String category) throws SQLException {
        String sql = "INSERT INTO savings_goals "
                + "(user_id, goal_name, target_amount, saved_amount, target_date, description, category) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, name);
            statement.setBigDecimal(3, targetAmount);
            statement.setBigDecimal(4, initialSaved);
            statement.setDate(5, Date.valueOf(targetDate));
            statement.setString(6, description);
            statement.setString(7, category);
            return statement.executeUpdate() == 1;
        }
    }

    public List<SavingsGoal> findForUser(long userId) throws SQLException {
        String sql = "SELECT goal_id, user_id, goal_name, target_amount, saved_amount, "
                + "target_date, description, category FROM savings_goals "
                + "WHERE user_id = ? ORDER BY target_date ASC, goal_id DESC";
        List<SavingsGoal> goals = new ArrayList<>();
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    goals.add(mapGoal(result));
                }
            }
        }
        return goals;
    }

    public Optional<SavingsGoal> findOwnedBy(long goalId, long userId) throws SQLException {
        String sql = "SELECT goal_id, user_id, goal_name, target_amount, saved_amount, "
                + "target_date, description, category FROM savings_goals "
                + "WHERE goal_id = ? AND user_id = ?";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, goalId);
            statement.setLong(2, userId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapGoal(result));
                }
            }
        }
        return Optional.empty();
    }

    public boolean update(long goalId, long userId, String name, BigDecimal targetAmount,
                          LocalDate targetDate, String description, String category) throws SQLException {
        Optional<SavingsGoal> existing = findOwnedBy(goalId, userId);
        if (existing.isEmpty() || targetAmount.compareTo(existing.get().getSavedAmount()) < 0) {
            return false;
        }
        String sql = "UPDATE savings_goals SET goal_name = ?, target_amount = ?, "
                + "target_date = ?, description = ?, category = ? "
                + "WHERE goal_id = ? AND user_id = ?";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setBigDecimal(2, targetAmount);
            statement.setDate(3, Date.valueOf(targetDate));
            statement.setString(4, description);
            statement.setString(5, category);
            statement.setLong(6, goalId);
            statement.setLong(7, userId);
            return statement.executeUpdate() == 1;
        }
    }

    public String addSavings(long goalId, long userId, BigDecimal amount) throws SQLException {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return "Savings amount must be greater than zero.";
        }
        Optional<SavingsGoal> existing = findOwnedBy(goalId, userId);
        if (existing.isEmpty()) {
            return "This savings goal could not be found.";
        }
        SavingsGoal goal = existing.get();
        BigDecimal newSaved = goal.getSavedAmount().add(amount);
        if (newSaved.compareTo(goal.getTargetAmount()) > 0) {
            return "Savings cannot exceed the target amount.";
        }

        String sql = "UPDATE savings_goals SET saved_amount = ? "
                + "WHERE goal_id = ? AND user_id = ? "
                + "AND saved_amount = ? AND ? <= target_amount";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBigDecimal(1, newSaved);
            statement.setLong(2, goalId);
            statement.setLong(3, userId);
            statement.setBigDecimal(4, goal.getSavedAmount());
            statement.setBigDecimal(5, newSaved);
            if (statement.executeUpdate() == 1) {
                return null;
            }
        }
        return "The savings goal changed. Please try again.";
    }

    public boolean delete(long goalId, long userId) throws SQLException {
        String sql = "DELETE FROM savings_goals WHERE goal_id = ? AND user_id = ?";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, goalId);
            statement.setLong(2, userId);
            return statement.executeUpdate() == 1;
        }
    }

    public SavingsSummary getSummary(long userId) throws SQLException {
        String sql = "SELECT COALESCE(SUM(CASE WHEN saved_amount < target_amount THEN 1 ELSE 0 END), 0) AS active_count, "
                + "COALESCE(SUM(saved_amount), 0) AS total_saved, "
                + "COALESCE(SUM(target_amount), 0) AS total_target "
                + "FROM savings_goals WHERE user_id = ?";
        BigDecimal saved = BigDecimal.ZERO;
        BigDecimal target = BigDecimal.ZERO;
        int activeCount = 0;
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    activeCount = result.getInt("active_count");
                    saved = result.getBigDecimal("total_saved");
                    target = result.getBigDecimal("total_target");
                }
            }
        }

        List<SavingsGoal> recent = new ArrayList<>();
        String recentSql = "SELECT goal_id, user_id, goal_name, target_amount, saved_amount, "
                + "target_date, description, category FROM savings_goals "
                + "WHERE user_id = ? ORDER BY updated_at DESC, goal_id DESC LIMIT 5";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(recentSql)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    recent.add(mapGoal(result));
                }
            }
        }

        BigDecimal overallProgress = BigDecimal.ZERO;
        if (target.compareTo(BigDecimal.ZERO) > 0) {
            overallProgress = saved.multiply(BigDecimal.valueOf(100))
                    .divide(target, 2, java.math.RoundingMode.HALF_UP);
        }
        return new SavingsSummary(activeCount, saved, target, recent, overallProgress);
    }

    private void ensureGoalColumns() throws SQLException {
        ensureColumn("description", "VARCHAR(500)");
        ensureColumn("category", "VARCHAR(60) NOT NULL DEFAULT 'Other'");
        ensureColumn("updated_at",
                "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
    }

    private void ensureColumn(String columnName, String definition) throws SQLException {
        String check = "SELECT 1 FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = 'savings_goals' "
                + "AND column_name = ?";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(check)) {
            statement.setString(1, columnName);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return;
                }
            }
        }
        try (Connection connection = DB.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE savings_goals ADD COLUMN " + columnName + " " + definition);
        }
    }

    private SavingsGoal mapGoal(ResultSet result) throws SQLException {
        Date date = result.getDate("target_date");
        return new SavingsGoal(
                result.getLong("goal_id"),
                result.getLong("user_id"),
                result.getString("goal_name"),
                result.getBigDecimal("target_amount"),
                result.getBigDecimal("saved_amount"),
                date == null ? null : date.toLocalDate(),
                result.getString("description"),
                result.getString("category"));
    }

    public record SavingsSummary(int activeCount, BigDecimal savedAmount,
                                 BigDecimal targetAmount, List<SavingsGoal> recentGoals,
                                 BigDecimal overallProgress) {}
}
