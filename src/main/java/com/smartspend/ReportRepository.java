package com.smartspend;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ReportRepository {
    public ReportData getReport(long userId, LocalDate startDate, LocalDate endDate)
            throws SQLException {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Invalid report date range.");
        }

        String detailSql = "SELECT expense_id, title, amount, category, expense_date "
                + "FROM expenses WHERE user_id = ? "
                + "AND expense_date >= ? AND expense_date <= ? "
                + "ORDER BY expense_date DESC, expense_id DESC";
        List<Expense> expenses = new ArrayList<>();
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(detailSql)) {
            statement.setLong(1, userId);
            statement.setDate(2, Date.valueOf(startDate));
            statement.setDate(3, Date.valueOf(endDate));
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    expenses.add(new Expense(
                            result.getLong("expense_id"),
                            userId,
                            result.getString("title"),
                            result.getBigDecimal("amount"),
                            result.getString("category"),
                            result.getDate("expense_date").toLocalDate()));
                }
            }
        }

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal highest = BigDecimal.ZERO;
        Map<String, BigDecimal> categoryTotals = new LinkedHashMap<>();
        for (Expense expense : expenses) {
            total = total.add(expense.getAmount());
            if (expense.getAmount().compareTo(highest) > 0) {
                highest = expense.getAmount();
            }
            categoryTotals.merge(expense.getCategory(), expense.getAmount(), BigDecimal::add);
        }

        Map<String, BigDecimal> categoryPercentages = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : categoryTotals.entrySet()) {
            BigDecimal percentage = total.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : entry.getValue().multiply(BigDecimal.valueOf(100))
                            .divide(total, 2, RoundingMode.HALF_UP);
            categoryPercentages.put(entry.getKey(), percentage);
        }

        return new ReportData(startDate, endDate, total,
                expenses.size(), highest, categoryTotals, categoryPercentages, expenses);
    }

    public record ReportData(LocalDate startDate, LocalDate endDate, BigDecimal total,
                             int transactionCount, BigDecimal highestExpense,
                             Map<String, BigDecimal> categoryTotals,
                             Map<String, BigDecimal> categoryPercentages,
                             List<Expense> expenses) {}
}
