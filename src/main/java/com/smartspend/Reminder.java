package com.smartspend;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class Reminder {
    private final long id;
    private final long userId;
    private final String title;
    private final String description;
    private final BigDecimal expectedAmount;
    private final String category;
    private final LocalDate reminderDate;
    private final LocalTime reminderTime;
    private final boolean completed;

    public Reminder(long id, long userId, String title, String description,
                    BigDecimal expectedAmount, String category,
                    LocalDate reminderDate, LocalTime reminderTime, boolean completed) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.expectedAmount = expectedAmount;
        this.category = category;
        this.reminderDate = reminderDate;
        this.reminderTime = reminderTime;
        this.completed = completed;
    }

    public long getId() { return id; }
    public long getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public BigDecimal getExpectedAmount() { return expectedAmount; }
    public String getCategory() { return category; }
    public LocalDate getReminderDate() { return reminderDate; }
    public LocalTime getReminderTime() { return reminderTime; }
    public boolean isCompleted() { return completed; }

    public String getStatus() {
        if (completed) return "COMPLETED";
        LocalDate today = LocalDate.now();
        if (reminderDate.isBefore(today)) return "PAST";
        if (reminderDate.isEqual(today)) return "TODAY";
        return "UPCOMING";
    }

    public boolean isDue() {
        if (completed) return false;
        LocalDate today = LocalDate.now();
        if (reminderDate.isBefore(today)) return true;
        if (!reminderDate.isEqual(today)) return false;
        return reminderTime == null || !reminderTime.isAfter(java.time.LocalTime.now());
    }
}
