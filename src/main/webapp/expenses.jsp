<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.smartspend.Expense" %>
<%@ page import="com.smartspend.HtmlUtil" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Set" %>
<%@ page import="com.smartspend.CsrfUtil" %>
<%
  List<Expense> expenses = (List<Expense>) request.getAttribute("expenses");
  Set<String> categories = (Set<String>) request.getAttribute("categories");
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Expenses | SmartSpend</title>
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
      <header class="page-header">
        <div>
          <p class="tag">TRANSACTIONS</p>
          <h1>Expenses</h1>
        </div>
        <a class="button" href="<%= request.getContextPath() %>/expenses/add">Add expense</a>
      </header>

      <% if ("1".equals(request.getParameter("success"))) { %>
        <div class="alert alert-success">Expense saved successfully.</div>
      <% } else if ("1".equals(request.getParameter("deleted"))) { %>
        <div class="alert alert-success">Expense deleted successfully.</div>
      <% } %>
      <% if (request.getAttribute("error") != null) { %>
        <div class="alert alert-error" role="alert"><%= HtmlUtil.escape(request.getAttribute("error")) %></div>
      <% } %>

      <form class="filter-panel" method="get" action="<%= request.getContextPath() %>/expenses">
        <label for="search">Search description
          <input id="search" name="search" type="search"
                 value="<%= HtmlUtil.escape(request.getAttribute("search")) %>" placeholder="Search expenses">
        </label>
        <label for="category">Category
          <select id="category" name="category">
            <option value="">All categories</option>
            <% for (String item : categories) { %>
              <option value="<%= HtmlUtil.escape(item) %>"
                <%= item.equals(request.getAttribute("selectedCategory")) ? "selected" : "" %>>
                <%= HtmlUtil.escape(item) %>
              </option>
            <% } %>
          </select>
        </label>
        <label for="dateFrom">From
          <input id="dateFrom" name="dateFrom" type="date"
                 value="<%= HtmlUtil.escape(request.getAttribute("dateFrom")) %>">
        </label>
        <label for="dateTo">To
          <input id="dateTo" name="dateTo" type="date"
                 value="<%= HtmlUtil.escape(request.getAttribute("dateTo")) %>">
        </label>
        <label for="month">Month
          <input id="month" name="month" type="month"
                 value="<%= HtmlUtil.escape(request.getAttribute("month")) %>">
        </label>
        <button class="button" type="submit">Apply</button>
        <a class="button button-secondary" href="<%= request.getContextPath() %>/expenses">Clear</a>
      </form>

      <section class="table-panel">
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Category</th>
                <th>Description</th>
                <th class="amount-column">Amount</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <% if (expenses == null || expenses.isEmpty()) { %>
                <tr><td colspan="5" class="empty-state">No expenses match your selection.</td></tr>
              <% } else { %>
                <% for (Expense expense : expenses) { %>
                  <tr>
                    <td><%= expense.getExpenseDate() %></td>
                    <td><span class="category-badge"><%= HtmlUtil.escape(expense.getCategory()) %></span></td>
                    <td><%= HtmlUtil.escape(expense.getDescription()) %></td>
                    <td class="amount-column">&#8377;<%= expense.getAmount().setScale(2).toPlainString() %></td>
                    <td class="actions">
                      <a class="action-link" href="<%= request.getContextPath() %>/expenses/edit?id=<%= expense.getId() %>">Edit</a>
                      <form method="post" action="<%= request.getContextPath() %>/expenses/delete"
                            onsubmit="return confirm('Delete this expense? This cannot be undone.')">
                        <input type="hidden" name="csrfToken" value="<%= HtmlUtil.escape(CsrfUtil.getToken(request)) %>">
                        <input type="hidden" name="expenseId" value="<%= expense.getId() %>">
                        <button class="action-link danger" type="submit">Delete</button>
                      </form>
                    </td>
                  </tr>
                <% } %>
              <% } %>
            </tbody>
          </table>
        </div>
      </section>
    </main>
  </div>
</body>
</html>
