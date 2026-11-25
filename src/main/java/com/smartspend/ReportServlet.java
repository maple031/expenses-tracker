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
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@WebServlet({"/reports", "/reports/pdf", "/reports/excel"})
public class ReportServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = loggedInUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login?required=1");
            return;
        }

        if ("pdf".equals(request.getParameter("format"))
                || "/reports/pdf".equals(request.getServletPath())) {
            downloadPdf(request, response, user);
        } else if ("excel".equals(request.getParameter("format"))
                || "/reports/excel".equals(request.getServletPath())) {
            downloadExcel(request, response, user);
        } else {
            showReport(request, response, user);
        }
    }

    private void showReport(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        String monthText = trim(request.getParameter("month"));
        String startText = trim(request.getParameter("startDate"));
        String endText = trim(request.getParameter("endDate"));
        LocalDate start;
        LocalDate end;

        if (!monthText.isEmpty()) {
            try {
                YearMonth month = YearMonth.parse(monthText);
                start = month.atDay(1);
                end = month.atEndOfMonth();
            } catch (RuntimeException e) {
                forwardReportError(request, response, "Select a valid month.");
                return;
            }
        } else if (!startText.isEmpty() || !endText.isEmpty()) {
            try {
                start = LocalDate.parse(startText);
                end = LocalDate.parse(endText);
            } catch (RuntimeException e) {
                forwardReportError(request, response, "Select both a valid start and end date.");
                return;
            }
        } else {
            YearMonth current = YearMonth.now();
            start = current.atDay(1);
            end = current.atEndOfMonth();
            monthText = current.toString();
        }

        if (end.isBefore(start)) {
            forwardReportError(request, response, "The end date must be on or after the start date.");
            return;
        }

        try {
            ReportRepository.ReportData report =
                    new ReportRepository().getReport(user.getId(), start, end);
            request.setAttribute("report", report);
            request.setAttribute("selectedMonth", monthText);
            request.setAttribute("selectedStart", start.toString());
            request.setAttribute("selectedEnd", end.toString());
            request.getRequestDispatcher("/reports.jsp").forward(request, response);
        } catch (IllegalArgumentException e) {
            forwardReportError(request, response, "Select a valid report period.");
        } catch (SQLException e) {
            AppLogger.logDatabaseError("Loading expense report", e);
            forwardReportError(request, response, "Could not load the report. Please try again.");
        }
    }

    private void downloadPdf(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {
        try {
            DateRange range = resolveRange(request);
            ReportRepository.ReportData report =
                    new ReportRepository().getReport(user.getId(), range.start(), range.end());
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"SmartSpend-report-"
                            + range.start() + "-to-" + range.end() + ".pdf\"");
            ReportPdfGenerator.generate(report, response.getOutputStream());
        } catch (IllegalArgumentException e) {
            AppLogger.logUnexpectedError("PDF report parameters", e);
            response.sendRedirect(request.getContextPath() + "/reports?error="
                    + java.net.URLEncoder.encode("Select valid report dates.",
                    java.nio.charset.StandardCharsets.UTF_8));
        } catch (SQLException e) {
            AppLogger.logDatabaseError("Generating PDF report", e);
            response.sendRedirect(request.getContextPath() + "/reports?error="
                    + java.net.URLEncoder.encode("Could not generate the PDF report.",
                    java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException e) {
            AppLogger.logUnexpectedError("Writing PDF report", e);
            response.sendRedirect(request.getContextPath() + "/reports?error="
                    + java.net.URLEncoder.encode("Could not download the PDF report.",
                    java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private void downloadExcel(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {
        try {
            DateRange range = resolveRange(request);
            ReportRepository.ReportData report =
                    new ReportRepository().getReport(user.getId(), range.start(), range.end());
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"SmartSpend-report-"
                            + range.start() + "-to-" + range.end() + ".xlsx\"");
            ReportExcelGenerator.generate(report, response.getOutputStream());
        } catch (IllegalArgumentException e) {
            AppLogger.logUnexpectedError("Excel report parameters", e);
            response.sendRedirect(request.getContextPath() + "/reports?error="
                    + java.net.URLEncoder.encode("Select valid report dates.",
                    java.nio.charset.StandardCharsets.UTF_8));
        } catch (SQLException e) {
            AppLogger.logDatabaseError("Generating Excel report", e);
            response.sendRedirect(request.getContextPath() + "/reports?error="
                    + java.net.URLEncoder.encode("Could not generate the Excel report.",
                    java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException e) {
            AppLogger.logUnexpectedError("Writing Excel report", e);
            response.sendRedirect(request.getContextPath() + "/reports?error="
                    + java.net.URLEncoder.encode("Could not download the Excel report.",
                    java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private DateRange resolveRange(HttpServletRequest request) {
        String monthText = trim(request.getParameter("month"));
        String startText = trim(request.getParameter("startDate"));
        String endText = trim(request.getParameter("endDate"));
        if (!monthText.isEmpty()) {
            try {
                YearMonth month = YearMonth.parse(monthText);
                return new DateRange(month.atDay(1), month.atEndOfMonth());
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Select a valid month.");
            }
        }
        if (startText.isEmpty() || endText.isEmpty()) {
            throw new IllegalArgumentException("Select both report dates.");
        }
        try {
            LocalDate start = LocalDate.parse(startText);
            LocalDate end = LocalDate.parse(endText);
            if (end.isBefore(start)) {
                throw new IllegalArgumentException("The end date must be on or after the start date.");
            }
            return new DateRange(start, end);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Select valid report dates.");
        }
    }

    private void forwardReportError(HttpServletRequest request, HttpServletResponse response,
                                    String message) throws ServletException, IOException {
        request.setAttribute("error", message);
        request.setAttribute("selectedMonth", trim(request.getParameter("month")));
        request.setAttribute("selectedStart", trim(request.getParameter("startDate")));
        request.setAttribute("selectedEnd", trim(request.getParameter("endDate")));
        request.getRequestDispatcher("/reports.jsp").forward(request, response);
    }

    private User loggedInUser(HttpServletRequest request) {
        var session = request.getSession(false);
        return session == null ? null : (User) session.getAttribute("loggedInUser");
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private record DateRange(LocalDate start, LocalDate end) {}
}
