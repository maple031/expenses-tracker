package com.smartspend;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public final class ReportExcelGenerator {
    private ReportExcelGenerator() {}

    public static void generate(ReportRepository.ReportData report, OutputStream output)
            throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet summary = workbook.createSheet("Summary");
            XSSFSheet details = workbook.createSheet("Expense Details");

            XSSFCellStyle header = workbook.createCellStyle();
            header.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(
                    new java.awt.Color(102, 87, 217), null));
            header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            header.setAlignment(HorizontalAlignment.CENTER);

            int rowNumber = 0;
            XSSFRow title = summary.createRow(rowNumber++);
            title.createCell(0).setCellValue("SmartSpend Expense Report");
            rowNumber++;
            summary.createRow(rowNumber++).createCell(0).setCellValue(
                    "Period: " + report.startDate() + " to " + report.endDate());
            summary.createRow(rowNumber++).createCell(0).setCellValue(
                    "Total expenses");
            summary.getRow(rowNumber - 1).createCell(1).setCellValue(
                    report.total().doubleValue());
            summary.createRow(rowNumber++).createCell(0).setCellValue("Transaction count");
            summary.getRow(rowNumber - 1).createCell(1).setCellValue(report.transactionCount());
            summary.createRow(rowNumber++).createCell(0).setCellValue("Highest expense");
            summary.getRow(rowNumber - 1).createCell(1).setCellValue(
                    report.highestExpense().doubleValue());

            rowNumber++;
            XSSFRow categoryHeader = summary.createRow(rowNumber++);
            categoryHeader.createCell(0).setCellValue("Category");
            categoryHeader.createCell(1).setCellValue("Amount");
            categoryHeader.createCell(2).setCellValue("Percentage");
            categoryHeader.getCell(0).setCellStyle(header);
            categoryHeader.getCell(1).setCellStyle(header);
            categoryHeader.getCell(2).setCellStyle(header);
            for (Map.Entry<String, BigDecimal> entry : report.categoryTotals().entrySet()) {
                XSSFRow row = summary.createRow(rowNumber++);
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(entry.getValue().doubleValue());
                row.createCell(2).setCellValue(
                        report.categoryPercentages().get(entry.getKey()).doubleValue());
            }

            XSSFRow detailHeader = details.createRow(0);
            detailHeader.createCell(0).setCellValue("Date");
            detailHeader.createCell(1).setCellValue("Category");
            detailHeader.createCell(2).setCellValue("Description");
            detailHeader.createCell(3).setCellValue("Amount");
            for (int i = 0; i < 4; i++) detailHeader.getCell(i).setCellStyle(header);

            int detailRow = 1;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (Expense expense : report.expenses()) {
                XSSFRow row = details.createRow(detailRow++);
                row.createCell(0).setCellValue(expense.getExpenseDate().format(formatter));
                row.createCell(1).setCellValue(expense.getCategory());
                row.createCell(2).setCellValue(expense.getDescription());
                row.createCell(3).setCellValue(expense.getAmount().doubleValue());
            }

            summary.autoSizeColumn(0);
            summary.autoSizeColumn(1);
            summary.autoSizeColumn(2);
            details.autoSizeColumn(0);
            details.autoSizeColumn(1);
            details.autoSizeColumn(2);
            details.autoSizeColumn(3);
            workbook.write(output);
        }
    }
}
