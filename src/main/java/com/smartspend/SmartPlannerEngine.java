package com.smartspend;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public final class SmartPlannerEngine {
    public SmartPlanRepository.SmartRecommendation analyze(
            long userId, String activityType, BigDecimal cost, LocalDate plannedDate)
            throws SQLException {
        BudgetRepository.BudgetSummary budget =
                new BudgetRepository().getSummary(userId, YearMonth.now());
        ExpenseRepository.Summary expenses =
                new ExpenseRepository().getCurrentMonthSummary(userId);
        ReminderRepository.ReminderSummary reminderSummary =
                new ReminderRepository().getSummary(userId);
        SavingsGoalRepository.SavingsSummary savings =
                new SavingsGoalRepository().getSummary(userId);

        BigDecimal remaining = budget.remaining();
        BigDecimal remainingAfter = remaining.subtract(cost);

        BigDecimal totalAfterActivity = budget.spent().add(cost);
        boolean mayExceed = budget.limit().compareTo(BigDecimal.ZERO) > 0
                && totalAfterActivity.compareTo(budget.limit()) > 0;
        boolean affordable = remaining.compareTo(cost) >= 0 && !mayExceed;
        String status;
        if (mayExceed) status = "WARNING";
        else if (remaining.compareTo(cost) >= 0
                && remaining.subtract(cost).compareTo(remaining.multiply(new BigDecimal("0.20"))) <= 0) {
            status = "CAUTION";
        } else status = "AFFORDABLE";

        BigDecimal suggestedLimit = remaining.multiply(new BigDecimal("0.80"));
        if (suggestedLimit.compareTo(cost) > 0) suggestedLimit = cost;
        if (suggestedLimit.compareTo(BigDecimal.ZERO) < 0) suggestedLimit = BigDecimal.ZERO;
        suggestedLimit = suggestedLimit.setScale(2, RoundingMode.HALF_UP);

        BigDecimal deficit = cost.subtract(remaining);
        if (deficit.compareTo(BigDecimal.ZERO) < 0) deficit = BigDecimal.ZERO;
        long monthsUntilActivity = 1;
        if (plannedDate != null && plannedDate.isAfter(LocalDate.now())) {
            monthsUntilActivity = ChronoUnit.MONTHS.between(
                    YearMonth.now().atDay(1), YearMonth.from(plannedDate).atDay(1)) + 1;
            if (monthsUntilActivity < 1) monthsUntilActivity = 1;
        }
        BigDecimal suggestedMonthlySavings = deficit.divide(
                BigDecimal.valueOf(monthsUntilActivity), 2, RoundingMode.HALF_UP);

        List<String> suggestions = new ArrayList<>();
        if ("Travel".equalsIgnoreCase(activityType)) {
            suggestions.add("Consider a lower-cost destination or shorter trip.");
            suggestions.add("Reduce accommodation expenses and compare flexible dates.");
            suggestions.add("Prefer public transport where suitable.");
        } else if ("Shopping".equalsIgnoreCase(activityType)) {
            suggestions.add("Compare options and choose a lower-cost alternative.");
            suggestions.add("Postpone the purchase until the monthly budget has more room.");
            suggestions.add("Check whether the purchase can be funded from an existing savings goal.");
        } else if ("Entertainment".equalsIgnoreCase(activityType)) {
            suggestions.add("Choose a free or lower-cost entertainment option.");
            suggestions.add("Set a personal spending cap before booking.");
        } else if ("Education".equalsIgnoreCase(activityType)) {
            suggestions.add("Look for discounts, used materials or lower-cost learning alternatives.");
        } else {
            suggestions.add("Break the activity into smaller lower-cost options.");
            suggestions.add("Postpone discretionary spending until the budget has more room.");
        }

        String message;
        if (mayExceed) {
            message = "This activity may exceed the current monthly budget."
                    + " Actual shortfall: \u20b9" + deficit.setScale(2).toPlainString() + ".";
        } else if (!affordable) {
            message = "This activity is not currently affordable from the remaining budget."
                    + " Actual shortfall: \u20b9" + deficit.setScale(2).toPlainString() + ".";
        } else if ("CAUTION".equals(status)) {
            message = "The activity is affordable, but it will use most of the remaining budget.";
        } else {
            message = "The activity fits within the current monthly budget.";
        }

        String context = " Current month's budget: \u20b9"
                + budget.limit().setScale(2).toPlainString()
                + "; expenses: \u20b9" + expenses.total().setScale(2).toPlainString()
                + "; remaining budget: \u20b9" + budget.remaining().setScale(2).toPlainString()
                + "; upcoming/today reminders: "
                + (reminderSummary.upcomingCount() + reminderSummary.todayCount())
                + "; active savings goals: " + savings.activeCount() + ".";

        String alternatives = String.join(" ", suggestions);
        return new SmartPlanRepository.SmartRecommendation(
                affordable,
                remainingAfter,
                mayExceed,
                suggestedLimit,
                suggestedMonthlySavings,
                status,
                message + context,
                alternatives);
    }
}
