package com.example.personal_finance_tracker.app.routes;

import com.example.personal_finance_tracker.app.models.FinanceEntry;
import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.services.FinanceEntryService;
import com.example.personal_finance_tracker.app.services.UserService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
public class DataDownloadController {

    @Autowired
    private FinanceEntryService financeEntryService;

    @Autowired
    private UserService userService;

    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    // Existing CSV download endpoints
    @PostMapping("/download/{userId}/csv")
    public ResponseEntity<byte[]> downloadCsv(@PathVariable Long userId) {
        log.info("Entering downloadCsv method for userId: {}", userId);
        List<FinanceEntry> transactions = financeEntryService.findByUserId(userId);
        log.debug("Transactions found: {}", transactions);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = getPrintWriter(outputStream, transactions);
        writer.close();

        byte[] csvBytes = outputStream.toByteArray();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "Transactions.csv");

        log.info("Exiting downloadCsv method for userId: {}", userId);
        return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes);
    }

    @PostMapping("/download/admin/csv")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<byte[]> adminDownloadCsv() {
        log.info("Entering adminDownloadCsv method");
        List<FinanceEntry> transactions = financeEntryService.findAll();
        log.debug("All transactions found: {}", transactions);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = getPrintWriter(outputStream, transactions);
        writer.close();

        byte[] csvBytes = outputStream.toByteArray();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "Transactions.csv");

        log.info("Exiting adminDownloadCsv method");
        return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes);
    }

    private PrintWriter getPrintWriter(ByteArrayOutputStream outputStream, List<FinanceEntry> transactions) {
        log.info("Entering getPrintWriter method with transactions list size: {}", transactions.size());
        PrintWriter writer = new PrintWriter(outputStream);

        // Write CSV header
        writer.println("ID,Label,Amount,Type,Category,Date,Username");

        // Write CSV data
        for (FinanceEntry transaction : transactions) {
            String username = userService.getUsernameByUserId(transaction.getUserId());
            writer.println(
                    transaction.getId() + "," +
                            transaction.getLabel() + "," +
                            transaction.getAmount() + "," +
                            transaction.getType() + "," +
                            transaction.getCategory() + "," +
                            transaction.getDate() + "," +
                            username
            );
        }

        writer.flush();
        log.info("Exiting getPrintWriter method");
        return writer;
    }

    // New PDF download endpoints
    @PostMapping("/download/{userId}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long userId) {
        log.info("Entering downloadPdf method for userId: {}", userId);
        List<FinanceEntry> transactions = financeEntryService.findByUserId(userId);
        log.debug("Transactions found for PDF: {}", transactions);
        byte[] pdfBytes = generatePdfBytes(transactions);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Transactions.pdf");

        log.info("Exiting downloadPdf method for userId: {}", userId);
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    @PostMapping("/download/admin/pdf")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<byte[]> adminDownloadPdf() {
        log.info("Entering adminDownloadPdf method");
        List<FinanceEntry> transactions = financeEntryService.findAll();
        log.debug("All transactions found for PDF: {}", transactions);
        byte[] pdfBytes = generatePdfBytes(transactions);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Transactions.pdf");

        log.info("Exiting adminDownloadPdf method");
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    private byte[] generatePdfBytes(List<FinanceEntry> transactions) {
        log.info("Entering generatePdfBytes method with transactions list size: {}", transactions.size());
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            document.open();

            // Add title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("Financial Transactions", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            log.debug("Added title to PDF document");

            // Add date generated
            Font dateFont = new Font(Font.FontFamily.HELVETICA, 12, Font.ITALIC);
            Paragraph dateGenerated = new Paragraph("Generated on: " +
                    dateFormatter.format(new java.util.Date()), dateFont);
            dateGenerated.setAlignment(Element.ALIGN_RIGHT);
            dateGenerated.setSpacingAfter(20);
            document.add(dateGenerated);
            log.debug("Added date generated to PDF document");

            // Create table
            PdfPTable table = new PdfPTable(7); // 7 columns
            table.setWidthPercentage(100);

            // Set column widths
            float[] columnWidths = {0.5f, 2f, 1f, 1f, 1.5f, 1.5f, 1.5f};
            table.setWidths(columnWidths);

            // Add table headers
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            String[] headers = {"ID", "Label", "Amount", "Type", "Category", "Date", "Username"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                cell.setPadding(5);
                table.addCell(cell);
            }
            log.debug("Added table headers to PDF document");

            // Add data rows
            Font dataFont = new Font(Font.FontFamily.HELVETICA, 10);

            for (FinanceEntry transaction : transactions) {
                String username = userService.getUsernameByUserId(transaction.getUserId());
                java.time.LocalDate localDate = transaction.getDate();

                // Add cells
                table.addCell(new Phrase(String.valueOf(transaction.getId()), dataFont));
                table.addCell(new Phrase(transaction.getLabel(), dataFont));
                table.addCell(new Phrase(String.valueOf(transaction.getAmount()), dataFont));
                table.addCell(new Phrase(transaction.getType(), dataFont));
                table.addCell(new Phrase(transaction.getCategory(), dataFont));

                if (localDate != null) {
                    // Convert LocalDate to java.util.Date
                    java.util.Date date = java.sql.Date.valueOf(localDate);  // or use Instant/ZonedDateTime if needed
                    table.addCell(new Phrase(dateFormatter.format(date), dataFont));
                } else {
                    table.addCell(new Phrase("N/A", dataFont));
                }

                table.addCell(new Phrase(username, dataFont));
            }
            log.debug("Added data rows to PDF document");

            document.add(table);

            // Add footer with page numbers
            int pageCount = writer.getPageNumber();
            for (int i = 1; i <= pageCount; i++) {
                PdfContentByte canvas = writer.getDirectContent();
                canvas.beginText();
                canvas.setFontAndSize(BaseFont.createFont(), 10);
                canvas.moveText(document.right() - 80, document.bottom() - 20);
                canvas.showText("Page " + i + " of " + pageCount);
                canvas.endText();
            }
            log.debug("Added footer with page numbers to PDF document");

            document.close();
            log.info("Exiting generatePdfBytes method - PDF generated successfully");
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    @PostMapping("/download/accountant/summary/csv")
    @PreAuthorize("hasRole('ROLE_ACCOUNTANT') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<byte[]> accountantDownloadSummaryCsv() {
        log.info("Entering accountantDownloadSummaryCsv method");
        // This will generate a summary report without exposing detailed transactions
        List<User> users = userService.getAllUsers();
        log.debug("All users found: {}", users);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);

        // Write CSV headers
        writer.println("User ID,Username,Total Income,Total Expense,Net Amount");

        // Write data for each user
        for (User user : users) {
            double totalIncome = financeEntryService.getTotalIncomeForUser(user.getId());
            double totalExpense = financeEntryService.getTotalExpenseForUser(user.getId());
            double netAmount = totalIncome - totalExpense;

            writer.println(user.getId() + "," + user.getUsername() + "," +
                    totalIncome + "," + totalExpense + "," + netAmount);
        }

        writer.close();

        byte[] csvBytes = outputStream.toByteArray();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "FinancialSummary.csv");

        log.info("Exiting accountantDownloadSummaryCsv method");
        return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes);
    }

    @PostMapping("/download/accountant/summary/pdf")
    @PreAuthorize("hasRole('ROLE_ACCOUNTANT') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<byte[]> accountantDownloadSummaryPdf() {
        log.info("Entering accountantDownloadSummaryPdf method");
        // Generate a PDF summary report
        List<User> users = userService.getAllUsers();
        log.debug("All users found: {}", users);
        byte[] pdfBytes = generateAccountantSummaryPdf(users);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "FinancialSummary.pdf");

        log.info("Exiting accountantDownloadSummaryPdf method");
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    private byte[] generateAccountantSummaryPdf(List<User> users) {
        log.info("Entering generateAccountantSummaryPdf method with users list size: {}", users.size());
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // Add title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("Financial Summary Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" ")); // Empty line
            log.debug("Added title to PDF document");

            // Create summary table
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);

            // Add table headers
            String[] headers = {"User ID", "Username", "Total Income", "Total Expense", "Net Amount"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD)));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
            }
            log.debug("Added table headers to PDF document");

            // Add data rows
            for (User user : users) {
                double totalIncome = financeEntryService.getTotalIncomeForUser(user.getId());
                double totalExpense = financeEntryService.getTotalExpenseForUser(user.getId());
                double netAmount = totalIncome - totalExpense;

                table.addCell(String.valueOf(user.getId()));
                table.addCell(user.getUsername());
                table.addCell(String.format("$%.2f", totalIncome));
                table.addCell(String.format("$%.2f", totalExpense));
                table.addCell(String.format("$%.2f", netAmount));
            }
            log.debug("Added data rows to PDF document");

            document.add(table);
            document.close();
            log.info("Exiting generateAccountantSummaryPdf method - PDF generated successfully");

            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error generating accountant summary PDF: {}", e.getMessage(), e);
            e.printStackTrace();
            return new byte[0];
        }
    }
}
