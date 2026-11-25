<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.smartspend.CsrfUtil" %>
<%@ page import="com.smartspend.HtmlUtil" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Create account | SmartSpend</title>
  <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/style.css">
</head>
<body>
  <main class="auth-layout">
    <section class="auth-card">
      <a class="brand" href="<%= request.getContextPath() %>/">SmartSpend</a>
      <p class="tag">START YOUR JOURNEY</p>
      <h1>Create account</h1>
      <p class="intro">Take control of your personal finances.</p>
      <% if (request.getAttribute("error") != null) { %>
        <div class="alert alert-error" role="alert"><%= HtmlUtil.escape(request.getAttribute("error")) %></div>
      <% } %>
      <form method="post" action="<%= request.getContextPath() %>/register" autocomplete="on">
        <input type="hidden" name="csrfToken" value="<%= HtmlUtil.escape(CsrfUtil.getToken(request)) %>">
        <label for="fullName">Full name</label>
        <input id="fullName" name="fullName" type="text" maxlength="100" required
               value="<%= HtmlUtil.escape(request.getAttribute("fullName")) %>"
               placeholder="Alex Morgan">

        <label for="email">Email</label>
        <input id="email" name="email" type="email" maxlength="150" required
               value="<%= HtmlUtil.escape(request.getAttribute("email")) %>"
               placeholder="alex@example.com">

        <label for="password">Password</label>
        <input id="password" name="password" type="password" minlength="8" maxlength="72" required
               placeholder="At least 8 characters">

        <label for="confirmPassword">Confirm password</label>
        <input id="confirmPassword" name="confirmPassword" type="password" minlength="8" maxlength="72" required
               placeholder="Re-enter your password">

        <button class="button button-full" type="submit">Create account</button>
      </form>
      <p class="auth-switch">Already have an account?
        <a href="<%= request.getContextPath() %>/login">Sign in</a>
      </p>
    </section>
  </main>
</body>
</html>
