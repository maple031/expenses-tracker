<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.smartspend.User" %>
<%@ page import="com.smartspend.HtmlUtil" %>
<%@ page import="com.smartspend.ExpenseRepository" %>
<%@ page import="com.smartspend.BudgetRepository" %>
<%@ page import="com.smartspend.SavingsGoalRepository" %>
<%@ page import="com.smartspend.SavingsGoal" %>
<%@ page import="com.smartspend.ReminderRepository" %>
<%@ page import="com.smartspend.SmartPlan" %>
<%@ page import="java.util.Map" %>
<%
  User user = (User) session.getAttribute("loggedInUser");
  ExpenseRepository.Summary expenseSummary =
      (ExpenseRepository.Summary) request.getAttribute("expenseSummary");
  BudgetRepository.BudgetSummary budgetSummary =
      (BudgetRepository.BudgetSummary) request.getAttribute("budgetSummary");
  java.math.BigDecimal progress = budgetSummary == null
      ? java.math.BigDecimal.ZERO : budgetSummary.percentage();
  java.math.BigDecimal visualProgress = progress.compareTo(java.math.BigDecimal.valueOf(100)) > 0
      ? java.math.BigDecimal.valueOf(100) : progress;
  SavingsGoalRepository.SavingsSummary savingsSummary =
      (SavingsGoalRepository.SavingsSummary) request.getAttribute("savingsSummary");
  ReminderRepository.ReminderSummary reminderSummary =
      (ReminderRepository.ReminderSummary) request.getAttribute("reminderSummary");
  Integer unseenReminderCount =
      (Integer) request.getAttribute("unseenReminderCount");
  if (unseenReminderCount == null) unseenReminderCount = 0;
  java.util.List<SmartPlan> savedPlans =
      (java.util.List<SmartPlan>) request.getAttribute("savedPlans");
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Dashboard | SmartSpend</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/style.css">
</head>
<body>
  <div class="app-shell">
    <aside class="sidebar">
      <a class="brand" href="<%= request.getContextPath() %>/">SmartSpend</a>
      <nav class="sidebar-nav" aria-label="Main navigation">
        <a class="active" href="<%= request.getContextPath() %>/dashboard">Dashboard</a>
        <a href="<%= request.getContextPath() %>/expenses">Expenses</a>
        <a href="<%= request.getContextPath() %>/budgets">Budgets</a>
        <a href="<%= request.getContextPath() %>/goals">Savings goals</a>
        <a href="<%= request.getContextPath() %>/reminders">
          Reminders
          <% if (unseenReminderCount > 0) { %>
            <span class="notification-badge"><%= unseenReminderCount %></span>
          <% } %>
        </a>
        <a href="<%= request.getContextPath() %>/planner">Smart Planner</a>
        <a href="<%= request.getContextPath() %>/reports">Reports</a>
      </nav>
      <form method="post" action="<%= request.getContextPath() %>/logout">
        <button class="button button-secondary button-small button-full" type="submit">Log out</button>
      </form>
    </aside>
    <main class="dashboard-main">
      <header class="page-header">
        <div>
          <p class="tag">YOUR DASHBOARD</p>
          <h1>Welcome, <span><%= HtmlUtil.escape(user.getName()) %></span></h1>
        </div>
        <a class="button" href="<%= request.getContextPath() %>/expenses">Manage expenses</a>
      </header>
      <% if (request.getAttribute("summaryError") != null) { %>
        <div class="alert alert-error"><%= HtmlUtil.escape(request.getAttribute("summaryError")) %></div>
      <% } %>
      <section class="summary-grid" aria-label="Monthly expense summary">
        <article class="summary-card">
          <span>This month's spending</span>
          <strong>&#8377;<%= expenseSummary == null ? "0.00" : expenseSummary.total().setScale(2).toPlainString() %></strong>
        </article>
        <article class="summary-card">
          <span>Transactions</span>
          <strong><%= expenseSummary == null ? 0 : expenseSummary.count() %></strong>
        </article>
        <article class="summary-card">
          <span>Highest expense</span>
          <strong>&#8377;<%= expenseSummary == null ? "0.00" : expenseSummary.highest().setScale(2).toPlainString() %></strong>
        </article>
      </section>
      <section class="budget-panel">
        <div class="section-heading">
          <div>
            <p class="tag">EXPENSE REMINDERS</p>
            <h2>Reminder summary</h2>
          </div>
          <a class="text-link" href="<%= request.getContextPath() %>/reminders">Manage reminders</a>
        </div>
        <% if (reminderSummary == null) { %>
          <p class="empty-state">Reminder summary is temporarily unavailable.</p>
        <% } else { %>
          <% if (unseenReminderCount > 0) { %>
            <div class="alert alert-warning">
              You have <strong><%= unseenReminderCount %></strong> due reminder(s).
              Review them in your reminder list.
            </div>
          <% } %>
          <div class="budget-overview-grid">
            <div><span>Upcoming</span><strong><%= reminderSummary.upcomingCount() %></strong></div>
            <div><span>Today</span><strong><%= reminderSummary.todayCount() %></strong></div>
            <div><span>Completed</span><strong><%= reminderSummary.completedCount() %></strong></div>
          </div>
          <% if (reminderSummary.nextReminder() != null) { %>
            <div class="category-row">
              <span>Next: <%= HtmlUtil.escape(reminderSummary.nextReminder().getTitle()) %></span>
              <strong><%= reminderSummary.nextReminder().getReminderDate() %>
                <%= reminderSummary.nextReminder().getReminderTime() == null
                    ? "" : " at " + reminderSummary.nextReminder().getReminderTime() %>
              </strong>
            </div>
          <% } else { %>
            <p class="empty-state">No upcoming reminders.</p>
          <% } %>
        <% } %>
      </section>
      <section class="budget-panel">
        <div class="section-heading">
          <div>
            <p class="tag">MONTHLY BUDGET</p>
            <h2>Budget overview</h2>
          </div>
          <a class="text-link" href="<%= request.getContextPath() %>/budgets">Manage budget</a>
        </div>
        <% if (budgetSummary == null || budgetSummary.limit().compareTo(java.math.BigDecimal.ZERO) == 0) { %>
          <p class="empty-state">No budget set for this month.</p>
          <a class="button" href="<%= request.getContextPath() %>/budgets">Set monthly budget</a>
        <% } else { %>
          <div class="budget-overview-grid">
            <div><span>Monthly budget</span><strong>&#8377;<%= budgetSummary.limit().setScale(2).toPlainString() %></strong></div>
            <div><span>Amount spent</span><strong>&#8377;<%= budgetSummary.spent().setScale(2).toPlainString() %></strong></div>
            <div><span>Remaining</span><strong>&#8377;<%= budgetSummary.remaining().setScale(2).toPlainString() %></strong></div>
          </div>
          <div class="progress-heading">
            <strong class="<%= budgetSummary.statusClass() %>"><%= budgetSummary.status() %></strong>
            <span><%= progress.setScale(2).toPlainString() %>% used</span>
          </div>
          <div class="progress-track" role="progressbar"
               aria-valuemin="0" aria-valuemax="100"
               aria-valuenow="<%= visualProgress.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() %>">
            <div class="progress-fill <%= budgetSummary.statusClass() %>"
                 style="width:<%= visualProgress.toPlainString() %>%"></div>
          </div>
          <p class="budget-message"><%= HtmlUtil.escape(budgetSummary.statusMessage()) %></p>
        <% } %>
      </section>
      <section class="budget-panel">
        <div class="section-heading">
          <div>
            <p class="tag">SAVINGS GOALS</p>
            <h2>Savings overview</h2>
          </div>
          <a class="text-link" href="<%= request.getContextPath() %>/goals">Manage goals</a>
        </div>
        <% if (savingsSummary == null) { %>
          <p class="empty-state">Savings summary is temporarily unavailable.</p>
        <% } else { %>
          <div class="budget-overview-grid">
            <div><span>Active goals</span><strong><%= savingsSummary.activeCount() %></strong></div>
            <div><span>Total saved</span><strong>&#8377;<%= savingsSummary.savedAmount().setScale(2).toPlainString() %></strong></div>
            <div><span>Total target</span><strong>&#8377;<%= savingsSummary.targetAmount().setScale(2).toPlainString() %></strong></div>
          </div>
          <div class="progress-heading">
            <strong class="normal">OVERALL PROGRESS</strong>
            <span><%= savingsSummary.overallProgress().setScale(2).toPlainString() %>%</span>
          </div>
          <div class="progress-track">
            <div class="progress-fill normal"
                 style="width:<%= savingsSummary.overallProgress().min(java.math.BigDecimal.valueOf(100)).toPlainString() %>%"></div>
          </div>
          <% if (!savingsSummary.recentGoals().isEmpty()) { %>
            <div class="recent-goals">
              <% for (SavingsGoal goal : savingsSummary.recentGoals()) { %>
                <div class="category-row">
                  <span><%= HtmlUtil.escape(goal.getName()) %></span>
                  <strong>&#8377;<%= goal.getSavedAmount().setScale(2).toPlainString() %> / &#8377;<%= goal.getTargetAmount().setScale(2).toPlainString() %></strong>
                </div>
              <% } %>
            </div>
          <% } %>
        <% } %>
      </section>
      <section class="budget-panel">
        <div class="section-heading">
          <div>
            <p class="tag">SMART PLANNER</p>
            <h2>Saved activities</h2>
          </div>
          <a class="text-link" href="<%= request.getContextPath() %>/planner">Open Smart Planner</a>
        </div>
        <% if (savedPlans == null || savedPlans.isEmpty()) { %>
          <p class="empty-state">No activity plans saved yet.</p>
          <a class="button" href="<%= request.getContextPath() %>/planner">Plan an activity</a>
        <% } else { %>
          <div class="recent-goals">
            <% for (SmartPlan plan : savedPlans) { %>
              <div class="category-row">
                <span><%= HtmlUtil.escape(plan.getActivityName()) %>
                  - <%= HtmlUtil.escape(plan.getRecommendationStatus()) %></span>
                <strong>&#8377;<%= plan.getEstimatedCost().setScale(2).toPlainString() %></strong>
              </div>
            <% } %>
          </div>
        <% } %>
      </section>
      <section class="category-panel">
        <div class="section-heading">
          <div>
            <p class="tag">THIS MONTH</p>
            <h2>Category-wise spending</h2>
          </div>
        </div>
        <% if (expenseSummary == null || expenseSummary.categoryTotals().isEmpty()) { %>
          <p class="empty-state">No expenses recorded this month.</p>
        <% } else { %>
          <div class="category-list">
            <% for (Map.Entry<String, java.math.BigDecimal> entry : expenseSummary.categoryTotals().entrySet()) { %>
              <div class="category-row">
                <span><%= HtmlUtil.escape(entry.getKey()) %></span>
                <strong>&#8377;<%= entry.getValue().setScale(2).toPlainString() %></strong>
              </div>
            <% } %>
          </div>
        <% } %>
      </section>
    </main>
  </div>
</body>
</html>
