package com.smartspend;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ReminderRepository {
    public ReminderRepository() throws SQLException {
        ensureColumns();
    }

    public boolean create(long userId, String title, String description,
                          BigDecimal expectedAmount, String category,
                          LocalDate date, LocalTime time) throws SQLException {
        String sql = "INSERT INTO reminders "
                + "(user_id, reminder_text, reminder_date, is_done, title, description, "
                + "expected_amount, category, reminder_time, reminder_kind) "
                + "VALUES (?, ?, ?, FALSE, ?, ?, ?, ?, ?, 'MANUAL')";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setString(2, title);
            statement.setDate(3, Date.valueOf(date));
            statement.setString(4, title);
            statement.setString(5, description);
            if (expectedAmount == null) statement.setNull(6, java.sql.Types.DECIMAL);
            else statement.setBigDecimal(6, expectedAmount);
            statement.setString(7, category);
            if (time == null) statement.setNull(8, java.sql.Types.TIME);
            else statement.setTime(8, Time.valueOf(time));
            return statement.executeUpdate() == 1;
        }
    }

    public Optional<Reminder> findOwnedBy(long reminderId, long userId) throws SQLException {
        String sql = "SELECT reminder_id, user_id, reminder_text, title, description, "
                + "expected_amount, category, reminder_date, reminder_time, is_done "
                + "FROM reminders WHERE reminder_id = ? AND user_id = ? "
                + "AND (reminder_kind = 'MANUAL' OR title IS NOT NULL)";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, reminderId);
            statement.setLong(2, userId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return Optional.of(mapReminder(result));
            }
        }
        return Optional.empty();
    }

    public List<Reminder> findForUser(long userId, String search, String category,
                                      LocalDate date, String status) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT reminder_id, user_id, reminder_text, title, description, "
                        + "expected_amount, category, reminder_date, reminder_time, is_done "
                        + "FROM reminders WHERE user_id = ? "
                        + "AND reminder_kind = 'MANUAL'");
        List<Object> parameters = new ArrayList<>();
        parameters.add(userId);

        if (search != null && !search.isBlank()) {
            sql.append(" AND (COALESCE(title, reminder_text) LIKE ? "
                    + "OR COALESCE(description, '') LIKE ?)");
            String pattern = "%" + search.trim() + "%";
            parameters.add(pattern);
            parameters.add(pattern);
        }
        if (category != null && !category.isBlank()) {
            sql.append(" AND category = ?");
            parameters.add(category);
        }
        if (date != null) {
            sql.append(" AND reminder_date = ?");
            parameters.add(Date.valueOf(date));
        }
        sql.append(" ORDER BY is_done ASC, reminder_date ASC, "
                + "(reminder_time IS NULL) ASC, reminder_time ASC");

        List<Reminder> reminders = new ArrayList<>();
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                statement.setObject(i + 1, parameters.get(i));
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Reminder reminder = mapReminder(result);
                    if (status == null || status.isBlank() || matchesStatus(reminder, status)) {
                        reminders.add(reminder);
                    }
                }
            }
        }
        return reminders;
    }

    public boolean update(long reminderId, long userId, String title, String description,
                          BigDecimal expectedAmount, String category,
                          LocalDate date, LocalTime time) throws SQLException {
        String sql = "UPDATE reminders SET reminder_text = ?, title = ?, description = ?, "
                + "expected_amount = ?, category = ?, reminder_date = ?, reminder_time = ? "
                + "WHERE reminder_id = ? AND user_id = ? AND reminder_kind = 'MANUAL'";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title);
            statement.setString(2, title);
            statement.setString(3, description);
            if (expectedAmount == null) statement.setNull(4, java.sql.Types.DECIMAL);
            else statement.setBigDecimal(4, expectedAmount);
            statement.setString(5, category);
            statement.setDate(6, Date.valueOf(date));
            if (time == null) statement.setNull(7, java.sql.Types.TIME);
            else statement.setTime(7, Time.valueOf(time));
            statement.setLong(8, reminderId);
            statement.setLong(9, userId);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean markCompleted(long reminderId, long userId) throws SQLException {
        String sql = "UPDATE reminders SET is_done = TRUE "
                + "WHERE reminder_id = ? AND user_id = ? AND reminder_kind = 'MANUAL'";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, reminderId);
            statement.setLong(2, userId);
            return statement.executeUpdate() == 1;
        }
    }

    public boolean delete(long reminderId, long userId) throws SQLException {
        String sql = "DELETE FROM reminders "
                + "WHERE reminder_id = ? AND user_id = ? AND reminder_kind = 'MANUAL'";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, reminderId);
            statement.setLong(2, userId);
            return statement.executeUpdate() == 1;
        }
    }

    public ReminderSummary getSummary(long userId) throws SQLException {
        List<Reminder> all = findForUser(userId, null, null, null, null);
        LocalDate today = LocalDate.now();
        int upcoming = 0;
        int todayCount = 0;
        int completed = 0;
        int due = 0;
        Reminder next = null;
        for (Reminder reminder : all) {
            if (reminder.isCompleted()) completed++;
            else if (reminder.getReminderDate().isBefore(today)) due++;
            else if (reminder.getReminderDate().isEqual(today)) {
                todayCount++;
                if (reminder.isDue()) due++;
            } else {
                upcoming++;
                if (next == null
                        || reminder.getReminderDate().isBefore(next.getReminderDate())
                        || (reminder.getReminderDate().isEqual(next.getReminderDate())
                        && compareTime(reminder, next) < 0)) {
                    next = reminder;
                }
            }
        }
        return new ReminderSummary(upcoming, todayCount, completed, due, next);
    }

    private int compareTime(Reminder left, Reminder right) {
        LocalTime leftTime = left.getReminderTime() == null ? LocalTime.MAX : left.getReminderTime();
        LocalTime rightTime = right.getReminderTime() == null ? LocalTime.MAX : right.getReminderTime();
        return leftTime.compareTo(rightTime);
    }

    private boolean matchesStatus(Reminder reminder, String status) {
        return switch (status) {
            case "UPCOMING" -> "UPCOMING".equals(reminder.getStatus());
            case "TODAY" -> "TODAY".equals(reminder.getStatus());
            case "PAST" -> "PAST".equals(reminder.getStatus());
            case "COMPLETED" -> reminder.isCompleted();
            default -> true;
        };
    }

    private Reminder mapReminder(ResultSet result) throws SQLException {
        Date date = result.getDate("reminder_date");
        Time time = result.getTime("reminder_time");
        String title = result.getString("title");
        if (title == null || title.isBlank()) title = result.getString("reminder_text");
        BigDecimal amount = result.getBigDecimal("expected_amount");
        return new Reminder(
                result.getLong("reminder_id"),
                result.getLong("user_id"),
                title,
                result.getString("description"),
                amount,
                result.getString("category"),
                date.toLocalDate(),
                time == null ? null : time.toLocalTime(),
                result.getBoolean("is_done"));
    }

    private void ensureColumns() throws SQLException {
        ensureColumn("title", "VARCHAR(150)");
        ensureColumn("description", "VARCHAR(500)");
        ensureColumn("expected_amount", "DECIMAL(12,2)");
        ensureColumn("category", "VARCHAR(60)");
        ensureColumn("reminder_time", "TIME");
        ensureColumn("reminder_kind", "VARCHAR(20) NOT NULL DEFAULT 'MANUAL'");
        ensureColumn("updated_at",
                "TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
        try (Connection connection = DB.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "UPDATE reminders SET reminder_kind = 'BUDGET' "
                            + "WHERE title IS NULL AND reminder_text LIKE 'Budget %'");
        }
    }

    private void ensureColumn(String columnName, String definition) throws SQLException {
        String check = "SELECT 1 FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = 'reminders' "
                + "AND column_name = ?";
        try (Connection connection = DB.getConnection();
             PreparedStatement statement = connection.prepareStatement(check)) {
            statement.setString(1, columnName);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) return;
            }
        }
        try (Connection connection = DB.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE reminders ADD COLUMN " + columnName + " " + definition);
        }
    }

    public record ReminderSummary(int upcomingCount, int todayCount,
                                  int completedCount, int dueCount,
                                  Reminder nextReminder) {}
}
