package com.smartspend;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    private final UserRepository users = new UserRepository();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String fullName = trim(request.getParameter("fullName"));
        String email = trim(request.getParameter("email")).toLowerCase();
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        if (fullName.isEmpty() || email.isEmpty() || password == null || password.isEmpty()
                || confirmPassword == null || confirmPassword.isEmpty()) {
            showError(request, response, "Please complete all fields.", fullName, email);
            return;
        }
        if (fullName.length() < 2 || fullName.length() > 100) {
            showError(request, response, "Full name must be between 2 and 100 characters.", fullName, email);
            return;
        }
        if (!email.matches("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*$")
                || email.length() > 150) {
            showError(request, response, "Enter a valid email address.", fullName, email);
            return;
        }
        if (password.length() < 8 || password.length() > 72) {
            showError(request, response, "Password must be between 8 and 72 characters.", fullName, email);
            return;
        }
        if (!password.equals(confirmPassword)) {
            showError(request, response, "Passwords do not match.", fullName, email);
            return;
        }

        try {
            if (users.emailExists(email)) {
                showError(request, response, "An account with this email already exists.", fullName, email);
                return;
            }
            String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(12));
            if (users.createUser(fullName, email, passwordHash).isPresent()) {
                response.sendRedirect(request.getContextPath() + "/login?registered=1");
            } else {
                showError(request, response, "Registration could not be completed. Please try again.", fullName, email);
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            showError(request, response, "An account with this email already exists.", fullName, email);
        } catch (SQLException e) {
            showError(request, response, "We could not connect to the database. Please try again.", fullName, email);
        }
    }

    private void showError(HttpServletRequest request, HttpServletResponse response, String message,
                           String fullName, String email) throws ServletException, IOException {
        request.setAttribute("error", message);
        request.setAttribute("fullName", fullName);
        request.setAttribute("email", email);
        request.getRequestDispatcher("/register.jsp").forward(request, response);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
