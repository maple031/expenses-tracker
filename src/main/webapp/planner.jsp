<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.smartspend.SmartPlan" %>
<%@ page import="com.smartspend.SmartPlanRepository" %>
<%@ page import="com.smartspend.HtmlUtil" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Set" %>
<%
  SmartPlan plan = (SmartPlan) request.getAttribute("plan");
  List<SmartPlan> plans = (List<SmartPlan>) request.getAttribute("plans");
  Set<String> activityTypes = (Set<String>) request.getAttribute("activityTypes");
  SmartPlanRepository.SmartRecommendation analysis =
      (SmartPlanRepository.SmartRecommendation) request.getAttribute("analysis");
  Long planId = (Long) request.getAttribute("planId");
  if (planId == null && plan != null) planId = plan.getId();
  String type = request.getAttribute("submittedType") != null
      ? HtmlUtil.escape(request.getAttribute("submittedType"))
      : (plan == null ? "" : plan.getActivityType());
  String name = request.getAttribute("submittedName") != null
      ? HtmlUtil.escape(request.getAttribute("submittedName"))
      : (plan == null ? "" : HtmlUtil.escape(plan.getActivityName()));
  String cost = request.getAttribute("submittedCost") != null
      ? HtmlUtil.escape(request.getAttribute("submittedCost"))
      : (plan == null ? "" : plan.getEstimatedCost().toPlainString());
  String date = request.getAttribute("submittedDate") != null
      ? HtmlUtil.escape(request.getAttribute("submittedDate"))
      : (plan == null ? "" : plan.getPlannedDate().toString());
  String description = request.getAttribute("submittedDescription") != null
      ? HtmlUtil.escape(request.getAttribute("submittedDescription"))
      : (plan == null ? "" : HtmlUtil.escape(plan.getDescription()));
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Smart Planner | SmartSpend</title>
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
        <a href="<%= request.getContextPath() %>/reminders">Reminders</a>
        <a class="active" href="<%= request.getContextPath() %>/planner">Smart Planner</a>
        <a href="<%= request.getContextPath() %>/reports">Reports</a>
      </nav>
      <form method="post" action="<%= request.getContextPath() %>/logout">
        <button class="button button-secondary button-small button-full" type="submit">Log out</button>
      </form>
    </aside>
    <main class="dashboard-main">
      <header class="page-header">
        <div>
          <p class="tag">RULE-BASED FINANCIAL PLANNING</p>
          <h1>Smart Planner</h1>
        </div>
      </header>

      <% if ("1".equals(request.getParameter("saved"))) { %>
        <div class="alert alert-success">Smart plan saved successfully.</div>
      <% } else if ("1".equals(request.getParameter("deleted"))) { %>
        <div class="alert alert-success">Smart plan deleted successfully.</div>
      <% } else if ("1".equals(request.getParameter("deleteError"))) { %>
        <div class="alert alert-error">Could not delete the smart plan.</div>
      <% } %>
      <% if (request.getAttribute("error") != null) { %>
        <div class="alert alert-error" role="alert"><%= HtmlUtil.escape(request.getAttribute("error")) %></div>
      <% } %>

      <section class="planner-layout">
        <div class="budget-form-panel">
          <h2><%= plan == null ? "Plan an activity" : "Edit planned activity" %></h2>
          <form class="expense-form" method="post" action="<%= request.getContextPath() %>/planner/analyze">
            <% if (planId != null) { %>
              <input type="hidden" name="planId" value="<%= planId %>">
            <% } %>
            <label for="activityType">Activity type</label>
            <select id="activityType" name="activityType" required>
              <option value="">Select activity</option>
              <% for (String item : activityTypes) { %>
                <option value="<%= HtmlUtil.escape(item) %>" <%= item.equals(type) ? "selected" : "" %>>
                  <%= HtmlUtil.escape(item) %>
                </option>
              <% } %>
            </select>

            <label for="activityName">Activity name</label>
            <input id="activityName" name="activityName" type="text" maxlength="150" required
                   value="<%= name %>" placeholder="Weekend trip">

            <label for="estimatedCost">Estimated cost</label>
            <input id="estimatedCost" name="estimatedCost" type="number" min="0.01"
                   max="9999999999.99" step="0.01" required
                   value="<%= cost %>" placeholder="5000.00">

            <label for="plannedDate">Planned date</label>
            <input id="plannedDate" name="plannedDate" type="date" required value="<%= date %>">

            <label for="description">Description (optional)</label>
            <textarea id="description" name="description" maxlength="500"
                      placeholder="Add planning details"><%= description %></textarea>

            <div class="planner-actions">
              <button class="button" type="submit" name="action" value="analyze">Analyze activity</button>
              <% if (planId != null) { %>
                <button class="button button-secondary" type="submit"
                        formaction="<%= request.getContextPath() %>/planner/save"
                        name="action" value="save">Save changes</button>
              <% } else { %>
                <button class="button button-secondary" type="submit"
                        formaction="<%= request.getContextPath() %>/planner/save"
                        name="action" value="save">Save plan</button>
              <% } %>
            </div>
          </form>
        </div>

        <div class="budget-overview-panel">
          <% if (analysis == null) { %>
            <p class="tag">FINANCIAL CHECK</p>
            <h2>Get a smart recommendation</h2>
            <p class="empty-state">
              Enter an activity and choose <strong>Analyze activity</strong>.
            </p>
          <% } else { %>
            <p class="tag">ANALYSIS RESULT</p>
            <h2><%= HtmlUtil.escape(analysis.status()) %></h2>
            <div class="recommendation-card <%= analysis.status().toLowerCase() %>">
              <strong>
                <%= analysis.affordable() ? "Affordable" :
                    (analysis.mayExceedBudget() ? "Budget warning" : "Not currently affordable") %>
              </strong>
              <p><%= HtmlUtil.escape(analysis.message()) %></p>
            </div>
            <div class="budget-overview-grid">
              <div><span>Remaining after activity</span>
                <strong>&#8377;<%= analysis.remainingAfter().setScale(2).toPlainString() %></strong></div>
              <div><span>Suggested spending limit</span>
                <strong>&#8377;<%= analysis.suggestedLimit().setScale(2).toPlainString() %></strong></div>
              <div><span>Suggested monthly savings</span>
                <strong>&#8377;<%= analysis.suggestedMonthlySavings().setScale(2).toPlainString() %></strong></div>
            </div>
            <% if (analysis.alternatives() != null && !analysis.alternatives().isBlank()) { %>
              <div class="alternative-box">
                <h3>Ways to reduce the cost</h3>
                <p><%= HtmlUtil.escape(analysis.alternatives()) %></p>
              </div>
            <% } %>
            <% if (!analysis.affordable() && "Travel".equals(type)) { %>
              <p class="planner-tip">
                Consider postponing the trip and saving
                &#8377;<%= analysis.suggestedMonthlySavings().setScale(2).toPlainString() %>
                each month until the planned date.
              </p>
            <% } %>
          <% } %>
        </div>
      </section>

      <section class="planner-history">
        <div class="section-heading">
          <div>
            <p class="tag">YOUR PLANS</p>
            <h2>Saved and previous plans</h2>
          </div>
        </div>
        <% if (plans == null || plans.isEmpty()) { %>
          <p class="empty-state">No saved plans yet.</p>
        <% } else { %>
          <div class="goal-grid">
            <% for (SmartPlan savedPlan : plans) { %>
              <article class="goal-card planner-card">
                <div class="goal-card-heading">
                  <span class="category-badge"><%= HtmlUtil.escape(savedPlan.getActivityType()) %></span>
                  <span class="goal-status <%= savedPlan.getRecommendationStatus().toLowerCase() %>">
                    <%= savedPlan.getRecommendationStatus() %>
                  </span>
                </div>
                <h2><%= HtmlUtil.escape(savedPlan.getActivityName()) %></h2>
                <p class="goal-date">Planned date: <strong><%= savedPlan.getPlannedDate() %></strong></p>
                <div class="goal-amounts">
                  <strong>&#8377;<%= savedPlan.getEstimatedCost().setScale(2).toPlainString() %></strong>
                  <span>estimated cost</span>
                </div>
                <div class="recommendation-card small <%= savedPlan.getRecommendationStatus().toLowerCase() %>">
                  <p><%= HtmlUtil.escape(savedPlan.getRecommendationText()) %></p>
                </div>
                <% if (savedPlan.getAlternatives() != null) { %>
                  <p class="goal-description"><%= HtmlUtil.escape(savedPlan.getAlternatives()) %></p>
                <% } %>
                <div class="budget-overview-grid planner-mini">
                  <div><span>Remaining after</span>
                    <strong>&#8377;<%= savedPlan.getRemainingAfter().setScale(2).toPlainString() %></strong></div>
                  <div><span>Suggested limit</span>
                    <strong>&#8377;<%= savedPlan.getSuggestedLimit().setScale(2).toPlainString() %></strong></div>
                </div>
                <div class="goal-actions">
                  <a class="action-link" href="<%= request.getContextPath() %>/planner/edit?id=<%= savedPlan.getId() %>">Edit</a>
                  <form method="post" action="<%= request.getContextPath() %>/planner/delete"
                        onsubmit="return confirm('Delete this smart plan? This cannot be undone.')">
                    <input type="hidden" name="planId" value="<%= savedPlan.getId() %>">
                    <button class="action-link danger" type="submit">Delete</button>
                  </form>
                </div>
              </article>
            <% } %>
          </div>
        <% } %>
      </section>
    </main>
  </div>
</body>
</html>
