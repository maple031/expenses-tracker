package com.smartspend;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.Map;

public final class ReportPdfGenerator {
    private ReportPdfGenerator() {}

    public static void generate(ReportRepository.ReportData report, OutputStream output)
            throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                float y = 760;

                y = text(content, bold, 18, 50, y, "SmartSpend Expense Report");
                y -= 18;
                y = text(content, regular, 11, 50, y,
                        "Period: " + report.startDate() + " to " + report.endDate());
                y = text(content, regular, 11, 50, y,
                        "Total expenses: Rs. " + report.total().setScale(2).toPlainString());
                y = text(content, regular, 11, 50, y,
                        "Transaction count: " + report.transactionCount());
                y = text(content, regular, 11, 50, y,
                        "Highest expense: Rs. " + report.highestExpense().setScale(2).toPlainString());

                y -= 14;
                y = text(content, bold, 12, 50, y, "Category-wise spending");
                for (Map.Entry<String, BigDecimal> entry : report.categoryTotals().entrySet()) {
                    if (y < 80) break;
                    y = text(content, regular, 10, 50, y,
                            entry.getKey() + ": Rs. " + entry.getValue().setScale(2).toPlainString()
                                    + " (" + report.categoryPercentages().get(entry.getKey())
                                    .setScale(2).toPlainString() + "%)");
                }

                y -= 14;
                y = text(content, bold, 12, 50, y, "Expense details");
                for (Expense expense : report.expenses()) {
                    if (y < 70) break;
                    y = text(content, regular, 9, 50, y,
                            expense.getExpenseDate() + " | " + expense.getCategory()
                                    + " | " + clean(expense.getDescription())
                                    + " | Rs. " + expense.getAmount().setScale(2).toPlainString());
                }
            }
            document.save(output);
        }
    }

    private static float text(PDPageContentStream content, PDType1Font font,
                              float size, float x, float y, String value) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(value);
        content.endText();
        return y - (size + 5);
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
