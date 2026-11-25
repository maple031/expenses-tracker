<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.smartspend.Reminder" %>
<%@ page import="com.smartspend.HtmlUtil" %>
<%@ page import="java.util.Set" %>
<%
  Reminder reminder = (Reminder) request.getAttribute("reminder");
  Set<String> categories = (Set<String>) request.getAttribute("categories");
  Long reminderId = (Long) request.getAttribute("reminderId");
  if (reminderId == null && reminder != null) reminderId = reminder.getId();
  String title = request.getAttribute("submittedTitle") != null
      ? HtmlUtil.escape(request.getAttribute("submittedTitle"))
      : (reminder == null ? "" : HtmlUtil.escape(reminder.getTitle()));
  String description = request.getAttribute("submittedDescription") != null
      ? HtmlUtil.escape(request.getAttribute("submittedDescription"))
      : (reminder == null ? "" : HtmlUtil.escape(reminder.getDescription()));
  String amount = request.getAttribute("submittedAmount") != null
      ? HtmlUtil.escape(request.getAttribute("submittedAmount"))
      : (reminder == null || reminder.getExpectedAmount() == null
          ? "" : reminder.getExpectedAmount().toPlainString());
  String category = request.getAttribute("submittedCategory") != null
      ? HtmlUtil.escape(request.getAttribute("submittedCategory"))
      : (reminder == null ? "" : HtmlUtil.escape(reminder.getCategory()));
  String date = request.getAttribute("submittedDate") != null
      ? HtmlUtil.escape(request.getAttribute("submittedDate"))
      : (reminder == null ? "" : reminder.getReminderDate().toString());
  String time = request.getAttribute("submittedTime") != null
      ? HtmlUtil.escape(request.getAttribute("submittedTime"))
      : (reminder == null || reminder.getReminderTime() == null
          ? "" : reminder.getReminderTime().toString());
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title><%= reminder == null ? "Create" : "Edit" %> reminder | SmartSpend</title>
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
        <a href="<%= request.getContextPath() %>/goals">Savings goals</a>
        <a class="active" href="<%= request.getContextPath() %>/reminders">Reminders</a>
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
            <p class="tag"><%= reminder == null ? "NEW REMINDER" : "UPDATE REMINDER" %></p>
            <h1><%= reminder == null ? "Create reminder" : "Edit reminder" %></h1>
          </div>
          <a class="text-link" href="<%= request.getContextPath() %>/reminders">Back to reminders</a>
        </div>
        <% if (request.getAttribute("error") != null) { %>
          <div class="alert alert-error" role="alert"><%= HtmlUtil.escape(request.getAttribute("error")) %></div>
        <% } %>
        <form class="expense-form" method="post" action="<%= request.getContextPath() %>/reminders/save">
          <% if (reminderId != null) { %>
            <input type="hidden" name="reminderId" value="<%= reminderId %>">
          <% } %>
          <label for="title">Reminder title</label>
          <input id="title" name="title" type="text" maxlength="150" required
                 value="<%= title %>" placeholder="Pay electricity bill">

          <label for="description">Description</label>
          <textarea id="description" name="description" maxlength="500"
                    placeholder="Any helpful details"><%= description %></textarea>

          <label for="expectedAmount">Expected amount (optional)</label>
          <input id="expectedAmount" name="expectedAmount" type="number" min="0"
                 max="9999999999.99" step="0.01" value="<%= amount %>" placeholder="0.00">

          <label for="category">Category</label>
          <select id="category" name="category" required>
            <option value="">Select category</option>
            <% for (String item : categories) { %>
              <option value="<%= HtmlUtil.escape(item) %>" <%= item.equals(category) ? "selected" : "" %>>
                <%= HtmlUtil.escape(item) %>
              </option>
            <% } %>
          </select>

          <label for="reminderDate">Reminder date</label>
          <input id="reminderDate" name="reminderDate" type="date" required value="<%= date %>">

          <label for="reminderTime">Reminder time (optional)</label>
          <input id="reminderTime" name="reminderTime" type="time" value="<%= time %>">

          <button class="button button-full" type="submit">
            <%= reminder == null ? "Create reminder" : "Save changes" %>
          </button>
        </form>
      </div>
    </main>
  </div>
</body>
</html>
