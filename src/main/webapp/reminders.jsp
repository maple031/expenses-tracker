<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.smartspend.Reminder" %>
<%@ page import="com.smartspend.ReminderRepository" %>
<%@ page import="com.smartspend.HtmlUtil" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Set" %>
<%
  List<Reminder> reminders = (List<Reminder>) request.getAttribute("reminders");
  Set<String> categories = (Set<String>) request.getAttribute("categories");
  ReminderRepository.ReminderSummary reminderSummary =
      (ReminderRepository.ReminderSummary) request.getAttribute("reminderSummary");
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Reminders | SmartSpend</title>
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
        <a class="active" href="<%= request.getContextPath() %>/reminders">
          Reminders
          <% if (reminderSummary != null && reminderSummary.dueCount() > 0) { %>
            <span class="notification-badge"><%= reminderSummary.dueCount() %></span>
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
          <p class="tag">EXPENSE REMINDERS</p>
          <h1>Reminders</h1>
        </div>
        <a class="button" href="<%= request.getContextPath() %>/reminders/new">Create reminder</a>
      </header>

      <% if ("1".equals(request.getParameter("success"))) { %>
        <div class="alert alert-success">Reminder saved successfully.</div>
      <% } else if ("1".equals(request.getParameter("completed"))) { %>
        <div class="alert alert-success">Reminder marked as completed.</div>
      <% } else if ("1".equals(request.getParameter("deleted"))) { %>
        <div class="alert alert-success">Reminder deleted successfully.</div>
      <% } %>
      <% if (request.getAttribute("error") != null) { %>
        <div class="alert alert-error" role="alert"><%= HtmlUtil.escape(request.getAttribute("error")) %></div>
      <% } else if ("1".equals(request.getParameter("deleteError"))) { %>
        <div class="alert alert-error" role="alert">Could not delete the reminder.</div>
      <% } else if ("1".equals(request.getParameter("completeError"))) { %>
        <div class="alert alert-error" role="alert">Could not update the reminder.</div>
      <% } %>

      <% if (reminderSummary != null && reminderSummary.dueCount() > 0) { %>
        <div class="alert alert-warning">
          You have <strong><%= reminderSummary.dueCount() %></strong> due reminder(s).
          Review them below.
        </div>
      <% } %>

      <section class="reminder-count-grid">
        <article><span>Upcoming</span><strong><%= reminderSummary == null ? 0 : reminderSummary.upcomingCount() %></strong></article>
        <article><span>Today</span><strong><%= reminderSummary == null ? 0 : reminderSummary.todayCount() %></strong></article>
        <article><span>Completed</span><strong><%= reminderSummary == null ? 0 : reminderSummary.completedCount() %></strong></article>
      </section>

      <form class="filter-panel reminder-filter" method="get"
            action="<%= request.getContextPath() %>/reminders">
        <label for="search">Search
          <input id="search" name="search" type="search"
                 value="<%= HtmlUtil.escape(request.getAttribute("search")) %>"
                 placeholder="Title or description">
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
        <label for="date">Date
          <input id="date" name="date" type="date"
                 value="<%= HtmlUtil.escape(request.getAttribute("selectedDate")) %>">
        </label>
        <label for="status">Status
          <select id="status" name="status">
            <option value="">All statuses</option>
            <% for (String item : new String[]{"UPCOMING", "TODAY", "PAST", "COMPLETED"}) { %>
              <option value="<%= item %>"
                      <%= item.equals(request.getAttribute("selectedStatus")) ? "selected" : "" %>>
                <%= item %>
              </option>
            <% } %>
          </select>
        </label>
        <button class="button" type="submit">Apply</button>
        <a class="button button-secondary" href="<%= request.getContextPath() %>/reminders">Clear</a>
      </form>

      <section class="reminder-sections">
        <% if (reminders == null || reminders.isEmpty()) { %>
          <div class="goal-empty">
            <p class="empty-state">No reminders match your selection.</p>
            <a class="button" href="<%= request.getContextPath() %>/reminders/new">Create reminder</a>
          </div>
        <% } else { %>
          <% String[] sections = {"TODAY", "UPCOMING", "PAST", "COMPLETED"}; %>
          <% for (String section : sections) { %>
            <% boolean hasSection = false; %>
            <% for (Reminder reminder : reminders) {
                 if (("COMPLETED".equals(section) && reminder.isCompleted())
                     || (!reminder.isCompleted() && section.equals(reminder.getStatus()))) {
                   hasSection = true;
                 }
               } %>
            <% if (hasSection) { %>
              <div class="reminder-section">
                <h2><%= section.charAt(0) + section.substring(1).toLowerCase() %> reminders</h2>
                <div class="goal-grid">
                  <% for (Reminder reminder : reminders) {
                       if (!(("COMPLETED".equals(section) && reminder.isCompleted())
                           || (!reminder.isCompleted() && section.equals(reminder.getStatus())))) continue;
                  %>
                    <article class="goal-card reminder-card">
                      <div class="goal-card-heading">
                        <span class="category-badge"><%= HtmlUtil.escape(reminder.getCategory()) %></span>
                        <span class="goal-status <%= reminder.getStatus().toLowerCase() %>">
                          <%= reminder.getStatus() %>
                        </span>
                      </div>
                      <h2><%= HtmlUtil.escape(reminder.getTitle()) %></h2>
                      <% if (reminder.getDescription() != null && !reminder.getDescription().isBlank()) { %>
                        <p class="goal-description"><%= HtmlUtil.escape(reminder.getDescription()) %></p>
                      <% } %>
                      <div class="reminder-details">
                        <span><strong>Date:</strong> <%= reminder.getReminderDate() %></span>
                        <% if (reminder.getReminderTime() != null) { %>
                          <span><strong>Time:</strong> <%= reminder.getReminderTime() %></span>
                        <% } %>
                        <% if (reminder.getExpectedAmount() != null) { %>
                          <span><strong>Amount:</strong> &#8377;<%= reminder.getExpectedAmount().setScale(2).toPlainString() %></span>
                        <% } %>
                      </div>
                      <div class="goal-actions">
                        <% if (!reminder.isCompleted()) { %>
                          <form method="post" action="<%= request.getContextPath() %>/reminders/complete">
                            <input type="hidden" name="reminderId" value="<%= reminder.getId() %>">
                            <button class="action-link" type="submit">Mark complete</button>
                          </form>
                        <% } %>
                        <a class="action-link" href="<%= request.getContextPath() %>/reminders/edit?id=<%= reminder.getId() %>">Edit</a>
                        <form method="post" action="<%= request.getContextPath() %>/reminders/delete"
                              onsubmit="return confirm('Delete this reminder? This cannot be undone.')">
                          <input type="hidden" name="reminderId" value="<%= reminder.getId() %>">
                          <button class="action-link danger" type="submit">Delete</button>
                        </form>
                      </div>
                    </article>
                  <% } %>
                </div>
              </div>
            <% } %>
          <% } %>
        <% } %>
      </section>
    </main>
  </div>
</body>
</html>
