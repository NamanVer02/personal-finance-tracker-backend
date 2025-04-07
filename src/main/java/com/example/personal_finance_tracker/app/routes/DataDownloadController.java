package com.example.personal_finance_tracker.app.routes;

import com.example.personal_finance_tracker.app.models.FinanceEntry;
import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.services.FinanceEntryService;
import com.example.personal_finance_tracker.app.services.UserService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
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
public class DataDownloadController {

    @Autowired
    private FinanceEntryService financeEntryService;

    @Autowired
    private UserService userService;

    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    // Existing CSV download endpoints
    @PostMapping("/download/{userId}/csv")
    public ResponseEntity<byte[]> downloadCsv(@PathVariable Long userId) {
        List<FinanceEntry> transactions = financeEntryService.findByUserId(userId);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = getPrintWriter(outputStream, transactions);
        writer.close();

        byte[] csvBytes = outputStream.toByteArray();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "Transactions.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes);
    }

    @PostMapping("/download/admin/csv")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<byte[]> adminDownloadCsv() {
        List<FinanceEntry> transactions = financeEntryService.findAll();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = getPrintWriter(outputStream, transactions);
        writer.close();

        byte[] csvBytes = outputStream.toByteArray();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "Transactions.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes);
    }

    private PrintWriter getPrintWriter(ByteArrayOutputStream outputStream, List<FinanceEntry> transactions) {
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
        return writer;
    }

    // New PDF download endpoints
    @PostMapping("/download/{userId}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long userId) {
        List<FinanceEntry> transactions = financeEntryService.findByUserId(userId);
        byte[] pdfBytes = generatePdfBytes(transactions);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Transactions.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    @PostMapping("/download/admin/pdf")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<byte[]> adminDownloadPdf() {
        List<FinanceEntry> transactions = financeEntryService.findAll();
        byte[] pdfBytes = generatePdfBytes(transactions);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Transactions.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    private byte[] generatePdfBytes(List<FinanceEntry> transactions) {
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

            // Add date generated
            Font dateFont = new Font(Font.FontFamily.HELVETICA, 12, Font.ITALIC);
            Paragraph dateGenerated = new Paragraph("Generated on: " +
                    dateFormatter.format(new java.util.Date()), dateFont);
            dateGenerated.setAlignment(Element.ALIGN_RIGHT);
            dateGenerated.setSpacingAfter(20);
            document.add(dateGenerated);

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

            // Add data rows
            Font dataFont = new Font(Font.FontFamily.HELVETICA, 10);
            for (FinanceEntry transaction : transactions) {
                String username = userService.getUsernameByUserId(transaction.getUserId());

                // Add cells
                table.addCell(new Phrase(String.valueOf(transaction.getId()), dataFont));
                table.addCell(new Phrase(transaction.getLabel(), dataFont));
                table.addCell(new Phrase(String.valueOf(transaction.getAmount()), dataFont));
                table.addCell(new Phrase(transaction.getType(), dataFont));
                table.addCell(new Phrase(transaction.getCategory(), dataFont));
                table.addCell(new Phrase(dateFormatter.format(transaction.getDate()), dataFont));
                table.addCell(new Phrase(username, dataFont));
            }

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

            document.close();
            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    @PostMapping("/download/accountant/summary/csv")
    @PreAuthorize("hasRole('ROLE_ACCOUNTANT') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<byte[]> accountantDownloadSummaryCsv() {
        // This will generate a summary report without exposing detailed transactions
        List<User> users = userService.getAllUsers();

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

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvBytes);
    }

    @PostMapping("/download/accountant/summary/pdf")
    @PreAuthorize("hasRole('ROLE_ACCOUNTANT') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<byte[]> accountantDownloadSummaryPdf() {
        // Generate a PDF summary report
        List<User> users = userService.getAllUsers();
        byte[] pdfBytes = generateAccountantSummaryPdf(users);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "FinancialSummary.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    private byte[] generateAccountantSummaryPdf(List<User> users) {
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

            document.add(table);
            document.close();

            return outputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}