package com.smartspend;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@WebServlet({"/budgets", "/budgets/save"})
public class BudgetServlet extends HttpServlet {
    private final BudgetRepository budgets = new BudgetRepository();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = loggedInUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login?required=1");
            return;
        }

        YearMonth month = parseMonth(request.getParameter("month"));
        if (month == null) {
            month = YearMonth.now();
        }
        try {
            BudgetRepository.BudgetSummary summary = budgets.getSummary(user.getId(), month);
            Budget budget = budgets.find(user.getId(), month).orElse(null);
            request.setAttribute("budget", budget);
            request.setAttribute("budgetSummary", summary);
            request.setAttribute("selectedMonth", month.toString());
            request.getRequestDispatcher("/budgets.jsp").forward(request, response);
        } catch (SQLException e) {
            AppLogger.logDatabaseError("Loading budget", e);
            request.setAttribute("error", "Could not load the budget. Please try again.");
            request.setAttribute("selectedMonth", month.toString());
            request.setAttribute("budgetSummary",
                    new BudgetRepository.BudgetSummary(
                            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                            BigDecimal.ZERO, "NORMAL", "Budget data is temporarily unavailable.", "normal"));
            request.getRequestDispatcher("/budgets.jsp").forward(request, response);
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

        String monthText = trim(request.getParameter("month"));
        String amountText = trim(request.getParameter("monthlyLimit"));
        YearMonth month = parseMonth(monthText);
        BigDecimal amount;

        if (month == null) {
            forwardError(request, response, monthText, "Select a valid month.");
            return;
        }
        try {
            amount = new BigDecimal(amountText);
        } catch (RuntimeException e) {
            forwardError(request, response, monthText, "Enter a valid budget amount.");
            return;
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0 || amount.scale() > 2
                || amount.compareTo(new BigDecimal("9999999999.99")) > 0) {
            forwardError(request, response, monthText,
                    "Enter an amount greater than zero with at most 2 decimal places.");
            return;
        }

        try {
            if (!budgets.save(user.getId(), month, amount)) {
                forwardError(request, response, monthText,
                        "The budget could not be saved. Please try again.");
                return;
            }
            response.sendRedirect(request.getContextPath()
                    + "/budgets?month=" + month + "&success=1");
        } catch (SQLException e) {
            AppLogger.logDatabaseError("Saving budget", e);
            forwardError(request, response, monthText,
                    "Could not save the budget. Please try again.");
        }
    }

    private void forwardError(HttpServletRequest request, HttpServletResponse response,
                              String monthText, String message)
            throws ServletException, IOException {
        request.setAttribute("error", message);
        request.setAttribute("selectedMonth",
                monthText == null || monthText.isBlank()
                        ? YearMonth.now().toString() : monthText);
        request.setAttribute("budgetSummary",
                new BudgetRepository.BudgetSummary(
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, "NORMAL", "Set a monthly budget to track progress.", "normal"));
        request.getRequestDispatcher("/budgets.jsp").forward(request, response);
    }

    private User loggedInUser(HttpServletRequest request) {
        var session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("loggedInUser");
    }

    private YearMonth parseMonth(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
