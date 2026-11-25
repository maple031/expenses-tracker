<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.smartspend.Expense" %>
<%@ page import="com.smartspend.HtmlUtil" %>
<%@ page import="java.util.Set" %>
<%@ page import="com.smartspend.CsrfUtil" %>
<%
  Expense expense = (Expense) request.getAttribute("expense");
  Set<String> categories = (Set<String>) request.getAttribute("categories");
  Long expenseId = (Long) request.getAttribute("expenseId");
  if (expenseId == null && expense != null) expenseId = expense.getId();

  String description = request.getAttribute("submittedDescription") != null
      ? HtmlUtil.escape(request.getAttribute("submittedDescription"))
      : (expense == null ? "" : HtmlUtil.escape(expense.getDescription()));
  String amount = request.getAttribute("submittedAmount") != null
      ? HtmlUtil.escape(request.getAttribute("submittedAmount"))
      : (expense == null ? "" : expense.getAmount().toPlainString());
  String category = request.getAttribute("submittedCategory") != null
      ? HtmlUtil.escape(request.getAttribute("submittedCategory"))
      : (expense == null ? "" : expense.getCategory());
  String date = request.getAttribute("submittedDate") != null
      ? HtmlUtil.escape(request.getAttribute("submittedDate"))
      : (expense == null ? "" : expense.getExpenseDate().toString());
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title><%= expense == null ? "Add" : "Edit" %> expense | SmartSpend</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/style.css">
</head>
<body>
  <div class="app-shell">
    <aside class="sidebar">
      <a class="brand" href="<%= request.getContextPath() %>/">SmartSpend</a>
      <nav class="sidebar-nav" aria-label="Main navigation">
        <a href="<%= request.getContextPath() %>/dashboard">Dashboard</a>
        <a class="active" href="<%= request.getContextPath() %>/expenses">Expenses</a>
        <a href="<%= request.getContextPath() %>/budgets">Budgets</a>
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
      <div class="form-container">
        <div class="section-heading">
          <div>
            <p class="tag"><%= expense == null ? "NEW TRANSACTION" : "UPDATE TRANSACTION" %></p>
            <h1><%= expense == null ? "Add expense" : "Edit expense" %></h1>
          </div>
          <a class="text-link" href="<%= request.getContextPath() %>/expenses">Back to expenses</a>
        </div>
        <% if (request.getAttribute("error") != null) { %>
          <div class="alert alert-error" role="alert"><%= HtmlUtil.escape(request.getAttribute("error")) %></div>
        <% } %>
        <form class="expense-form" method="post" action="<%= request.getContextPath() %>/expenses/save">
          <input type="hidden" name="csrfToken" value="<%= HtmlUtil.escape(CsrfUtil.getToken(request)) %>">
          <% if (expenseId != null) { %>
            <input type="hidden" name="expenseId" value="<%= expenseId %>">
          <% } %>
          <label for="description">Description</label>
          <input id="description" name="description" type="text" maxlength="150" required
                 value="<%= description %>" placeholder="Lunch at cafe">

          <label for="amount">Amount</label>
          <input id="amount" name="amount" type="number" min="0.01" max="9999999999.99" step="0.01" required
                 value="<%= amount %>" placeholder="0.00">

          <label for="category">Category</label>
          <select id="category" name="category" required>
            <option value="">Select category</option>
            <% for (String item : categories) { %>
              <option value="<%= HtmlUtil.escape(item) %>" <%= item.equals(category) ? "selected" : "" %>>
                <%= HtmlUtil.escape(item) %>
              </option>
            <% } %>
          </select>

          <label for="expenseDate">Date</label>
          <input id="expenseDate" name="expenseDate" type="date" required value="<%= date %>">

          <button class="button button-full" type="submit">
            <%= expense == null ? "Save expense" : "Save changes" %>
          </button>
        </form>
      </div>
    </main>
  </div>
</body>
</html>
