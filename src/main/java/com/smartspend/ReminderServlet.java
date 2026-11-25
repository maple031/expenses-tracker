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
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

@WebServlet({
        "/reminders", "/reminders/new", "/reminders/save", "/reminders/edit",
        "/reminders/delete", "/reminders/complete"
})
public class ReminderServlet extends HttpServlet {
    private static final Set<String> CATEGORIES =
            Set.of("Food", "Travel", "Shopping", "Bills", "Education", "Health", "Other");

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
            ReminderRepository repository = new ReminderRepository();
            if ("/reminders/new".equals(path)) {
                request.setAttribute("categories", CATEGORIES);
                request.getRequestDispatcher("/reminder-form.jsp").forward(request, response);
                return;
            }
            if ("/reminders/edit".equals(path)) {
                Long reminderId = parseLong(request.getParameter("id"));
                if (reminderId == null) {
                    response.sendRedirect(request.getContextPath() + "/reminders");
                    return;
                }
                var reminder = repository.findOwnedBy(reminderId, user.getId());
                if (reminder.isEmpty()) {
                    response.sendRedirect(request.getContextPath() + "/reminders");
                    return;
                }
                request.setAttribute("reminder", reminder.get());
                request.setAttribute("categories", CATEGORIES);
                request.getRequestDispatcher("/reminder-form.jsp").forward(request, response);
                return;
            }

