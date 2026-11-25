package com.smartspend;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SmartPlan {
    private final long id;
    private final long userId;
    private final String activityType;
    private final String activityName;
    private final BigDecimal estimatedCost;
    private final LocalDate plannedDate;
    private final String description;
    private final boolean affordable;
    private final BigDecimal remainingAfter;
    private final boolean mayExceedBudget;
    private final BigDecimal suggestedLimit;
    private final BigDecimal suggestedMonthlySavings;
    private final String recommendationStatus;
    private final String recommendationText;
    private final String alternatives;

    public SmartPlan(long id, long userId, String activityType, String activityName,
                     BigDecimal estimatedCost, LocalDate plannedDate, String description,
                     boolean affordable, BigDecimal remainingAfter, boolean mayExceedBudget,
                     BigDecimal suggestedLimit, BigDecimal suggestedMonthlySavings,
                     String recommendationStatus, String recommendationText, String alternatives) {
        this.id = id;
        this.userId = userId;
        this.activityType = activityType;
        this.activityName = activityName;
        this.estimatedCost = estimatedCost;
        this.plannedDate = plannedDate;
        this.description = description;
        this.affordable = affordable;
        this.remainingAfter = remainingAfter;
        this.mayExceedBudget = mayExceedBudget;
        this.suggestedLimit = suggestedLimit;
        this.suggestedMonthlySavings = suggestedMonthlySavings;
        this.recommendationStatus = recommendationStatus;
        this.recommendationText = recommendationText;
        this.alternatives = alternatives;
    }

    public long getId() { return id; }
    public long getUserId() { return userId; }
    public String getActivityType() { return activityType; }
    public String getActivityName() { return activityName; }
    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public LocalDate getPlannedDate() { return plannedDate; }
    public String getDescription() { return description; }
    public boolean isAffordable() { return affordable; }
    public BigDecimal getRemainingAfter() { return remainingAfter; }
    public BigDecimal getShortfall() {
        if (remainingAfter.compareTo(BigDecimal.ZERO) >= 0) {
            return BigDecimal.ZERO;
        }
        return remainingAfter.abs();
    }
    public boolean isMayExceedBudget() { return mayExceedBudget; }
    public BigDecimal getSuggestedLimit() { return suggestedLimit; }
    public BigDecimal getSuggestedMonthlySavings() { return suggestedMonthlySavings; }
    public String getRecommendationStatus() { return recommendationStatus; }
    public String getRecommendationText() { return recommendationText; }
    public String getAlternatives() { return alternatives; }
}
