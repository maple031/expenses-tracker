package com.smartspend;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Expense {
    private final long id;
    private final long userId;
    private final String description;
    private final BigDecimal amount;
    private final String category;
    private final LocalDate expenseDate;

    public Expense(long id, long userId, String description, BigDecimal amount,
                   String category, LocalDate expenseDate) {
        this.id = id;
        this.userId = userId;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.expenseDate = expenseDate;
    }

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }
}
