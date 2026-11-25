<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.smartspend.SavingsGoal" %>
<%@ page import="com.smartspend.HtmlUtil" %>
<%@ page import="java.util.Set" %>
<%
  SavingsGoal goal = (SavingsGoal) request.getAttribute("goal");
  Set<String> categories = (Set<String>) request.getAttribute("categories");
  Long goalId = (Long) request.getAttribute("goalId");
  if (goalId == null && goal != null) goalId = goal.getId();
  String name = request.getAttribute("submittedGoalName") != null
      ? HtmlUtil.escape(request.getAttribute("submittedGoalName"))
      : (goal == null ? "" : HtmlUtil.escape(goal.getName()));
  String target = request.getAttribute("submittedTargetAmount") != null
      ? HtmlUtil.escape(request.getAttribute("submittedTargetAmount"))
      : (goal == null ? "" : goal.getTargetAmount().toPlainString());
  String saved = request.getAttribute("submittedSavedAmount") != null
      ? HtmlUtil.escape(request.getAttribute("submittedSavedAmount"))
      : (goal == null ? "" : goal.getSavedAmount().toPlainString());
  String date = request.getAttribute("submittedTargetDate") != null
      ? HtmlUtil.escape(request.getAttribute("submittedTargetDate"))
      : (goal == null ? "" : (goal.getTargetDate() == null ? "" : goal.getTargetDate().toString()));
  String description = request.getAttribute("submittedDescription") != null
      ? HtmlUtil.escape(request.getAttribute("submittedDescription"))
      : (goal == null ? "" : HtmlUtil.escape(goal.getDescription()));
  String category = request.getAttribute("submittedCategory") != null
      ? HtmlUtil.escape(request.getAttribute("submittedCategory"))
      : (goal == null ? "" : goal.getCategory());
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title><%= goal == null ? "Create" : "Edit" %> goal | SmartSpend</title>
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
        <button class="button button-secondary button-small button-full" type="submit">Log out</button>
      </form>
    </aside>
    <main class="dashboard-main">
      <div class="form-container">
        <div class="section-heading">
          <div>
            <p class="tag"><%= goal == null ? "NEW SAVINGS GOAL" : "UPDATE SAVINGS GOAL" %></p>
            <h1><%= goal == null ? "Create savings goal" : "Edit savings goal" %></h1>
          </div>
          <a class="text-link" href="<%= request.getContextPath() %>/goals">Back to goals</a>
        </div>
        <% if (request.getAttribute("error") != null) { %>
          <div class="alert alert-error" role="alert"><%= HtmlUtil.escape(request.getAttribute("error")) %></div>
        <% } %>
        <form class="expense-form" method="post" action="<%= request.getContextPath() %>/goals/save">
          <% if (goalId != null) { %>
            <input type="hidden" name="goalId" value="<%= goalId %>">
          <% } %>
          <label for="goalName">Goal name</label>
          <input id="goalName" name="goalName" type="text" maxlength="120" required
                 value="<%= name %>" placeholder="Europe vacation">

          <label for="targetAmount">Target amount</label>
          <input id="targetAmount" name="targetAmount" type="number" min="0.01"
                 max="9999999999.99" step="0.01" required value="<%= target %>" placeholder="50000.00">

          <% if (goal == null) { %>
            <label for="savedAmount">Initial saved amount (optional)</label>
            <input id="savedAmount" name="savedAmount" type="number" min="0"
                   max="9999999999.99" step="0.01" value="<%= saved %>" placeholder="0.00">
          <% } %>

          <label for="targetDate">Target date</label>
          <input id="targetDate" name="targetDate" type="date" required value="<%= date %>">

          <label for="category">Goal category</label>
          <select id="category" name="category" required>
            <option value="">Select category</option>
            <% for (String item : categories) { %>
              <option value="<%= HtmlUtil.escape(item) %>" <%= item.equals(category) ? "selected" : "" %>>
                <%= HtmlUtil.escape(item) %>
              </option>
            <% } %>
          </select>

          <label for="description">Description</label>
          <textarea id="description" name="description" maxlength="500"
                    placeholder="What are you saving for?"><%= description %></textarea>

          <button class="button button-full" type="submit">
            <%= goal == null ? "Create goal" : "Save changes" %>
          </button>
        </form>
      </div>
    </main>
  </div>
</body>
</html>
