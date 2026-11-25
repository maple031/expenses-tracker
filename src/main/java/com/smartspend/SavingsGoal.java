package com.smartspend;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class SavingsGoal {
    private final long id;
    private final long userId;
    private final String name;
    private final BigDecimal targetAmount;
    private final BigDecimal savedAmount;
    private final LocalDate targetDate;
    private final String description;
    private final String category;

    public SavingsGoal(long id, long userId, String name, BigDecimal targetAmount,
                       BigDecimal savedAmount, LocalDate targetDate,
                       String description, String category) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.targetAmount = targetAmount;
        this.savedAmount = savedAmount;
        this.targetDate = targetDate;
        this.description = description;
        this.category = category;
    }

    public long getId() { return id; }
    public long getUserId() { return userId; }
    public String getName() { return name; }
    public BigDecimal getTargetAmount() { return targetAmount; }
    public BigDecimal getSavedAmount() { return savedAmount; }
    public LocalDate getTargetDate() { return targetDate; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }

    public BigDecimal getRemainingAmount() {
        BigDecimal remaining = targetAmount.subtract(savedAmount);
        return remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining;
    }

    public BigDecimal getProgressPercentage() {
        if (targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return savedAmount.multiply(BigDecimal.valueOf(100))
                .divide(targetAmount, 2, RoundingMode.HALF_UP);
    }

    public String getStatus() {
        if (savedAmount.compareTo(targetAmount) >= 0) {
            return "COMPLETED";
        }
        if (targetDate != null && targetDate.isBefore(LocalDate.now())) {
            return "OVERDUE";
        }
        return "ACTIVE";
    }
}
