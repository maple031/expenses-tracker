package com.smartspend;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.YearMonth;
import java.math.BigDecimal;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = (User) request.getSession(false).getAttribute("loggedInUser");
        ExpenseRepository repository = new ExpenseRepository();
        BudgetRepository budgetRepository = new BudgetRepository();
        try {
            ReminderRepository reminderRepository = new ReminderRepository();
            request.setAttribute("expenseSummary", repository.getCurrentMonthSummary(user.getId()));
            request.setAttribute("budgetSummary",
                    budgetRepository.getSummary(user.getId(), YearMonth.now()));
            request.setAttribute("savingsSummary",
                    new SavingsGoalRepository().getSummary(user.getId()));
            ReminderRepository.ReminderSummary reminderSummary =
                    reminderRepository.getSummary(user.getId());
            request.setAttribute("reminderSummary", reminderSummary);
            request.setAttribute("savedPlans",
                    new SmartPlanRepository().findForUser(user.getId()));

            @SuppressWarnings("unchecked")
            Set<Long> seen = (Set<Long>) request.getSession(false).getAttribute("seenReminderIds");
            if (seen == null) {
                seen = new HashSet<>();
            }
            int unseen = 0;
            List<Reminder> allReminders = reminderRepository.findForUser(
                    user.getId(), null, null, null, null);
            for (Reminder reminder : allReminders) {
                if (reminder.isDue() && !seen.contains(reminder.getId())) {
                    unseen++;
                }
                if (reminder.isDue()) seen.add(reminder.getId());
            }
            request.getSession(false).setAttribute("seenReminderIds", seen);
            request.setAttribute("unseenReminderCount", unseen);
        } catch (SQLException e) {
            AppLogger.logDatabaseError("Loading dashboard summaries", e);
            request.setAttribute("expenseSummary",
                    new ExpenseRepository.Summary(
                            java.math.BigDecimal.ZERO, 0, java.math.BigDecimal.ZERO,
                            java.util.Map.of()));
            request.setAttribute("budgetSummary",
                    new BudgetRepository.BudgetSummary(
                            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                            BigDecimal.ZERO, "NORMAL", "Budget data is temporarily unavailable.", "normal"));
            request.setAttribute("savingsSummary",
                    new SavingsGoalRepository.SavingsSummary(
                            0, BigDecimal.ZERO, BigDecimal.ZERO, java.util.List.of(),
                            BigDecimal.ZERO));
            request.setAttribute("reminderSummary",
                    new ReminderRepository.ReminderSummary(0, 0, 0, 0, null));
            request.setAttribute("unseenReminderCount", 0);
            request.setAttribute("savedPlans", java.util.List.of());
            request.setAttribute("summaryError", "Expense summary is temporarily unavailable.");
        }
        request.getRequestDispatcher("/dashboard.jsp").forward(request, response);
    }
}
