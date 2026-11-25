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
import java.util.List;
import java.util.Set;

@WebServlet({
        "/planner", "/planner/save", "/planner/analyze", "/planner/edit",
        "/planner/delete"
})
public class SmartPlannerServlet extends HttpServlet {
    private static final Set<String> ACTIVITY_TYPES =
            Set.of("Travel", "Shopping", "Entertainment", "Education", "Other");

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
            SmartPlanRepository repository = new SmartPlanRepository();
            if ("/planner/edit".equals(path)) {
                Long planId = parseLong(request.getParameter("id"));
                if (planId == null) {
                    response.sendRedirect(request.getContextPath() + "/planner");
                    return;
                }
                var plan = repository.findOwnedBy(planId, user.getId());
                if (plan.isEmpty()) {
                    response.sendRedirect(request.getContextPath() + "/planner");
                    return;
                }
                request.setAttribute("plan", plan.get());
                request.setAttribute("activityTypes", ACTIVITY_TYPES);
                request.getRequestDispatcher("/planner.jsp").forward(request, response);
                return;
            }
            request.setAttribute("plans", repository.findForUser(user.getId()));
            request.setAttribute("activityTypes", ACTIVITY_TYPES);
            request.getRequestDispatcher("/planner.jsp").forward(request, response);
        } catch (SQLException e) {
            request.setAttribute("error", "Could not load Smart Planner. Please try again.");
            request.setAttribute("plans", List.of());
            request.setAttribute("activityTypes", ACTIVITY_TYPES);
            request.getRequestDispatcher("/planner.jsp").forward(request, response);
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

        if ("delete".equals(request.getParameter("action"))
                || "/planner/delete".equals(request.getServletPath())) {
            deletePlan(request, response, user);
        } else {
            saveOrAnalyze(request, response, user);
        }
    }

    private void saveOrAnalyze(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        Long planId = parseLong(request.getParameter("planId"));
        String type = trim(request.getParameter("activityType"));
        String name = trim(request.getParameter("activityName"));
        String costText = trim(request.getParameter("estimatedCost"));
        String dateText = trim(request.getParameter("plannedDate"));
        String description = trim(request.getParameter("description"));

        if (!ACTIVITY_TYPES.contains(type)) {
            forwardError(request, response, "Please select a valid activity type.", planId);
            return;
        }
        if (name.isEmpty() || name.length() > 150) {
            forwardError(request, response, "Activity name is required and must be 150 characters or fewer.", planId);
            return;
        }
        if (description.length() > 500) {
            forwardError(request, response, "Description must be 500 characters or fewer.", planId);
            return;
        }

        BigDecimal cost;
        LocalDate date;
        try {
            cost = new BigDecimal(costText);
            date = LocalDate.parse(dateText);
        } catch (RuntimeException e) {
            forwardError(request, response, "Enter a valid cost and planned date.", planId);
            return;
        }
        if (cost.compareTo(BigDecimal.ZERO) <= 0
                || cost.scale() > 2
                || cost.compareTo(new BigDecimal("9999999999.99")) > 0) {
            forwardError(request, response, "Estimated cost must be greater than zero.", planId);
            return;
        }

        try {
            SmartPlanRepository repository = new SmartPlanRepository();
            SmartPlanRepository.SmartRecommendation recommendation =
                    new SmartPlannerEngine().analyze(user.getId(), type, cost, date);

            if ("/planner/analyze".equals(request.getServletPath())
                    || "analyze".equals(request.getParameter("action"))) {
                request.setAttribute("analysis", recommendation);
                request.setAttribute("submittedType", type);
                request.setAttribute("submittedName", name);
                request.setAttribute("submittedCost", costText);
                request.setAttribute("submittedDate", dateText);
                request.setAttribute("submittedDescription", description);
                request.setAttribute("activityTypes", ACTIVITY_TYPES);
                request.setAttribute("plans", repository.findForUser(user.getId()));
                request.getRequestDispatcher("/planner.jsp").forward(request, response);
                return;
            }

            boolean success;
            if (planId == null) {
                success = repository.create(user.getId(), type, name, cost, date,
                        description, recommendation);
            } else {
                success = repository.update(planId, user.getId(), type, name, cost, date,
                        description, recommendation);
            }
            if (!success) {
                forwardError(request, response,
                        "The plan could not be saved or does not belong to you.", planId);
                return;
            }
            response.sendRedirect(request.getContextPath() + "/planner?saved=1");
        } catch (SQLException e) {
            forwardError(request, response, "Could not save or analyze the plan. Please try again.", planId);
        }
    }

    private void deletePlan(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {
        Long planId = parseLong(request.getParameter("planId"));
        if (planId != null) {
            try {
                new SmartPlanRepository().delete(planId, user.getId());
            } catch (SQLException e) {
                response.sendRedirect(request.getContextPath() + "/planner?deleteError=1");
                return;
            }
        }
        response.sendRedirect(request.getContextPath() + "/planner?deleted=1");
    }

    private void forwardError(HttpServletRequest request, HttpServletResponse response,
                              String message, Long planId)
            throws ServletException, IOException {
        request.setAttribute("error", message);
        request.setAttribute("activityTypes", ACTIVITY_TYPES);
        request.setAttribute("submittedType", trim(request.getParameter("activityType")));
        request.setAttribute("submittedName", trim(request.getParameter("activityName")));
        request.setAttribute("submittedCost", trim(request.getParameter("estimatedCost")));
        request.setAttribute("submittedDate", trim(request.getParameter("plannedDate")));
        request.setAttribute("submittedDescription", trim(request.getParameter("description")));
        request.setAttribute("planId", planId);
        if (planId != null) {
            try {
                var plan = new SmartPlanRepository().findOwnedBy(planId, loggedInUser(request).getId());
                if (plan.isPresent()) request.setAttribute("plan", plan.get());
            } catch (SQLException ignored) {}
        } else {
            try {
                request.setAttribute("plans",
                        new SmartPlanRepository().findForUser(loggedInUser(request).getId()));
            } catch (SQLException ignored) {}
        }
        request.getRequestDispatcher("/planner.jsp").forward(request, response);
    }

    private User loggedInUser(HttpServletRequest request) {
        var session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("loggedInUser");
    }

    private Long parseLong(String value) {
        try { return value == null || value.isBlank() ? null : Long.parseLong(value); }
        catch (NumberFormatException e) { return null; }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
