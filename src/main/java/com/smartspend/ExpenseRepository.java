package com.smartspend;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ExpenseRepository {
    public boolean create(long userId, String description, BigDecimal amount,
                          String category, LocalDate date) throws SQLException {
        String sql = "INSERT INTO expenses (user_id, title, amount, category, expense_date) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, description);
            statement.setBigDecimal(3, amount);
            statement.setString(4, category);
            statement.setDate(5, Date.valueOf(date));
            return statement.executeUpdate() == 1;
        }
    }

    public Optional<Expense> findOwnedBy(long expenseId, long userId) throws SQLException {
        String sql = "SELECT expense_id, user_id, title, amount, category, expense_date "
                + "FROM expenses WHERE expense_id = ? AND user_id = ?";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, expenseId);
            statement.setLong(2, userId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapExpense(result));
                }
            }
        }
        return Optional.empty();
    }

    public boolean update(long expenseId, long userId, String description, BigDecimal amount,
                          String category, LocalDate date) throws SQLException {
        String sql = "UPDATE expenses SET title = ?, amount = ?, category = ?, expense_date = ? "
                + "WHERE expense_id = ? AND user_id = ?";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, description);
            statement.setBigDecimal(2, amount);
            statement.setString(3, category);
            statement.setDate(4, Date.valueOf(date));
            statement.setLong(5, expenseId);
            statement.setLong(6, userId);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean delete(long expenseId, long userId) throws SQLException {
        String sql = "DELETE FROM expenses WHERE expense_id = ? AND user_id = ?";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, expenseId);
            statement.setLong(2, userId);
            return statement.executeUpdate() == 1;
        }
    }

    public List<Expense> findForUser(long userId, String search, String category,
                                     LocalDate dateFrom, LocalDate dateTo, YearMonth month)
            throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT expense_id, user_id, title, amount, category, expense_date "
                        + "FROM expenses WHERE user_id = ?");
        List<Object> parameters = new ArrayList<>();
        parameters.add(userId);

        if (search != null && !search.isBlank()) {
            sql.append(" AND (title LIKE ? OR COALESCE(notes, '') LIKE ?)");
            String pattern = "%" + search.trim() + "%";
            parameters.add(pattern);
            parameters.add(pattern);
        }
        if (category != null && !category.isBlank()) {
            sql.append(" AND category = ?");
            parameters.add(category);
        }
        if (dateFrom != null) {
            sql.append(" AND expense_date >= ?");
            parameters.add(Date.valueOf(dateFrom));
        }
        if (dateTo != null) {
            sql.append(" AND expense_date <= ?");
            parameters.add(Date.valueOf(dateTo));
        }
        if (month != null) {
            sql.append(" AND expense_date >= ? AND expense_date < ?");
            parameters.add(Date.valueOf(month.atDay(1)));
            parameters.add(Date.valueOf(month.plusMonths(1).atDay(1)));
        }
        sql.append(" ORDER BY expense_date DESC, expense_id DESC");

        List<Expense> expenses = new ArrayList<>();
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                statement.setObject(i + 1, parameters.get(i));
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    expenses.add(mapExpense(result));
                }
            }
        }
        return expenses;
    }

    public Summary getCurrentMonthSummary(long userId) throws SQLException {
        YearMonth currentMonth = YearMonth.now();
        LocalDate firstDay = currentMonth.atDay(1);
        LocalDate nextMonth = currentMonth.plusMonths(1).atDay(1);

        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        BigDecimal highest = BigDecimal.ZERO;
        Map<String, BigDecimal> categoryTotals = new LinkedHashMap<>();

        String summarySql = "SELECT COUNT(*) AS transaction_count, "
                + "COALESCE(SUM(amount), 0) AS total_amount, "
                + "COALESCE(MAX(amount), 0) AS highest_amount "
                + "FROM expenses WHERE user_id = ? AND expense_date >= ? AND expense_date < ?";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(summarySql)) {
            statement.setLong(1, userId);
            statement.setDate(2, Date.valueOf(firstDay));
            statement.setDate(3, Date.valueOf(nextMonth));
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    count = result.getInt("transaction_count");
                    total = result.getBigDecimal("total_amount");
                    highest = result.getBigDecimal("highest_amount");
                }
            }
        }

        String categorySql = "SELECT category, SUM(amount) AS category_total FROM expenses "
                + "WHERE user_id = ? AND expense_date >= ? AND expense_date < ? "
                + "GROUP BY category ORDER BY category_total DESC";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(categorySql)) {
            statement.setLong(1, userId);
            statement.setDate(2, Date.valueOf(firstDay));
            statement.setDate(3, Date.valueOf(nextMonth));
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    categoryTotals.put(result.getString("category"), result.getBigDecimal("category_total"));
                }
            }
        }
        return new Summary(total, count, highest, categoryTotals);
    }

    private Expense mapExpense(ResultSet result) throws SQLException {
        Date date = result.getDate("expense_date");
        return new Expense(
                result.getLong("expense_id"),
                result.getLong("user_id"),
                result.getString("title"),
                result.getBigDecimal("amount"),
                result.getString("category"),
                date.toLocalDate());
    }

    public record Summary(BigDecimal total, int count, BigDecimal highest,
                          Map<String, BigDecimal> categoryTotals) {}
}
