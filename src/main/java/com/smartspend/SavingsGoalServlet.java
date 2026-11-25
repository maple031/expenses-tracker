package com.smartspend;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

@WebServlet({"/goals", "/goals/save", "/goals/edit", "/goals/delete", "/goals/add-savings", "/goals/new"})
public class SavingsGoalServlet extends HttpServlet {
    private static final Set<String> CATEGORIES =
            Set.of("Travel", "Education", "Shopping", "Emergency", "Other");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = loggedInUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login?required=1");
            return;
        }

        String path = request.getServletPath();
        try {
            SavingsGoalRepository repository = new SavingsGoalRepository();
            if ("/goals/new".equals(path)) {
                request.setAttribute("categories", CATEGORIES);
                request.getRequestDispatcher("/goal-form.jsp").forward(request, response);
                return;
            }
            if ("/goals/edit".equals(path)) {
                Long goalId = parseLong(request.getParameter("id"));
                if (goalId == null) {
                    response.sendRedirect(request.getContextPath() + "/goals");
                    return;
                }
                var goal = repository.findOwnedBy(goalId, user.getId());
                if (goal.isEmpty()) {
                    response.sendRedirect(request.getContextPath() + "/goals");
                    return;
                }
                request.setAttribute("goal", goal.get());
                request.setAttribute("categories", CATEGORIES);
                request.getRequestDispatcher("/goal-form.jsp").forward(request, response);
                return;
            }

            request.setAttribute("goals", repository.findForUser(user.getId()));
            request.setAttribute("categories", CATEGORIES);
            request.getRequestDispatcher("/goals.jsp").forward(request, response);
        } catch (SQLException e) {
            AppLogger.logDatabaseError("Loading savings goals", e);
            request.setAttribute("error", "Could not load savings goals. Please try again.");
            request.setAttribute("goals", List.of());
            request.setAttribute("categories", CATEGORIES);
            request.getRequestDispatcher("/goals.jsp").forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User user = loggedInUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login?required=1");
            return;
        }

        String path = request.getServletPath();
        if ("/goals/delete".equals(path)) {
            deleteGoal(request, response, user);
        } else if ("/goals/add-savings".equals(path)) {
            addSavings(request, response, user);
        } else {
            saveGoal(request, response, user);
        }
    }

    private void saveGoal(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        Long goalId = parseLong(request.getParameter("goalId"));
        String name = trim(request.getParameter("goalName"));
        String targetText = trim(request.getParameter("targetAmount"));
        String savedText = trim(request.getParameter("savedAmount"));
        String dateText = trim(request.getParameter("targetDate"));
        String description = trim(request.getParameter("description"));
        String category = trim(request.getParameter("category"));

        if (name.isEmpty() || name.length() > 120) {
            forwardGoalError(request, response, "Goal name is required and must be 120 characters or fewer.",
                    goalId);
            return;
        }
        if (!CATEGORIES.contains(category)) {
            forwardGoalError(request, response, "Please select a valid goal category.", goalId);
            return;
        }
        if (description.length() > 500) {
            forwardGoalError(request, response, "Description must be 500 characters or fewer.", goalId);
            return;
        }

        BigDecimal target;
        BigDecimal initialSaved = BigDecimal.ZERO;
        LocalDate targetDate;
        try {
            target = new BigDecimal(targetText);
            if (goalId == null && !savedText.isEmpty()) {
                initialSaved = new BigDecimal(savedText);
            }
            targetDate = LocalDate.parse(dateText);
        } catch (RuntimeException e) {
            forwardGoalError(request, response, "Enter valid target amount, initial savings and target date.",
                    goalId);
            return;
        }
        if (target.compareTo(BigDecimal.ZERO) <= 0
                || target.scale() > 2
                || target.compareTo(new BigDecimal("9999999999.99")) > 0) {
            forwardGoalError(request, response, "Target amount must be greater than zero.", goalId);
            return;
        }
        if (initialSaved.compareTo(BigDecimal.ZERO) < 0
                || initialSaved.compareTo(target) > 0
                || initialSaved.scale() > 2) {
            forwardGoalError(request, response,
                    "Initial saved amount must be at least zero and cannot exceed the target.", goalId);
            return;
        }

        try {
            SavingsGoalRepository repository = new SavingsGoalRepository();
            boolean success;
            if (goalId == null) {
                success = repository.create(user.getId(), name, target, initialSaved,
                        targetDate, description, category);
            } else {
                success = repository.update(goalId, user.getId(), name, target,
                        targetDate, description, category);
            }
            if (!success) {
                forwardGoalError(request, response,
                        "The goal could not be saved. Saved amount cannot exceed a reduced target.",
                        goalId);
                return;
            }
            response.sendRedirect(request.getContextPath() + "/goals?success=1");
        } catch (SQLException e) {
            AppLogger.logDatabaseError("Saving savings goal", e);
            forwardGoalError(request, response, "Could not save the goal. Please try again.", goalId);
        }
    }

    private void addSavings(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        Long goalId = parseLong(request.getParameter("goalId"));
        String amountText = trim(request.getParameter("addAmount"));
        if (goalId == null) {
            response.sendRedirect(request.getContextPath() + "/goals");
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountText);
        } catch (RuntimeException e) {
            response.sendRedirect(request.getContextPath() + "/goals?addError=invalid");
            return;
        }
        try {
            SavingsGoalRepository repository = new SavingsGoalRepository();
            String error = repository.addSavings(goalId, user.getId(), amount);
            if (error != null) {
                response.sendRedirect(request.getContextPath() + "/goals?addError="
                        + java.net.URLEncoder.encode(error, java.nio.charset.StandardCharsets.UTF_8));
            } else {
                response.sendRedirect(request.getContextPath() + "/goals?added=1");
            }
        } catch (SQLException e) {
            AppLogger.logDatabaseError("Adding savings to goal", e);
            response.sendRedirect(request.getContextPath() + "/goals?addError="
                    + java.net.URLEncoder.encode("Could not add savings. Please try again.",
                    java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private void deleteGoal(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {
        Long goalId = parseLong(request.getParameter("goalId"));
        if (goalId != null) {
            try {
                new SavingsGoalRepository().delete(goalId, user.getId());
            } catch (SQLException e) {
                AppLogger.logDatabaseError("Deleting savings goal", e);
                response.sendRedirect(request.getContextPath() + "/goals?deleteError=1");
                return;
            }
        }
        response.sendRedirect(request.getContextPath() + "/goals?deleted=1");
    }

    private void forwardGoalError(HttpServletRequest request, HttpServletResponse response,
                                  String message, Long goalId)
            throws ServletException, IOException {
        request.setAttribute("error", message);
        request.setAttribute("categories", CATEGORIES);
        request.setAttribute("submittedGoalName", trim(request.getParameter("goalName")));
        request.setAttribute("submittedTargetAmount", trim(request.getParameter("targetAmount")));
        request.setAttribute("submittedSavedAmount", trim(request.getParameter("savedAmount")));
        request.setAttribute("submittedTargetDate", trim(request.getParameter("targetDate")));
        request.setAttribute("submittedDescription", trim(request.getParameter("description")));
        request.setAttribute("submittedCategory", trim(request.getParameter("category")));
        request.setAttribute("goalId", goalId);

        if (goalId != null) {
            try {
                var existing = new SavingsGoalRepository().findOwnedBy(goalId,
                        loggedInUser(request).getId());
                if (existing.isPresent()) {
                    request.setAttribute("goal", existing.get());
                }
            } catch (SQLException e) {
                AppLogger.logDatabaseError("Reloading savings goal after form error", e);
            }
        }
        request.getRequestDispatcher("/goal-form.jsp").forward(request, response);
    }

    private User loggedInUser(HttpServletRequest request) {
        var session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("loggedInUser");
    }

    private Long parseLong(String value) {
        try {
            return value == null || value.isBlank() ? null : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
