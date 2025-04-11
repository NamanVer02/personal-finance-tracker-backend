package com.example.personal_finance_tracker.app.routes;

import com.example.personal_finance_tracker.app.models.FinanceEntry;
import com.example.personal_finance_tracker.app.models.User;
import com.example.personal_finance_tracker.app.repository.FinanceEntryQueryRepository;
import com.example.personal_finance_tracker.app.security.UserDetailsImpl;
import com.example.personal_finance_tracker.app.services.FinanceEntryService;
import com.example.personal_finance_tracker.app.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class FinanceEntryController {
    private final FinanceEntryService financeEntryService;
    private final UserService userService;
    private final FinanceEntryQueryRepository financeEntryRepository;

    @PostMapping("/get")
    public List<FinanceEntry> getAll() {
        log.info("Entering getAll method");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Check if user has ADMIN role
        boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN"));

        if (isAdmin) {
            // Admin can see all entries
            log.info("Admin user detected. Fetching all entries.");
            List<FinanceEntry> entries = financeEntryService.findAll();
            log.info("Exiting getAll method with all entries. Count: {}", entries.size());
            return entries;
        } else {
            // Regular users can only see their own entries
            log.info("Regular user detected. Fetching entries for user ID: {}", userDetails.getId());
            List<FinanceEntry> entries = financeEntryService.findByUserId(userDetails.getId());
            log.info("Exiting getAll method with user specific entries. Count: {}", entries.size());
            return entries;
        }
    }

    @PostMapping("/get/{type}")
    public List<FinanceEntry> getByType(@PathVariable String type) {
        log.info("Entering getByType method with type: {}", type);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Check if user has ADMIN role
        boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN"));

        if (isAdmin) {
            // Admin can see all entries of a specific type
            log.info("Admin user detected. Fetching all entries of type: {}", type);
            List<FinanceEntry> entries = financeEntryService.findByType(type);
            log.info("Exiting getByType method with all entries of type {}. Count: {}", type, entries.size());
            return entries;
        } else {
            // Regular users can only see their own entries of a specific type
            log.info("Regular user detected. Fetching entries of type {} for user ID: {}", type, userDetails.getId());
            List<FinanceEntry> entries = financeEntryService.findByTypeAndUserId(type, userDetails.getId());
            log.info("Exiting getByType method with user specific entries of type {}. Count: {}", type, entries.size());
            return entries;
        }
    }

    @PostMapping("/post")
    public FinanceEntry create(@RequestBody FinanceEntry financeEntry) {
        log.info("Entering create method with financeEntry: {}", financeEntry);
        // Set the current user as the owner of the finance entry
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        financeEntry.setUserId(userDetails.getId());
        log.info("Finance entry associated with user ID: {}", userDetails.getId());
        FinanceEntry createdEntry = financeEntryService.create(financeEntry);
        log.info("Exiting create method with created entry: {}", createdEntry);
        return createdEntry;
    }

    @PutMapping("/put/{id}")
    public FinanceEntry update(@PathVariable Long id, @RequestBody FinanceEntry financeEntry) throws Exception {
        log.info("Entering update method for ID: {} with financeEntry: {}", id, financeEntry);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN"));

        // Get the existing entry to check ownership
        List<FinanceEntry> userEntries = financeEntryService.findByUserId(userDetails.getId());
        boolean isOwner = userEntries.stream().anyMatch(entry -> entry.getId().equals(id));

        // Only allow update if user is admin or the owner of the entry
        if (isAdmin || isOwner) {
            log.info("User is authorized to update entry. isAdmin: {}, isOwner: {}", isAdmin, isOwner);
            // Preserve the original user ID - don't allow changing ownership
            financeEntry.setUserId(isAdmin && !isOwner ?
                    financeEntryService.findAll().stream()
                            .filter(e -> e.getId().equals(id))
                            .findFirst()
                            .map(FinanceEntry::getUserId)
                            .orElse(null) :
                    userDetails.getId());

            FinanceEntry updatedEntry = financeEntryService.update(id, financeEntry);
            log.info("Exiting update method with updated entry: {}", updatedEntry);
            return updatedEntry;
        } else {
            log.warn("User does not have permission to update entry with ID: {}", id);
            throw new Exception("You don't have permission to update this entry");
        }
    }

    @DeleteMapping("delete/{id}")
    public void delete(@PathVariable Long id) {
        log.info("Entering delete method for ID: {}", id);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        boolean isAdmin = authentication.getAuthorities().contains(new SimpleGrantedAuthority("ADMIN"));

        List<FinanceEntry> userEntries = financeEntryService.findByUserId(userDetails.getId());
        boolean isOwner = userEntries.stream().anyMatch(entry -> entry.getId().equals(id));

        if(isAdmin || isOwner) {
            log.info("User is authorized to delete entry. isAdmin: {}, isOwner: {}", isAdmin, isOwner);
            financeEntryService.deleteById(id);
            log.info("Entry with ID {} deleted successfully", id);
        } else {
            log.warn("User does not have permission to delete entry with ID: {}", id);
        }
        financeEntryService.deleteById(id);
    }

    @PostMapping("/get/summary/expense/{userId}")
    public ResponseEntity<Map<String, Double>> getCategoryWiseSpendingForCurrentMonth(@PathVariable Long userId) {
        log.info("Entering getCategoryWiseSpendingForCurrentMonth method for userId: {}", userId);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("User is not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        if (!userDetails.getId().equals(userId)) {
            log.warn("User ID in request does not match authenticated user ID");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Double> categoryWiseSpending = financeEntryService.getCategoryWiseSpending(userId);
        log.info("Exiting getCategoryWiseSpendingForCurrentMonth method with categoryWiseSpending: {}", categoryWiseSpending);
        return ResponseEntity.ok(categoryWiseSpending);
    }

    @PostMapping("/get/summary/income/{userId}")
    public ResponseEntity<Map<String, Double>> getCategoryWiseIncomeForCurrentMonth(@PathVariable Long userId) {
        log.info("Entering getCategoryWiseIncomeForCurrentMonth method for userId: {}", userId);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("User is not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        if (!userDetails.getId().equals(userId)) {
            log.warn("User ID in request does not match authenticated user ID");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Double> categoryWiseIncome = financeEntryService.getCategoryWiseIncome(userId);
        log.info("Exiting getCategoryWiseIncomeForCurrentMonth method with categoryWiseIncome: {}", categoryWiseIncome);
        return ResponseEntity.ok(categoryWiseIncome);
    }

    @PostMapping("/get/admin/transactions")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public List<FinanceEntry> getAllFinanceEntries() {
        log.info("Entering getAllFinanceEntries method (Admin)");
        List<FinanceEntry> entries = financeEntryService.findAll();
        log.info("Exiting getAllFinanceEntries method (Admin) with all entries. Count: {}", entries.size());
        return entries;
    }

    @PostMapping("/get/admin/users")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public List<User> getAllUsers() {
        log.info("Entering getAllUsers method (Admin)");
        List<User> users = userService.getAllUsers();
        log.info("Exiting getAllUsers method (Admin) with all users. Count: {}", users.size());
        return users;
    }

    @GetMapping("/search")
    public ResponseEntity<Page<FinanceEntry>> searchFinanceEntries(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date,desc") String[] sort) {
        log.info("Entering searchFinanceEntries method with parameters: type={}, category={}, minAmount={}, maxAmount={}, startDate={}, endDate={}, searchTerm={}, page={}, size={}, sort={}",
                type, category, minAmount, maxAmount, startDate, endDate, searchTerm, page, size, sort);

        Pageable pageable = PageRequest.of(page, size, Sort.by(createSortOrder(sort)));

        Page<FinanceEntry> entries = financeEntryRepository.findFinanceEntriesWithFilters(
                getCurrentUserId(),
                type,
                category,
                minAmount,
                maxAmount,
                startDate,
                endDate,
                searchTerm,
                pageable
        );

        log.info("Exiting searchFinanceEntries method with page of entries. Count: {}", entries.getTotalElements());
        return ResponseEntity.ok(entries);
    }

    @PostMapping("/admin/search")
    public ResponseEntity<Page<FinanceEntry>> searchFinanceEntriesAdmin(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date,desc") String[] sort) {
        log.info("Entering searchFinanceEntriesAdmin method with parameters: id={}, type={}, category={}, minAmount={}, maxAmount={}, startDate={}, endDate={}, searchTerm={}, page={}, size={}, sort={}",
                id, type, category, minAmount, maxAmount, startDate, endDate, searchTerm, page, size, sort);

        Pageable pageable = PageRequest.of(page, size, Sort.by(createSortOrder(sort)));

        Page<FinanceEntry> entries = (id == null) ? financeEntryRepository.findAllFinanceEntriesWithFiltersAdmin(
                type,
                category,
                minAmount,
                maxAmount,
                startDate,
                endDate,
                searchTerm,
                pageable
        ) : financeEntryRepository.findFinanceEntriesWithFilters(
                id,
                type,
                category,
                minAmount,
                maxAmount,
                startDate,
                endDate,
                searchTerm,
                pageable
        );

        log.info("Exiting searchFinanceEntriesAdmin method with page of entries. Count: {}", entries.getTotalElements());
        return ResponseEntity.ok(entries);
    }

    private Sort.Order[] createSortOrder(String[] sort) {
        List<Sort.Order> orders = new ArrayList<>();

        if (sort[0].contains(",")) {
            // Multiple sort criteria
            for (String sortOrder : sort) {
                String[] parts = sortOrder.split(",");
                orders.add(new Sort.Order(
                        parts.length > 1 && parts[1].equalsIgnoreCase("desc") ?
                                Sort.Direction.DESC : Sort.Direction.ASC,
                        parts[0]
                ));
            }
        } else {
            // Single sort criteria
            orders.add(new Sort.Order(
                    sort.length > 1 && sort[1].equalsIgnoreCase("desc") ?
                            Sort.Direction.DESC : Sort.Direction.ASC,
                    sort[0]
            ));
        }

        return orders.toArray(new Sort.Order[0]);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            return userDetails.getId();
        }
        throw new IllegalStateException("User not authenticated");
    }
}
