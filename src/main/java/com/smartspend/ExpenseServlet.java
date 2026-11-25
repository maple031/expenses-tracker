package com.smartspend;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

@WebServlet({"/expenses", "/expenses/add", "/expenses/save", "/expenses/delete", "/expenses/edit"})
public class ExpenseServlet extends HttpServlet {
    private static final Set<String> CATEGORIES = Set.of(
            "Food", "Travel", "Shopping", "Education", "Bills",
            "Health", "Entertainment", "Other");
    private final ExpenseRepository expenses = new ExpenseRepository();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = loggedInUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login?required=1");
            return;
        }

        String path = request.getServletPath();
        if ("/expenses/add".equals(path)) {
            request.setAttribute("categories", CATEGORIES);
            request.getRequestDispatcher("/expense-form.jsp").forward(request, response);
            return;
        }
        if ("/expenses/edit".equals(path)) {
            showEdit(request, response, user);
            return;
        }
        showList(request, response, user);
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

        if ("delete".equals(request.getParameter("action"))
                || "/expenses/delete".equals(request.getServletPath())) {
            deleteExpense(request, response, user);
        } else {
            saveExpense(request, response, user);
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        String search = trim(request.getParameter("search"));
        String category = trim(request.getParameter("category"));
        String dateFromText = trim(request.getParameter("dateFrom"));
        String dateToText = trim(request.getParameter("dateTo"));
        String monthText = trim(request.getParameter("month"));

        LocalDate dateFrom = parseDate(dateFromText);
        LocalDate dateTo = parseDate(dateToText);
        YearMonth month = parseMonth(monthText);

        try {
            List<Expense> expenseList = expenses.findForUser(
                    user.getId(), search,
                    CATEGORIES.contains(category) ? category : "",
                    dateFrom, dateTo, month);
            request.setAttribute("expenses", expenseList);
            request.setAttribute("categories", CATEGORIES);
            request.setAttribute("search", search);
            request.setAttribute("selectedCategory", CATEGORIES.contains(category) ? category : "");
            request.setAttribute("dateFrom", dateFromText);
            request.setAttribute("dateTo", dateToText);
            request.setAttribute("month", monthText);
            request.getRequestDispatcher("/expenses.jsp").forward(request, response);
        } catch (SQLException e) {
            forwardDatabaseError(request, response, "Could not load expenses. Please try again.");
        }
    }

    private void showEdit(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        Long expenseId = parseLong(request.getParameter("id"));
        if (expenseId == null) {
            response.sendRedirect(request.getContextPath() + "/expenses");
            return;
        }
        try {
            var expense = expenses.findOwnedBy(expenseId, user.getId());
            if (expense.isEmpty()) {
                response.sendRedirect(request.getContextPath() + "/expenses");
                return;
            }
            request.setAttribute("expense", expense.get());
            request.setAttribute("categories", CATEGORIES);
            request.getRequestDispatcher("/expense-form.jsp").forward(request, response);
        } catch (SQLException e) {
            forwardDatabaseError(request, response, "Could not load that expense.");
        }
    }

    private void saveExpense(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        Long expenseId = parseLong(request.getParameter("expenseId"));
        String description = trim(request.getParameter("description"));
        String amountText = trim(request.getParameter("amount"));
        String category = trim(request.getParameter("category"));
        String dateText = trim(request.getParameter("expenseDate"));

        if (description.isEmpty() || description.length() > 150) {
            forwardFormError(request, response, expenseId,
                    "Description is required and must be 150 characters or fewer.");
            return;
        }
        if (!CATEGORIES.contains(category)) {
            forwardFormError(request, response, expenseId, "Please select a valid category.");
            return;
        }

        BigDecimal amount;
        LocalDate date;
        try {
            amount = new BigDecimal(amountText);
            date = LocalDate.parse(dateText);
        } catch (RuntimeException e) {
            forwardFormError(request, response, expenseId, "Enter a valid amount and date.");
            return;
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0 || amount.scale() > 2
                || amount.compareTo(new BigDecimal("9999999999.99")) > 0) {
            forwardFormError(request, response, expenseId, "Enter an amount greater than zero with at most 2 decimal places.");
            return;
        }

        try {
            boolean success;
            if (expenseId == null) {
                success = expenses.create(user.getId(), description, amount, category, date);
            } else {
                success = expenses.update(expenseId, user.getId(), description, amount, category, date);
            }
            if (!success) {
                forwardFormError(request, response, expenseId,
                        "The expense could not be saved. It may no longer belong to you.");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/expenses?success=1");
        } catch (SQLException e) {
            forwardFormError(request, response, expenseId,
                    "Could not save the expense. Please try again.");
        }
    }

    private void deleteExpense(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        Long expenseId = parseLong(request.getParameter("expenseId"));
        if (expenseId == null) {
            response.sendRedirect(request.getContextPath() + "/expenses");
            return;
        }
        try {
            expenses.delete(expenseId, user.getId());
            response.sendRedirect(request.getContextPath() + "/expenses?deleted=1");
        } catch (SQLException e) {
            forwardDatabaseError(request, response, "Could not delete the expense. Please try again.");
        }
    }

    private void forwardFormError(HttpServletRequest request, HttpServletResponse response,
                                  Long expenseId, String message)
            throws ServletException, IOException {
        request.setAttribute("error", message);
        request.setAttribute("categories", CATEGORIES);
        if (expenseId != null) {
            try {
                var existing = expenses.findOwnedBy(expenseId, loggedInUser(request).getId());
                if (existing.isPresent()) {
                    request.setAttribute("expense", existing.get());
                }
            } catch (SQLException ignored) {
                // The submitted values below remain available for correction.
            }
        }
        request.setAttribute("submittedDescription", trim(request.getParameter("description")));
        request.setAttribute("submittedAmount", trim(request.getParameter("amount")));
        request.setAttribute("submittedCategory", trim(request.getParameter("category")));
        request.setAttribute("submittedDate", trim(request.getParameter("expenseDate")));
        request.setAttribute("expenseId", expenseId);
        request.getRequestDispatcher("/expense-form.jsp").forward(request, response);
    }

    private void forwardDatabaseError(HttpServletRequest request, HttpServletResponse response,
                                      String message) throws ServletException, IOException {
        request.setAttribute("error", message);
        request.setAttribute("expenses", List.of());
        request.setAttribute("categories", CATEGORIES);
        request.getRequestDispatcher("/expenses.jsp").forward(request, response);
    }

    private User loggedInUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("loggedInUser");
    }

    private LocalDate parseDate(String value) {
        if (value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private YearMonth parseMonth(String value) {
        if (value.isBlank()) {
            return null;
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
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
