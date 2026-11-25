<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.smartspend.SavingsGoal" %>
<%@ page import="com.smartspend.HtmlUtil" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Set" %>
<%@ page import="com.smartspend.CsrfUtil" %>
<%
  List<SavingsGoal> goals = (List<SavingsGoal>) request.getAttribute("goals");
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Savings Goals | SmartSpend</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/style.css">
</head>
<body>
  <div class="app-shell">
    <aside class="sidebar">
      <a class="brand" href="<%= request.getContextPath() %>/">SmartSpend</a>
      <nav class="sidebar-nav" aria-label="Main navigation">
        <a href="<%= request.getContextPath() %>/dashboard">Dashboard</a>
        <a href="<%= request.getContextPath() %>/expenses">Expenses</a>
        <a href="<%= request.getContextPath() %>/budgets">Budgets</a>
        <a class="active" href="<%= request.getContextPath() %>/goals">Savings goals</a>
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
          <p class="tag">YOUR SAVINGS</p>
          <h1>Savings goals</h1>
        </div>
        <a class="button" href="<%= request.getContextPath() %>/goals/new">Create goal</a>
      </header>

      <% if ("1".equals(request.getParameter("success"))) { %>
        <div class="alert alert-success">Savings goal saved successfully.</div>
      <% } else if ("1".equals(request.getParameter("added"))) { %>
        <div class="alert alert-success">Savings added successfully.</div>
      <% } else if ("1".equals(request.getParameter("deleted"))) { %>
        <div class="alert alert-success">Savings goal deleted successfully.</div>
      <% } %>
      <% if (request.getParameter("addError") != null) { %>
        <div class="alert alert-error" role="alert"><%= HtmlUtil.escape(request.getParameter("addError")) %></div>
      <% } else if (request.getAttribute("error") != null) { %>
        <div class="alert alert-error" role="alert"><%= HtmlUtil.escape(request.getAttribute("error")) %></div>
      <% } else if ("1".equals(request.getParameter("deleteError"))) { %>
        <div class="alert alert-error" role="alert">Could not delete the savings goal.</div>
      <% } %>

      <% if (goals == null || goals.isEmpty()) { %>
        <section class="goal-empty">
          <p class="empty-state">No savings goals yet.</p>
          <a class="button" href="<%= request.getContextPath() %>/goals/new">Create your first goal</a>
        </section>
      <% } else { %>
        <section class="goal-grid">
          <% for (SavingsGoal goal : goals) { %>
            <% java.math.BigDecimal visualProgress = goal.getProgressPercentage()
                .compareTo(java.math.BigDecimal.valueOf(100)) > 0
                ? java.math.BigDecimal.valueOf(100) : goal.getProgressPercentage(); %>
            <article class="goal-card">
              <div class="goal-card-heading">
                <span class="category-badge"><%= HtmlUtil.escape(goal.getCategory()) %></span>
                <span class="goal-status <%= goal.getStatus().toLowerCase() %>"><%= goal.getStatus() %></span>
              </div>
              <h2><%= HtmlUtil.escape(goal.getName()) %></h2>
              <% if (goal.getDescription() != null && !goal.getDescription().isBlank()) { %>
                <p class="goal-description"><%= HtmlUtil.escape(goal.getDescription()) %></p>
              <% } %>
              <div class="goal-amounts">
                <strong>&#8377;<%= goal.getSavedAmount().setScale(2).toPlainString() %></strong>
                <span>of &#8377;<%= goal.getTargetAmount().setScale(2).toPlainString() %></span>
              </div>
              <div class="progress-heading">
                <strong><%= goal.getProgressPercentage().setScale(2).toPlainString() %>%</strong>
                <span>&#8377;<%= goal.getRemainingAmount().setScale(2).toPlainString() %> remaining</span>
              </div>
              <div class="progress-track" role="progressbar"
                   aria-valuemin="0" aria-valuemax="100"
                   aria-valuenow="<%= visualProgress.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() %>">
                <div class="progress-fill <%= "COMPLETED".equals(goal.getStatus()) ? "normal" : "normal" %>"
                     style="width:<%= visualProgress.toPlainString() %>%"></div>
              </div>
              <p class="goal-date">Target date:
                <strong><%= goal.getTargetDate() == null ? "Not set" : goal.getTargetDate() %></strong>
              </p>
              <form class="add-savings-form" method="post"
                    action="<%= request.getContextPath() %>/goals/add-savings">
                <input type="hidden" name="csrfToken" value="<%= HtmlUtil.escape(CsrfUtil.getToken(request)) %>">
                <input type="hidden" name="goalId" value="<%= goal.getId() %>">
                <label for="addAmount-<%= goal.getId() %>">Add savings</label>
                <div>
                  <input id="addAmount-<%= goal.getId() %>" name="addAmount" type="number"
                         min="0.01" step="0.01"
                         max="<%= goal.getRemainingAmount().toPlainString() %>"
                         required placeholder="Amount"
                         <%= "COMPLETED".equals(goal.getStatus()) ? "disabled" : "" %>>
                  <button class="button button-small" type="submit"
                          <%= "COMPLETED".equals(goal.getStatus()) ? "disabled" : "" %>>
                    Add
                  </button>
                </div>
              </form>
              <div class="goal-actions">
                <a class="action-link" href="<%= request.getContextPath() %>/goals/edit?id=<%= goal.getId() %>">Edit</a>
                <form method="post" action="<%= request.getContextPath() %>/goals/delete"
                      onsubmit="return confirm('Delete this savings goal? This cannot be undone.')">
                  <input type="hidden" name="csrfToken" value="<%= HtmlUtil.escape(CsrfUtil.getToken(request)) %>">
                  <input type="hidden" name="goalId" value="<%= goal.getId() %>">
                  <button class="action-link danger" type="submit">Delete</button>
                </form>
              </div>
            </article>
          <% } %>
        </section>
      <% } %>
    </main>
  </div>
</body>
</html>
