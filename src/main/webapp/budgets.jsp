<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.smartspend.Budget" %>
<%@ page import="com.smartspend.BudgetRepository" %>
<%@ page import="com.smartspend.HtmlUtil" %>
<%@ page import="com.smartspend.CsrfUtil" %>
<%
  Budget budget = (Budget) request.getAttribute("budget");
  BudgetRepository.BudgetSummary budgetSummary =
      (BudgetRepository.BudgetSummary) request.getAttribute("budgetSummary");
  String selectedMonth = (String) request.getAttribute("selectedMonth");
  if (selectedMonth == null) selectedMonth = java.time.YearMonth.now().toString();
  java.math.BigDecimal progress = budgetSummary == null
      ? java.math.BigDecimal.ZERO : budgetSummary.percentage();
  java.math.BigDecimal visualProgress = progress.compareTo(java.math.BigDecimal.valueOf(100)) > 0
      ? java.math.BigDecimal.valueOf(100) : progress;
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Budgets | SmartSpend</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/style.css">
</head>
<body>
  <div class="app-shell">
    <aside class="sidebar">
      <a class="brand" href="<%= request.getContextPath() %>/">SmartSpend</a>
      <nav class="sidebar-nav" aria-label="Main navigation">
        <a href="<%= request.getContextPath() %>/dashboard">Dashboard</a>
        <a href="<%= request.getContextPath() %>/expenses">Expenses</a>
        <a class="active" href="<%= request.getContextPath() %>/budgets">Budgets</a>
        <a href="<%= request.getContextPath() %>/goals">Savings goals</a>
        <a href="<%= request.getContextPath() %>/reminders">Reminders</a>
        <a href="<%= request.getContextPath() %>/planner">Smart Planner</a>
        <a href="<%= request.getContextPath() %>/reports">Reports</a>
      </nav>
      <form method="post" action="<%= request.getContextPath() %>/logout">
        <input type="hidden" name="csrfToken" value="<%= HtmlUtil.escape(CsrfUtil.getToken(request)) %>">
        <button class="button button-secondary button-small button-full" type="submit">Log out</button>
      </form>
    </aside>
    <main class="dashboard-main">
      <header class="page-header">
        <div>
          <p class="tag">MONTHLY PLANNING</p>
          <h1>Budgets</h1>
        </div>
      </header>

      <% if ("1".equals(request.getParameter("success"))) { %>
        <div class="alert alert-success">Monthly budget saved successfully.</div>
      <% } %>
      <% if (request.getAttribute("error") != null) { %>
        <div class="alert alert-error" role="alert"><%= HtmlUtil.escape(request.getAttribute("error")) %></div>
      <% } %>

      <section class="budget-layout">
        <div class="budget-form-panel">
          <h2><%= budget == null ? "Set monthly budget" : "Update monthly budget" %></h2>
          <form class="expense-form" method="post" action="<%= request.getContextPath() %>/budgets/save">
            <input type="hidden" name="csrfToken" value="<%= HtmlUtil.escape(CsrfUtil.getToken(request)) %>">
            <label for="month">Month</label>
            <input id="month" name="month" type="month" value="<%= HtmlUtil.escape(selectedMonth) %>" required>

            <label for="monthlyLimit">Budget amount</label>
            <input id="monthlyLimit" name="monthlyLimit" type="number" min="0.01"
                   max="9999999999.99" step="0.01" required
                   value="<%= budget == null ? "" : budget.getMonthlyLimit().toPlainString() %>"
                   placeholder="25000.00">

            <button class="button button-full" type="submit">
              <%= budget == null ? "Set budget" : "Update budget" %>
            </button>
          </form>
        </div>

        <div class="budget-overview-panel">
          <div class="section-heading">
            <div>
              <p class="tag">BUDGET OVERVIEW</p>
              <h2><%= HtmlUtil.escape(selectedMonth) %></h2>
            </div>
            <span class="status-badge <%= budgetSummary.statusClass() %>">
              <%= budgetSummary.status() %>
            </span>
          </div>

          <% if (budgetSummary.limit().compareTo(java.math.BigDecimal.ZERO) == 0) { %>
            <p class="empty-state">Set a budget to see spending progress.</p>
          <% } else { %>
            <div class="budget-overview-grid">
              <div><span>Total budget</span><strong>&#8377;<%= budgetSummary.limit().setScale(2).toPlainString() %></strong></div>
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
            <div class="alert <%= "EXCEEDED".equals(budgetSummary.status())
                ? "alert-error" : ("WARNING".equals(budgetSummary.status()) ? "alert-warning" : "alert-success") %>">
              <%= HtmlUtil.escape(budgetSummary.statusMessage()) %>
            </div>
          <% } %>
        </div>
      </section>
    </main>
  </div>
</body>
</html>
