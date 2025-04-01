package com.example.personal_finance_tracker.app.routes;

import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.services.FinanceEntryService;
import com.example.personal_finance_tracker.app.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accountant")
@RequiredArgsConstructor
public class AccountantController {

    private final FinanceEntryService financeEntryService;
    private final UserService userService;

    @PostMapping("/users")
    @PreAuthorize("hasRole('ROLE_ACCOUNTANT') or hasRole('ROLE_ADMIN')")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @PostMapping("/summary/overall")
    @PreAuthorize("hasRole('ROLE_ACCOUNTANT') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getOverallSummary() {
        // Get summary data for all users combined
        double totalIncome = financeEntryService.getTotalIncomeForAllUsers();
        double totalExpense = financeEntryService.getTotalExpenseForAllUsers();
        double netAmount = totalIncome - totalExpense;

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalIncome", totalIncome);
        summary.put("totalExpense", totalExpense);
        summary.put("netAmount", netAmount);

        return ResponseEntity.ok(summary);
    }

    @PostMapping("/summary/user/{userId}")
    @PreAuthorize("hasRole('ROLE_ACCOUNTANT') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserSummary(@PathVariable Long userId) {
        // Get summary data for a specific user
        double totalIncome = financeEntryService.getTotalIncomeForUser(userId);
        double totalExpense = financeEntryService.getTotalExpenseForUser(userId);
        double netAmount = totalIncome - totalExpense;

        Map<String, Object> summary = new HashMap<>();
        summary.put("userId", userId);
        summary.put("totalIncome", totalIncome);
        summary.put("totalExpense", totalExpense);
        summary.put("netAmount", netAmount);

        // Category-wise summary
        Map<String, Double> categoryWiseIncome = financeEntryService.getCategoryWiseIncomeForCurrentYear(userId);
        Map<String, Double> categoryWiseExpense = financeEntryService.getCategoryWiseExpenseForCurrentYear(userId);

        summary.put("categoryWiseIncome", categoryWiseIncome);
        summary.put("categoryWiseExpense", categoryWiseExpense);

        return ResponseEntity.ok(summary);
    }

    @PostMapping("/summary/monthly")
    @PreAuthorize("hasRole('ROLE_ACCOUNTANT') or hasRole('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> getMonthlySummary() {
        // Get monthly financial data for all users
        Map<String, Double> monthlyIncome = financeEntryService.getMonthlyIncomeForAllUsers();
        Map<String, Double> monthlyExpense = financeEntryService.getMonthlyExpenseForAllUsers();

        Map<String, Object> summary = new HashMap<>();
        summary.put("monthlyIncome", monthlyIncome);
        summary.put("monthlyExpense", monthlyExpense);

        return ResponseEntity.ok(summary);
    }
}