            showList(request, response, user, repository);
        } catch (SQLException e) {
            request.setAttribute("error", "Could not load reminders. Please try again.");
            request.setAttribute("reminders", List.of());
            request.setAttribute("categories", CATEGORIES);
            request.setAttribute("reminderSummary",
                    new ReminderRepository.ReminderSummary(0, 0, 0, 0, null));
            request.getRequestDispatcher("/reminders.jsp").forward(request, response);
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

        if ("complete".equals(request.getParameter("action"))
                || "/reminders/complete".equals(request.getServletPath())) {
            completeReminder(request, response, user);
        } else if ("delete".equals(request.getParameter("action"))
                || "/reminders/delete".equals(request.getServletPath())) {
            deleteReminder(request, response, user);
        } else {
            saveReminder(request, response, user);
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response,
                          User user, ReminderRepository repository)
            throws ServletException, IOException {
        String search = trim(request.getParameter("search"));
        String category = trim(request.getParameter("category"));
        String dateText = trim(request.getParameter("date"));
        String status = trim(request.getParameter("status"));
        LocalDate date = parseDate(dateText);
        if (CATEGORIES.contains(category) == false) category = "";
        if (!Set.of("", "UPCOMING", "TODAY", "PAST", "COMPLETED").contains(status)) status = "";

        try {
            List<Reminder> reminders = repository.findForUser(
                    user.getId(), search, category, date, status);
            ReminderRepository.ReminderSummary summary = repository.getSummary(user.getId());
            markDueAsSeen(request, repository, summary, user.getId());

            request.setAttribute("reminders", reminders);
            request.setAttribute("categories", CATEGORIES);
            request.setAttribute("reminderSummary", summary);
            request.setAttribute("search", search);
            request.setAttribute("selectedCategory", category);
            request.setAttribute("selectedDate", dateText);
            request.setAttribute("selectedStatus", status);
            request.getRequestDispatcher("/reminders.jsp").forward(request, response);
        } catch (SQLException e) {
            request.setAttribute("error", "Could not load reminders. Please try again.");
            request.setAttribute("reminders", List.of());
            request.setAttribute("categories", CATEGORIES);
            request.getRequestDispatcher("/reminders.jsp").forward(request, response);
        }
    }

    private void saveReminder(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        Long reminderId = parseLong(request.getParameter("reminderId"));
        String title = trim(request.getParameter("title"));
        String description = trim(request.getParameter("description"));
        String amountText = trim(request.getParameter("expectedAmount"));
        String category = trim(request.getParameter("category"));
        String dateText = trim(request.getParameter("reminderDate"));
        String timeText = trim(request.getParameter("reminderTime"));

        if (title.isEmpty() || title.length() > 150) {
            forwardReminderError(request, response, "Reminder title is required and must be 150 characters or fewer.",
                    reminderId);
            return;
        }
        if (description.length() > 500) {
            forwardReminderError(request, response, "Description must be 500 characters or fewer.", reminderId);
            return;
        }
        if (!CATEGORIES.contains(category)) {
            forwardReminderError(request, response, "Please select a valid reminder category.", reminderId);
            return;
        }

        LocalDate date;
        LocalTime time = null;
        BigDecimal amount = null;
        try {
            date = LocalDate.parse(dateText);
            if (!timeText.isEmpty()) time = LocalTime.parse(timeText);
            if (!amountText.isEmpty()) {
                amount = new BigDecimal(amountText);
                if (amount.compareTo(BigDecimal.ZERO) < 0
                        || amount.scale() > 2
                        || amount.compareTo(new BigDecimal("9999999999.99")) > 0) {
                    throw new IllegalArgumentException();
                }
            }
        } catch (RuntimeException e) {
            forwardReminderError(request, response,
                    "Enter a valid date, optional time and optional amount.", reminderId);
            return;
        }

        try {
            ReminderRepository repository = new ReminderRepository();
            boolean success;
            if (reminderId == null) {
                success = repository.create(user.getId(), title, description,
                        amount, category, date, time);
            } else {
                success = repository.update(reminderId, user.getId(), title, description,
                        amount, category, date, time);
            }
            if (!success) {
                forwardReminderError(request, response,
                        "The reminder could not be saved or does not belong to you.", reminderId);
                return;
            }
            response.sendRedirect(request.getContextPath() + "/reminders?success=1");
        } catch (SQLException e) {
            forwardReminderError(request, response,
                    "Could not save the reminder. Please try again.", reminderId);
        }
    }

    private void completeReminder(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {
        Long reminderId = parseLong(request.getParameter("reminderId"));
        if (reminderId != null) {
            try {
                new ReminderRepository().markCompleted(reminderId, user.getId());
            } catch (SQLException e) {
                response.sendRedirect(request.getContextPath() + "/reminders?completeError=1");
                return;
            }
        }
        response.sendRedirect(request.getContextPath() + "/reminders?completed=1");
    }

    private void deleteReminder(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {
        Long reminderId = parseLong(request.getParameter("reminderId"));
        if (reminderId != null) {
            try {
                new ReminderRepository().delete(reminderId, user.getId());
            } catch (SQLException e) {
                response.sendRedirect(request.getContextPath() + "/reminders?deleteError=1");
                return;
            }
        }
        response.sendRedirect(request.getContextPath() + "/reminders?deleted=1");
    }

    private void forwardReminderError(HttpServletRequest request, HttpServletResponse response,
                                      String message, Long reminderId)
            throws ServletException, IOException {
        request.setAttribute("error", message);
        request.setAttribute("categories", CATEGORIES);
        request.setAttribute("submittedTitle", trim(request.getParameter("title")));
        request.setAttribute("submittedDescription", trim(request.getParameter("description")));
        request.setAttribute("submittedAmount", trim(request.getParameter("expectedAmount")));
        request.setAttribute("submittedCategory", trim(request.getParameter("category")));
        request.setAttribute("submittedDate", trim(request.getParameter("reminderDate")));
        request.setAttribute("submittedTime", trim(request.getParameter("reminderTime")));
        request.setAttribute("reminderId", reminderId);
        if (reminderId != null) {
            try {
                var existing = new ReminderRepository().findOwnedBy(reminderId,
                        loggedInUser(request).getId());
                if (existing.isPresent()) request.setAttribute("reminder", existing.get());
            } catch (SQLException ignored) {}
        }
        request.getRequestDispatcher("/reminder-form.jsp").forward(request, response);
    }

    @SuppressWarnings("unchecked")
    private void markDueAsSeen(HttpServletRequest request, ReminderRepository repository,
                               ReminderRepository.ReminderSummary summary, long userId)
            throws SQLException {
        var session = request.getSession();
        java.util.Set<Long> seen =
                (java.util.Set<Long>) session.getAttribute("seenReminderIds");
        if (seen == null) {
            seen = new java.util.HashSet<>();
            session.setAttribute("seenReminderIds", seen);
        }
        List<Reminder> all = repository.findForUser(userId, null, null, null, null);
        for (Reminder reminder : all) {
            if (reminder.isDue()) seen.add(reminder.getId());
        }
        session.setAttribute("seenReminderIds", seen);
    }

    private User loggedInUser(HttpServletRequest request) {
        var session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("loggedInUser");
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException e) { return null; }
    }

    private Long parseLong(String value) {
        try { return value == null || value.isBlank() ? null : Long.parseLong(value); }
        catch (NumberFormatException e) { return null; }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
