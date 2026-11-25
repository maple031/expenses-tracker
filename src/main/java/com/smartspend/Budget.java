package com.smartspend;

import java.math.BigDecimal;
import java.time.YearMonth;

public class Budget {
    private final long id;
    private final long userId;
    private final YearMonth month;
    private final BigDecimal monthlyLimit;

    public Budget(long id, long userId, YearMonth month, BigDecimal monthlyLimit) {
        this.id = id;
        this.userId = userId;
        this.month = month;
        this.monthlyLimit = monthlyLimit;
    }

    public long getId() {
        return id;
    }

    public long getUserId() {
        return userId;
    }

    public YearMonth getMonth() {
        return month;
    }

    public BigDecimal getMonthlyLimit() {
        return monthlyLimit;
    }
}
