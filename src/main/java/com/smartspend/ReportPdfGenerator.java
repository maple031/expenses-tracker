package com.smartspend;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ReportPdfGenerator {
    private static final float LEFT_MARGIN = 50;
    private static final float BOTTOM_MARGIN = 50;
    private static final float TOP_MARGIN = 760;
    private ReportPdfGenerator() {}

    public static void generate(ReportRepository.ReportData report, OutputStream output)
            throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PdfPageWriter writer = new PdfPageWriter(document, bold, regular);

            writer.text(bold, 18, "SmartSpend Expense Report");
            writer.text(regular, 11, "Period: " + report.startDate() + " to " + report.endDate());
            writer.text(regular, 11,
                    "Total expenses: Rs. " + report.total().setScale(2).toPlainString());
            writer.text(regular, 11,
                    "Transaction count: " + report.transactionCount());
            writer.text(regular, 11,
                    "Highest expense: Rs. " + report.highestExpense().setScale(2).toPlainString());

            writer.blank(14);
            writer.text(bold, 12, "Category-wise spending");
            if (report.categoryTotals().isEmpty()) {
                writer.text(regular, 10, "No category spending for this period.");
            } else {
                for (Map.Entry<String, BigDecimal> entry : report.categoryTotals().entrySet()) {
                    writer.ensureSpace(16);
                    String line = clean(entry.getKey()) + ": Rs. "
                            + entry.getValue().setScale(2).toPlainString()
                            + " (" + report.categoryPercentages().get(entry.getKey())
                            .setScale(2).toPlainString() + "%)";
                    writer.wrapText(regular, 10, line, 100);
                }
            }

            writer.blank(14);
            writer.startExpenseSection(report);
            if (report.expenses().isEmpty()) {
                writer.text(regular, 10, "No expense details for this period.");
            } else {
                for (Expense expense : report.expenses()) {
                    String line = expense.getExpenseDate() + " | "
                            + clean(expense.getCategory()) + " | "
                            + clean(expense.getDescription()) + " | Rs. "
                            + expense.getAmount().setScale(2).toPlainString();
                    writer.wrapExpenseLine(line);
                }
            }
            writer.close();
            document.save(output);
        }
    }

    private static final class PdfPageWriter {
        private final PDDocument document;
        private final PDType1Font bold;
        private final PDType1Font regular;
        private PDPageContentStream content;
        private float y = TOP_MARGIN;
        private int pageNumber = 0;
        private int expenseHeadingPage = 0;

        private PdfPageWriter(PDDocument document, PDType1Font bold, PDType1Font regular)
                throws IOException {
            this.document = document;
            this.bold = bold;
            this.regular = regular;
            newPage(false);
        }

        private void newPage(boolean continued) throws IOException {
            if (content != null) {
                content.close();
            }
            PDPage page = new PDPage();
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            pageNumber++;
            y = TOP_MARGIN;
            if (continued) {
                text(bold, 12, "SmartSpend Expense Report - continued");
                blank(8);
            }
        }

        private void ensureSpace(float needed) throws IOException {
            if (y - needed < BOTTOM_MARGIN) {
                newPage(true);
            }
        }

        private void blank(float amount) throws IOException {
            ensureSpace(amount);
            y -= amount;
        }

        private void text(PDType1Font font, float size, String value) throws IOException {
            wrapText(font, size, value, 105);
        }

        private void wrapText(PDType1Font font, float size, String value, int estimatedCharacters)
                throws IOException {
            for (String line : wrap(value, estimatedCharacters)) {
                ensureSpace(size + 5);
                content.beginText();
                content.setFont(font, size);
                content.newLineAtOffset(LEFT_MARGIN, y);
                content.showText(line);
                content.endText();
                y -= size + 5;
            }
        }

        private void startExpenseSection(ReportRepository.ReportData report) throws IOException {
            ensureSpace(45);
            text(bold, 12, "Expense details");
            ensureSpace(20);
            text(bold, 9, "Date | Category | Description | Amount");
            ensureSpace(10);
            text(regular, 8, "Period: " + report.startDate() + " to " + report.endDate());
            expenseHeadingPage = pageNumber;
        }

        private void wrapExpenseLine(String value) throws IOException {
            for (String line : wrap(value, 82)) {
                ensureSpace(23);
                if (expenseHeadingPage != pageNumber) {
                    text(bold, 9, "Expense details - continued");
                    text(bold, 9, "Date | Category | Description | Amount");
                    expenseHeadingPage = pageNumber;
                }
                text(regular, 9, line);
            }
        }

        private void close() throws IOException {
            if (content != null) {
                content.close();
                content = null;
            }
        }

        private List<String> wrap(String value, int maximumCharacters) {
            List<String> lines = new ArrayList<>();
            if (value == null || value.isBlank()) {
                lines.add("");
                return lines;
            }

            String remaining = clean(value);
            while (remaining.length() > maximumCharacters) {
                int split = remaining.lastIndexOf(' ', maximumCharacters);
                if (split < maximumCharacters / 2) {
                    split = maximumCharacters;
                }
                lines.add(remaining.substring(0, split).trim());
                remaining = remaining.substring(split).trim();
            }
            if (!remaining.isEmpty()) {
                lines.add(remaining);
            }
            return lines;
        }
    }

    private static String clean(String value) {
        if (value == null) return "";
        String cleaned = value.replace('\n', ' ').replace('\r', ' ');
        StringBuilder result = new StringBuilder();
        for (char character : cleaned.toCharArray()) {
            result.append(character <= 127 ? character : '?');
        }
        return result.toString();
    }
}
