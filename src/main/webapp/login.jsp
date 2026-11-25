<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.smartspend.CsrfUtil" %>
<%@ page import="com.smartspend.HtmlUtil" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Sign in | SmartSpend</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/style.css">
</head>
<body>
  <main class="auth-layout">
    <section class="auth-card">
      <a class="brand" href="<%= request.getContextPath() %>/">SmartSpend</a>
      <p class="tag">WELCOME BACK</p>
      <h1>Sign in</h1>
      <p class="intro">Continue to your SmartSpend dashboard.</p>
      <% if ("1".equals(request.getParameter("registered"))) { %>
        <div class="alert alert-success" role="status">Account created successfully. Sign in to continue.</div>
      <% } else if ("1".equals(request.getParameter("loggedOut"))) { %>
        <div class="alert alert-success" role="status">You have been logged out.</div>
      <% } else if ("1".equals(request.getParameter("required"))) { %>
        <div class="alert alert-error" role="alert">Please sign in to access the dashboard.</div>
      <% } %>
      <% if (request.getAttribute("error") != null) { %>
        <div class="alert alert-error" role="alert"><%= HtmlUtil.escape(request.getAttribute("error")) %></div>
      <% } %>
      <form method="post" action="<%= request.getContextPath() %>/login" autocomplete="on">
        <input type="hidden" name="csrfToken" value="<%= HtmlUtil.escape(CsrfUtil.getToken(request)) %>">
        <label for="email">Email</label>
        <input id="email" name="email" type="email" maxlength="150" required
               value="<%= HtmlUtil.escape(request.getAttribute("email")) %>"
               placeholder="alex@example.com">

        <label for="password">Password</label>
        <input id="password" name="password" type="password" minlength="8" maxlength="72" required
               placeholder="Enter your password">

        <button class="button button-full" type="submit">Sign in</button>
      </form>
      <p class="auth-switch">New to SmartSpend?
        <a href="<%= request.getContextPath() %>/register">Create account</a>
      </p>
    </section>
  </main>
</body>
</html>
