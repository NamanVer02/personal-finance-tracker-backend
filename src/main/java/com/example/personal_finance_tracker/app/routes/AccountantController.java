package com.example.personal_finance_tracker.app.routes;

import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.services.FinanceEntryService;
import com.example.personal_finance_tracker.app.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accountant")
@RequiredArgsConstructor
@Slf4j
public class AccountantController {

    private final FinanceEntryService financeEntryService;
    private final UserService userService;

    @PostMapping("/users")
    @PreAuthorize("hasRole('ROLE_ACCOUNTANT') or hasRole('ROLE_ADMIN')")
    public List<User> getAllUsers() {
        log.info("Entering getAllUsers method for accountant");
        List<User> users = userService.getAllUsers();
        log.info("Exiting getAllUsers method with {} users", users.size());
        return users;
    }

    @PostMapping("/summary/overall")
    @PreAuthorize("hasRole('ROLE_ACCOUNTANT') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getOverallSummary() {
        log.info("Entering getOverallSummary method");
        double totalIncome = financeEntryService.getTotalIncomeForAllUsers();
        double totalExpense = financeEntryService.getTotalExpenseForAllUsers();
        double netAmount = totalIncome - totalExpense;

        log.debug("Calculated overall summary - Income: {}, Expense: {}, Net: {}",
                totalIncome, totalExpense, netAmount);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalIncome", totalIncome);
        summary.put("totalExpense", totalExpense);
        summary.put("netAmount", netAmount);

        log.info("Exiting getOverallSummary method");
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/summary/user/{userId}")
    @PreAuthorize("hasRole('ROLE_ACCOUNTANT') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserSummary(@PathVariable Long userId) {
        log.info("Entering getUserSummary method for userId: {}", userId);

        double totalIncome = financeEntryService.getTotalIncomeForUser(userId);
        double totalExpense = financeEntryService.getTotalExpenseForUser(userId);
        double netAmount = totalIncome - totalExpense;

        log.debug("User {} summary - Income: {}, Expense: {}, Net: {}",
                userId, totalIncome, totalExpense, netAmount);

        Map<String, Object> summary = new HashMap<>();
        summary.put("userId", userId);
        summary.put("totalIncome", totalIncome);
        summary.put("totalExpense", totalExpense);
        summary.put("netAmount", netAmount);

        Map<String, Double> categoryWiseIncome = financeEntryService.getCategoryWiseIncomeForCurrentYear(userId);
        Map<String, Double> categoryWiseExpense = financeEntryService.getCategoryWiseExpenseForCurrentYear(userId);

        log.debug("User {} category details - Income Categories: {}, Expense Categories: {}",
                userId, categoryWiseIncome.size(), categoryWiseExpense.size());

        summary.put("categoryWiseIncome", categoryWiseIncome);
        summary.put("categoryWiseExpense", categoryWiseExpense);

        log.info("Exiting getUserSummary method for userId: {}", userId);
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/summary/monthly")
    @PreAuthorize("hasRole('ROLE_ACCOUNTANT') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getMonthlySummary() {
        log.info("Entering getMonthlySummary method");

        Map<String, Double> monthlyIncome = financeEntryService.getMonthlyIncomeForAllUsers();
        Map<String, Double> monthlyExpense = financeEntryService.getMonthlyExpenseForAllUsers();

        log.debug("Monthly summary contains {} income entries and {} expense entries",
                monthlyIncome.size(), monthlyExpense.size());

        Map<String, Object> summary = new HashMap<>();
        summary.put("monthlyIncome", monthlyIncome);
        summary.put("monthlyExpense", monthlyExpense);

        log.info("Exiting getMonthlySummary method");
        return ResponseEntity.ok(summary);
    }
}
