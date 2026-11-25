<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.smartspend.ReportRepository" %>
<%@ page import="com.smartspend.Expense" %>
<%@ page import="com.smartspend.HtmlUtil" %>
<%@ page import="java.util.Map" %>
<%
  ReportRepository.ReportData report =
      (ReportRepository.ReportData) request.getAttribute("report");
  String selectedMonth = (String) request.getAttribute("selectedMonth");
  String selectedStart = (String) request.getAttribute("selectedStart");
  String selectedEnd = (String) request.getAttribute("selectedEnd");
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Reports | SmartSpend</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/style.css">
  <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
</head>
<body>
  <div class="app-shell">
    <aside class="sidebar">
      <a class="brand" href="<%= request.getContextPath() %>/">SmartSpend</a>
      <nav class="sidebar-nav" aria-label="Main navigation">
        <a href="<%= request.getContextPath() %>/dashboard">Dashboard</a>
        <a href="<%= request.getContextPath() %>/expenses">Expenses</a>
        <a href="<%= request.getContextPath() %>/budgets">Budgets</a>
        <a href="<%= request.getContextPath() %>/goals">Savings goals</a>
        <a href="<%= request.getContextPath() %>/reminders">Reminders</a>
        <a href="<%= request.getContextPath() %>/planner">Smart Planner</a>
        <a class="active" href="<%= request.getContextPath() %>/reports">Reports</a>
      </nav>
      <form method="post" action="<%= request.getContextPath() %>/logout">
        <button class="button button-secondary button-small button-full" type="submit">Log out</button>
      </form>
    </aside>
    <main class="dashboard-main">
      <header class="page-header">
        <div>
          <p class="tag">FINANCIAL REPORTS</p>
          <h1>Reports & downloads</h1>
        </div>
      </header>

      <% if (request.getParameter("error") != null) { %>
        <div class="alert alert-error" role="alert">
          <%= HtmlUtil.escape(request.getParameter("error")) %>
        </div>
      <% } else if (request.getAttribute("error") != null) { %>
        <div class="alert alert-error" role="alert">
          <%= HtmlUtil.escape(request.getAttribute("error")) %>
        </div>
      <% } %>

      <section class="report-filter-grid">
        <form class="budget-form-panel" method="get" action="<%= request.getContextPath() %>/reports">
          <h2>Monthly report</h2>
          <label for="month">Month and year</label>
          <input id="month" name="month" type="month"
                 value="<%= HtmlUtil.escape(selectedMonth) %>" required>
          <button class="button button-full" type="submit">View monthly report</button>
        </form>
        <form class="budget-form-panel" method="get" action="<%= request.getContextPath() %>/reports">
          <h2>Custom date range</h2>
          <label for="startDate">Start date</label>
          <input id="startDate" name="startDate" type="date"
                 value="<%= HtmlUtil.escape(selectedStart) %>" required>
          <label for="endDate">End date</label>
          <input id="endDate" name="endDate" type="date"
                 value="<%= HtmlUtil.escape(selectedEnd) %>" required>
          <button class="button button-full" type="submit">View date range</button>
        </form>
      </section>

      <% if (report != null) { %>
        <section class="report-actions">
          <div>
            <p class="tag">SELECTED PERIOD</p>
            <h2><%= report.startDate() %> to <%= report.endDate() %></h2>
          </div>
          <div>
            <% if (selectedMonth != null && !selectedMonth.isBlank()) { %>
              <a class="button" href="<%= request.getContextPath() %>/reports/pdf?month=<%= HtmlUtil.escape(selectedMonth) %>">Download PDF</a>
              <a class="button button-secondary" href="<%= request.getContextPath() %>/reports/excel?month=<%= HtmlUtil.escape(selectedMonth) %>">Download Excel</a>
            <% } else { %>
              <a class="button" href="<%= request.getContextPath() %>/reports/pdf?startDate=<%= report.startDate() %>&endDate=<%= report.endDate() %>">Download PDF</a>
              <a class="button button-secondary" href="<%= request.getContextPath() %>/reports/excel?startDate=<%= report.startDate() %>&endDate=<%= report.endDate() %>">Download Excel</a>
            <% } %>
          </div>
        </section>

        <section class="summary-grid report-summary">
          <article class="summary-card">
            <span>Total expenses</span>
            <strong>&#8377;<%= report.total().setScale(2).toPlainString() %></strong>
          </article>
          <article class="summary-card">
            <span>Transactions</span>
            <strong><%= report.transactionCount() %></strong>
          </article>
          <article class="summary-card">
            <span>Highest expense</span>
            <strong>&#8377;<%= report.highestExpense().setScale(2).toPlainString() %></strong>
          </article>
        </section>

        <section class="report-content-grid">
          <article class="category-panel">
            <div class="section-heading">
              <div><p class="tag">CATEGORY-WISE</p><h2>Spending breakdown</h2></div>
            </div>
            <% if (report.categoryTotals().isEmpty()) { %>
              <p class="empty-state">No expense data for this period.</p>
            <% } else { %>
              <div class="category-list">
                <% for (Map.Entry<String, java.math.BigDecimal> entry : report.categoryTotals().entrySet()) { %>
                  <div class="category-row">
                    <span><%= HtmlUtil.escape(entry.getKey()) %></span>
                    <strong>&#8377;<%= entry.getValue().setScale(2).toPlainString() %>
                      (<%= report.categoryPercentages().get(entry.getKey()).setScale(2).toPlainString() %>%)
                    </strong>
                  </div>
                <% } %>
              </div>
              <canvas id="categoryChart" height="220"
                      aria-label="Category-wise spending chart" role="img"></canvas>
            <% } %>
          </article>
          <article class="table-panel">
            <div class="section-heading">
              <div><p class="tag">DETAILS</p><h2>Expense transactions</h2></div>
            </div>
            <div class="table-wrap">
              <table>
                <thead>
                  <tr><th>Date</th><th>Category</th><th>Description</th><th class="amount-column">Amount</th></tr>
                </thead>
                <tbody>
                  <% if (report.expenses().isEmpty()) { %>
                    <tr><td colspan="4" class="empty-state">No expenses found.</td></tr>
                  <% } else { %>
                    <% for (Expense expense : report.expenses()) { %>
                      <tr>
                        <td><%= expense.getExpenseDate() %></td>
                        <td><span class="category-badge"><%= HtmlUtil.escape(expense.getCategory()) %></span></td>
                        <td><%= HtmlUtil.escape(expense.getDescription()) %></td>
                        <td class="amount-column">&#8377;<%= expense.getAmount().setScale(2).toPlainString() %></td>
                      </tr>
                    <% } %>
                  <% } %>
                </tbody>
              </table>
            </div>
          </article>
        </section>
        <% if (!report.categoryTotals().isEmpty()) { %>
          <script>
            const reportCategories = [
              <% for (Map.Entry<String, java.math.BigDecimal> entry : report.categoryTotals().entrySet()) { %>
                "<%= HtmlUtil.escape(entry.getKey()).replace("\"", "\\\"") %>",
              <% } %>
            ];
            const reportAmounts = [
              <% for (Map.Entry<String, java.math.BigDecimal> entry : report.categoryTotals().entrySet()) { %>
                <%= entry.getValue().toPlainString() %>,
              <% } %>
            ];
            new Chart(document.getElementById("categoryChart"), {
              type: "doughnut",
              data: {
                labels: reportCategories,
                datasets: [{
                  data: reportAmounts,
                  backgroundColor: ["#6657d9", "#42a86b", "#e3a008", "#dc6b6b", "#4e91cc", "#a66ccf", "#50aaa8", "#8790a5"]
                }]
              },
              options: {
                responsive: true,
                plugins: { legend: { position: "bottom" } }
              }
            });
          </script>
        <% } %>
      <% } %>
    </main>
  </div>
</body>
</html>
