package com.example.personal_finance_tracker.app.routes;

import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.models.dto.*;
import com.example.personal_finance_tracker.app.repository.UserRepo;
import com.example.personal_finance_tracker.app.services.FinanceEntryService;
import com.example.personal_finance_tracker.app.services.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserRepo userRepo;
    private final FinanceEntryService financeEntryService;

    public UserController(UserService userService, UserRepo userRepo, FinanceEntryService financeEntryService) {
        this.userService = userService;
        this.userRepo = userRepo;
        this.financeEntryService = financeEntryService;
    }

    @PutMapping("/{id}/details")
    public ResponseEntity<UserDetailsResponse> getUserDetails(@PathVariable Long id) {
        log.info("Fetching user details for ID: {}", id);
        try {
            User user = userRepo.findById(id)
                    .orElseThrow(() -> {
                        log.error("User not found with ID: {}", id);
                        return new RuntimeException("User not found");
                    });

            int totalTransactions = financeEntryService.getTransactionsCount(id);
            log.debug("Found {} transactions for user ID: {}", totalTransactions, id);

            UserDetailsResponse response = new UserDetailsResponse();
            response.setUser(user);
            response.setTotalTransactions(totalTransactions);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching user details for ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<String> updatePassword(
            @PathVariable Long id,
            @RequestBody UpdatePasswordRequest request) {

        log.info("Password update request for user ID: {}", id);
        try {
            boolean updated = userService.updatePassword(
                    id,
                    request.getCurrentPassword(),
                    request.getNewPassword()
            );

            if (updated) {
                log.info("Password updated successfully for user ID: {}", id);
                return ResponseEntity.ok("Password updated successfully");
            } else {
                log.warn("Failed password update attempt for user ID: {}", id);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Current password is incorrect");
            }
        } catch (Exception e) {
            log.error("Error updating password for user ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred during password update");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAccount(
            @PathVariable Long id,
            @RequestBody AccountDeleteRequest accountDeleteRequest) {

        log.info("Account deletion request for user ID: {}", id);
        try {
            boolean deleted = userService.deleteUser(id, accountDeleteRequest.getPassword());

            if (deleted) {
                log.info("Account deleted successfully for user ID: {}", id);
                return ResponseEntity.ok("Account deleted successfully");
            } else {
                log.warn("Failed account deletion attempt for user ID: {}", id);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Password is incorrect");
            }
        } catch (Exception e) {
            log.error("Error deleting account for user ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred during account deletion");
        }
    }

    @PutMapping("/{id}/2fa/disable")
    public ResponseEntity<String> disableTwoFactorAuth(
            @PathVariable Long id,
            @RequestBody DisableGoogleAuthRequest disableGoogleAuthRequest) {

        log.info("2FA disable request for user ID: {}", id);
        try {
            boolean disabled = userService.disableTwoFactorAuth(
                    id,
                    disableGoogleAuthRequest.getPassword()
            );

            if (disabled) {
                log.info("2FA disabled successfully for user ID: {}", id);
                return ResponseEntity.ok("Two-factor authentication disabled successfully");
            } else {
                log.warn("Failed 2FA disable attempt for user ID: {}", id);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Password is incorrect");
            }
        } catch (Exception e) {
            log.error("Error disabling 2FA for user ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while disabling 2FA");
        }
    }

    @PutMapping("/{id}/profileImage")
    public ResponseEntity<?> updateProfileImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        log.info("Profile image update request for user ID: {}", id);
        try {
            if (file.isEmpty()) {
                log.warn("Empty file received for user ID: {}", id);
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Please select an image file"));
            }

            // Convert image to Base64
            byte[] imageBytes = file.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // Update profile image through service
            userService.updateProfileImage(id, base64Image);

            log.info("Successfully updated profile image for user ID: {}", id);
            // Return the new image as part of the response
            return ResponseEntity.ok(new ProfileImageResponse(base64Image));

        } catch (IOException e) {
            log.error("File processing error for user ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error processing image file"));
        } catch (Exception e) {
            log.error("Error updating profile image for user ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error updating profile image"));
        }
    }


}
