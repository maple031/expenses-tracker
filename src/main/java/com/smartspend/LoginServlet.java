package com.smartspend;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private final UserRepository users = new UserRepository();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession existing = request.getSession(false);
        if (existing != null && existing.getAttribute("loggedInUser") != null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String email = trim(request.getParameter("email")).toLowerCase();
        String password = request.getParameter("password");

        if (email.isEmpty() || password == null || password.isEmpty()) {
            showError(request, response, "Enter both email and password.", email);
            return;
        }

        try {
            var result = users.findByEmail(email);
            if (result.isEmpty() || !BCrypt.checkpw(password, result.get().passwordHash())) {
                showError(request, response, "The email or password is incorrect.", email);
                return;
            }

            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession session = request.getSession(true);
            session.setAttribute("loggedInUser", result.get().user());
            session.setMaxInactiveInterval(30 * 60);
            CsrfUtil.getToken(request);
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } catch (SQLException e) {
            AppLogger.logDatabaseError("Login credential lookup", e);
            showError(request, response, "We could not connect to the database. Please try again.", email);
        } catch (IllegalArgumentException e) {
            showError(request, response, "The email or password is incorrect.", email);
        }
    }

    private void showError(HttpServletRequest request, HttpServletResponse response, String message, String email)
            throws ServletException, IOException {
        request.setAttribute("error", message);
        request.setAttribute("email", email);
        request.getRequestDispatcher("/login.jsp").forward(request, response);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
