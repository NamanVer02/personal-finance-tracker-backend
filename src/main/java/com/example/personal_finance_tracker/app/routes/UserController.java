package com.example.personal_finance_tracker.app.routes;


import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.models.dto.AccountDeleteRequest;
import com.example.personal_finance_tracker.app.models.dto.DisableGoogleAuthRequest;
import com.example.personal_finance_tracker.app.models.dto.UpdatePasswordRequest;
import com.example.personal_finance_tracker.app.models.dto.UserDetailsResponse;
import com.example.personal_finance_tracker.app.repository.UserRepo;
import com.example.personal_finance_tracker.app.services.FinanceEntryService;
import com.example.personal_finance_tracker.app.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepo userRepo;

    @Autowired
    private FinanceEntryService financeEntryService;

    @PutMapping("/{id}/details")
    public UserDetailsResponse getUserDetails (@PathVariable Long id) {
        UserDetailsResponse userDetailsResponse = new UserDetailsResponse();
        User user = userRepo.findById(id).orElseThrow();

        int totalTransactions = financeEntryService.getTransactionsCount(id);

        userDetailsResponse.setUser(user);
        userDetailsResponse.setTotalTransactions(totalTransactions);
        return userDetailsResponse;
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<String> updatePassword(
            @PathVariable Long id,
            @RequestBody UpdatePasswordRequest request) {

        boolean updated = userService.updatePassword(id, request.getCurrentPassword(), request.getNewPassword());

        if (updated) {
            return ResponseEntity.ok("Password updated successfully");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Current password is incorrect");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAccount(
            @PathVariable Long id,
            @RequestBody AccountDeleteRequest accountDeleteRequest) {

        boolean deleted = userService.deleteUser(id, accountDeleteRequest.getPassword());

        if (deleted) {
            return ResponseEntity.ok("Account deleted successfully");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Password is incorrect");
        }
    }

    @PutMapping("/{id}/2fa/disable")
    public ResponseEntity<String> disableTwoFactorAuth(
            @PathVariable Long id,
            @RequestBody DisableGoogleAuthRequest disableGoogleAuthRequest) {

        boolean disabled = userService.disableTwoFactorAuth(id, disableGoogleAuthRequest.getPassword());

        if (disabled) {
            return ResponseEntity.ok("Two-factor authentication disabled successfully");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Password is incorrect");
        }
    }
}