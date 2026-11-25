<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.smartspend.User" %>
<%@ page import="jakarta.servlet.http.HttpSession" %>
<%
  HttpSession currentSession = request.getSession(false);
  User loggedInUser = currentSession == null ? null : (User) currentSession.getAttribute("loggedInUser");
%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>SmartSpend</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/style.css">
</head>
<body>
  <main class="container">
    <section class="card welcome-card">
    <p class="tag">PERSONAL FINANCE, SIMPLIFIED</p>
    <h1>Welcome to <span>SmartSpend</span></h1>
    <p class="intro">Track spending, build budgets and reach your savings goals.</p>
    <div class="action-row">
      <% if (loggedInUser != null) { %>
        <a class="button" href="<%= request.getContextPath() %>/dashboard">Go to dashboard</a>
      <% } else { %>
        <a class="button" href="<%= request.getContextPath() %>/register">Create account</a>
        <a class="button button-secondary" href="<%= request.getContextPath() %>/login">Sign in</a>
      <% } %>
      <a class="text-link" href="<%= request.getContextPath() %>/db-test">Test database</a>
    </div>
    </section>
  </main>
</body>
</html>
