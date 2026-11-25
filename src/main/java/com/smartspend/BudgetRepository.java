package com.smartspend;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Optional;

public final class BudgetRepository {
    public Optional<Budget> find(long userId, YearMonth month) throws SQLException {
        String sql = "SELECT budget_id, user_id, budget_month, monthly_limit "
                + "FROM budgets WHERE user_id = ? AND budget_month = ?";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setDate(2, Date.valueOf(month.atDay(1)));
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    YearMonth budgetMonth = YearMonth.from(result.getDate("budget_month").toLocalDate());
                    return Optional.of(new Budget(
                            result.getLong("budget_id"),
                            result.getLong("user_id"),
                            budgetMonth,
                            result.getBigDecimal("monthly_limit")));
                }
            }
        }
        return Optional.empty();
    }

    public boolean save(long userId, YearMonth month, BigDecimal amount) throws SQLException {
        Optional<Budget> existing = find(userId, month);
        if (existing.isPresent()) {
            String sql = "UPDATE budgets SET monthly_limit = ? "
                    + "WHERE budget_id = ? AND user_id = ?";
            try (Connection connection = DB.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setBigDecimal(1, amount);
                statement.setLong(2, existing.get().getId());
                statement.setLong(3, userId);
                return statement.executeUpdate() == 1;
            }
        }

        String sql = "INSERT INTO budgets (user_id, budget_month, monthly_limit) VALUES (?, ?, ?)";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setDate(2, Date.valueOf(month.atDay(1)));
            statement.setBigDecimal(3, amount);
            return statement.executeUpdate() == 1;
        }
    }

    public BudgetSummary getSummary(long userId, YearMonth month) throws SQLException {
        BigDecimal limit = BigDecimal.ZERO;
        Optional<Budget> budget = find(userId, month);
        if (budget.isPresent()) {
            limit = budget.get().getMonthlyLimit();
        }

        BigDecimal spent = BigDecimal.ZERO;
        LocalDate firstDay = month.atDay(1);
        LocalDate nextMonth = month.plusMonths(1).atDay(1);
        String sql = "SELECT COALESCE(SUM(amount), 0) AS spent "
                + "FROM expenses WHERE user_id = ? AND expense_date >= ? AND expense_date < ?";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setDate(2, Date.valueOf(firstDay));
            statement.setDate(3, Date.valueOf(nextMonth));
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    spent = result.getBigDecimal("spent");
                }
            }
        }

        BigDecimal remaining = limit.subtract(spent);
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }
        BigDecimal percentage = BigDecimal.ZERO;
        if (limit.compareTo(BigDecimal.ZERO) > 0) {
            percentage = spent.multiply(BigDecimal.valueOf(100))
                    .divide(limit, 2, RoundingMode.HALF_UP);
        }

        String status;
        String statusMessage;
        String statusClass;
        if (percentage.compareTo(BigDecimal.valueOf(100)) >= 0) {
            status = "EXCEEDED";
            statusMessage = "Budget exceeded. Reduce spending or increase the monthly budget.";
            statusClass = "exceeded";
        } else if (percentage.compareTo(BigDecimal.valueOf(80)) >= 0) {
            status = "WARNING";
            statusMessage = "Warning: you have used at least 80% of this month's budget.";
            statusClass = "warning";
        } else {
            status = "NORMAL";
            statusMessage = "Your spending is within the normal budget range.";
            statusClass = "normal";
        }

        if (limit.compareTo(BigDecimal.ZERO) > 0) {
            createBudgetAlert(userId, month, percentage);
        }

        return new BudgetSummary(
                budget.map(Budget::getMonthlyLimit).orElse(BigDecimal.ZERO),
                spent, remaining, percentage, status, statusMessage, statusClass);
    }

    private void createBudgetAlert(long userId, YearMonth month, BigDecimal percentage)
            throws SQLException {
        if (percentage.compareTo(BigDecimal.valueOf(80)) < 0) {
            return;
        }

        String monthName = month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        String text;
        if (percentage.compareTo(BigDecimal.valueOf(100)) >= 0) {
            text = "Budget exceeded for " + monthName + " " + month.getYear() + ".";
        } else {
            text = "Budget warning: 80% reached for " + monthName + " " + month.getYear() + ".";
        }

        boolean alreadyExists;
        String existsSql = "SELECT 1 FROM reminders "
                + "WHERE user_id = ? AND reminder_text = ? LIMIT 1";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(existsSql)) {
            statement.setLong(1, userId);
            statement.setString(2, text);
            try (ResultSet result = statement.executeQuery()) {
                alreadyExists = result.next();
            }
        }

        if (!alreadyExists) {
            String insert = "INSERT INTO reminders "
                    + "(user_id, reminder_text, reminder_date, is_done, reminder_kind) "
                    + "VALUES (?, ?, ?, FALSE, 'BUDGET')";
            try (Connection connection = DB.getConnection();
                 PreparedStatement statement = connection.prepareStatement(insert)) {
                statement.setLong(1, userId);
                statement.setString(2, text);
                statement.setDate(3, Date.valueOf(LocalDate.now()));
                statement.executeUpdate();
            }
        }
    }

    public record BudgetSummary(BigDecimal limit, BigDecimal spent, BigDecimal remaining,
                                BigDecimal percentage, String status, String statusMessage,
                                String statusClass) {}
}
