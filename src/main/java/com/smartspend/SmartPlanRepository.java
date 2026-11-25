package com.smartspend;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SmartPlanRepository {
    public SmartPlanRepository() throws SQLException {
        ensureTable();
    }

    public boolean create(long userId, String activityType, String activityName,
                          BigDecimal estimatedCost, LocalDate plannedDate, String description,
                          SmartRecommendation recommendation) throws SQLException {
        String sql = "INSERT INTO smart_plans "
                + "(user_id, activity_type, activity_name, estimated_cost, planned_date, description, "
                + "affordable, remaining_after, may_exceed_budget, suggested_limit, "
                + "suggested_monthly_savings, recommendation_status, recommendation_text, alternatives) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, activityType);
            statement.setString(3, activityName);
            statement.setBigDecimal(4, estimatedCost);
            statement.setDate(5, Date.valueOf(plannedDate));
            statement.setString(6, description);
            statement.setBoolean(7, recommendation.affordable());
            statement.setBigDecimal(8, recommendation.remainingAfter());
            statement.setBoolean(9, recommendation.mayExceedBudget());
            statement.setBigDecimal(10, recommendation.suggestedLimit());
            statement.setBigDecimal(11, recommendation.suggestedMonthlySavings());
            statement.setString(12, recommendation.status());
            statement.setString(13, recommendation.message());
            statement.setString(14, recommendation.alternatives());
            return statement.executeUpdate() == 1;
        }
    }

    public boolean update(long planId, long userId, String activityType, String activityName,
                          BigDecimal estimatedCost, LocalDate plannedDate, String description,
                          SmartRecommendation recommendation) throws SQLException {
        String sql = "UPDATE smart_plans SET activity_type = ?, activity_name = ?, estimated_cost = ?, "
                + "planned_date = ?, description = ?, affordable = ?, remaining_after = ?, "
                + "may_exceed_budget = ?, suggested_limit = ?, suggested_monthly_savings = ?, "
                + "recommendation_status = ?, recommendation_text = ?, alternatives = ? "
                + "WHERE plan_id = ? AND user_id = ?";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, activityType);
            statement.setString(2, activityName);
            statement.setBigDecimal(3, estimatedCost);
            statement.setDate(4, Date.valueOf(plannedDate));
            statement.setString(5, description);
            statement.setBoolean(6, recommendation.affordable());
            statement.setBigDecimal(7, recommendation.remainingAfter());
            statement.setBoolean(8, recommendation.mayExceedBudget());
            statement.setBigDecimal(9, recommendation.suggestedLimit());
            statement.setBigDecimal(10, recommendation.suggestedMonthlySavings());
            statement.setString(11, recommendation.status());
            statement.setString(12, recommendation.message());
            statement.setString(13, recommendation.alternatives());
            statement.setLong(14, planId);
            statement.setLong(15, userId);
            return statement.executeUpdate() == 1;
        }
    }

    public Optional<SmartPlan> findOwnedBy(long planId, long userId) throws SQLException {
        String sql = "SELECT * FROM smart_plans WHERE plan_id = ? AND user_id = ?";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, planId);
            statement.setLong(2, userId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return Optional.of(mapPlan(result));
            }
        }
        return Optional.empty();
    }

    public List<SmartPlan> findForUser(long userId) throws SQLException {
        String sql = "SELECT * FROM smart_plans WHERE user_id = ? "
                + "ORDER BY planned_date ASC, plan_id DESC";
        List<SmartPlan> plans = new ArrayList<>();
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) plans.add(mapPlan(result));
            }
        }
        return plans;
    }

    public boolean delete(long planId, long userId) throws SQLException {
        String sql = "DELETE FROM smart_plans WHERE plan_id = ? AND user_id = ?";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, planId);
            statement.setLong(2, userId);
            return statement.executeUpdate() == 1;
        }
    }

    private SmartPlan mapPlan(ResultSet result) throws SQLException {
        Date date = result.getDate("planned_date");
        return new SmartPlan(
                result.getLong("plan_id"),
                result.getLong("user_id"),
                result.getString("activity_type"),
                result.getString("activity_name"),
                result.getBigDecimal("estimated_cost"),
                date.toLocalDate(),
                result.getString("description"),
                result.getBoolean("affordable"),
                result.getBigDecimal("remaining_after"),
                result.getBoolean("may_exceed_budget"),
                result.getBigDecimal("suggested_limit"),
                result.getBigDecimal("suggested_monthly_savings"),
                result.getString("recommendation_status"),
                result.getString("recommendation_text"),
                result.getString("alternatives"));
    }

    private void ensureTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS smart_plans ("
                + "plan_id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                + "user_id BIGINT NOT NULL,"
                + "activity_type VARCHAR(60) NOT NULL,"
                + "activity_name VARCHAR(150) NOT NULL,"
                + "estimated_cost DECIMAL(12,2) NOT NULL,"
                + "planned_date DATE NOT NULL,"
                + "description VARCHAR(500),"
                + "affordable BOOLEAN NOT NULL DEFAULT TRUE,"
                + "remaining_after DECIMAL(12,2) NOT NULL DEFAULT 0,"
                + "may_exceed_budget BOOLEAN NOT NULL DEFAULT FALSE,"
                + "suggested_limit DECIMAL(12,2) NOT NULL DEFAULT 0,"
                + "suggested_monthly_savings DECIMAL(12,2) NOT NULL DEFAULT 0,"
                + "recommendation_status VARCHAR(20) NOT NULL DEFAULT 'AFFORDABLE',"
                + "recommendation_text TEXT NOT NULL,"
                + "alternatives TEXT,"
                + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                + "CONSTRAINT fk_plans_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,"
                + "CONSTRAINT chk_plan_cost CHECK (estimated_cost > 0),"
                + "INDEX idx_plans_user_date (user_id, planned_date))";
        try (Connection connection = DB.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    public record SmartRecommendation(boolean affordable, BigDecimal remainingAfter,
                                      boolean mayExceedBudget, BigDecimal suggestedLimit,
                                      BigDecimal suggestedMonthlySavings, String status,
                                      String message, String alternatives) {}
}
