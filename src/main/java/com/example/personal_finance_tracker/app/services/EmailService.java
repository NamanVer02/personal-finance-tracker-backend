package com.example.personal_finance_tracker.app.services;

import com.example.personal_finance_tracker.app.exceptions.RateLimitExceededException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final Cache<String, Integer> emailRateLimitCache;
    
    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;
    
    // Rate limit configuration
    private static final int MAX_EMAILS_PER_USER = 5;
    private static final int RATE_LIMIT_WINDOW_HOURS = 24;
    
    // Email stats tracking
    private final AtomicInteger totalEmailsSent = new AtomicInteger(0);
    private final AtomicInteger totalEmailsBlocked = new AtomicInteger(0);
    private final Map<String, Integer> emailsPerRecipient = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> emailLogs = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_LOG_SIZE = 100;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        
        // Initialize rate limiting cache
        this.emailRateLimitCache = Caffeine.newBuilder()
                .expireAfterWrite(RATE_LIMIT_WINDOW_HOURS, TimeUnit.HOURS)
                .build();
    }

    /**
     * Sends an email with rate limiting
     */
    public void sendEmail(String to, String subject, String htmlContent) 
            throws MessagingException, RateLimitExceededException {
        
        // Check rate limit
        if (!checkAndUpdateRateLimit(to)) {
            log.warn("Rate limit exceeded for email: {}", to);
            
            // Track blocked email
            totalEmailsBlocked.incrementAndGet();
            
            // Log the blocked attempt
            addEmailLog(to, subject, "BLOCKED", "Rate limit exceeded");
            
            throw new RateLimitExceededException("Email rate limit exceeded. Please try again later.");
        }
        
        if (!emailEnabled) {
            // Email sending is disabled, just log the content
            log.info("Email sending is disabled. Would have sent email to: {}", to);
            log.info("Subject: {}", subject);
            log.debug("Content: {}", htmlContent);
            
            // Track the email as if it was sent (for testing purposes)
            totalEmailsSent.incrementAndGet();
            
            // Add to recipient counter
            emailsPerRecipient.merge(to, 1, Integer::sum);
            
            // Log the mock send
            addEmailLog(to, subject, "MOCK_SENT", "Email sending disabled");
            
            return;
        }
        
        int maxRetries = 3;
        int retryCount = 0;
        MessagingException lastException = null;
        
        while (retryCount < maxRetries) {
            try {
                log.info("Sending email to: {} (attempt {})", to, retryCount + 1);
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlContent, true);
                
                mailSender.send(message);
                log.info("Email sent successfully to: {}", to);
                
                // Track successful email
                totalEmailsSent.incrementAndGet();
                
                // Add to recipient counter
                emailsPerRecipient.merge(to, 1, Integer::sum);
                
                // Log the successful send
                addEmailLog(to, subject, "SENT", "Success");
                
                return; // Success, exit method
            } catch (MessagingException e) {
                lastException = e;
                log.warn("Failed to send email to: {} (attempt {}). Reason: {}", 
                        to, retryCount + 1, e.getMessage());
                retryCount++;
                
                if (retryCount < maxRetries) {
                    try {
                        // Wait before retrying (exponential backoff)
                        Thread.sleep(1000 * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        // If we get here, all retries failed
        log.error("Failed to send email to: {} after {} attempts", to, maxRetries);
        
        // Log the failed send
        addEmailLog(to, subject, "FAILED", lastException != null ? lastException.getMessage() : "Unknown error");
        
        if (lastException != null) {
            throw lastException;
        }
    }
    
    /**
     * Adds an entry to the email log with timestamp
     */
    private void addEmailLog(String to, String subject, String status, String message) {
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        logEntry.put("recipient", to);
        logEntry.put("subject", subject);
        logEntry.put("status", status);
        logEntry.put("message", message);
        
        emailLogs.add(0, logEntry); // Add to beginning to get reverse chronological order
        
        // Maintain max size
        while (emailLogs.size() > MAX_LOG_SIZE) {
            emailLogs.remove(emailLogs.size() - 1);
        }
    }
    
    /**
     * Checks if user has exceeded email rate limit and updates counter
     */
    private boolean checkAndUpdateRateLimit(String emailAddress) {
        Integer currentCount = emailRateLimitCache.getIfPresent(emailAddress);
        
        if (currentCount == null) {
            emailRateLimitCache.put(emailAddress, 1);
            return true;
        } else if (currentCount < MAX_EMAILS_PER_USER) {
            emailRateLimitCache.put(emailAddress, currentCount + 1);
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Sends 2FA setup instructions to a user
     */
    public void send2FASetupEmail(String to, String username, String secret, String qrCodeBase64) 
            throws MessagingException, RateLimitExceededException {
        
        String subject = "Your Two-Factor Authentication Setup";
        String content = """
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 5px;">
                    <h2 style="color: #5b21b6; text-align: center;">Two-Factor Authentication Setup</h2>
                    <p>Hello %s,</p>
                    <p>Thank you for setting up two-factor authentication for your Personal Finance Tracker account. This adds an important layer of security to protect your financial data.</p>
                    
                    <h3 style="color: #5b21b6; margin-top: 20px;">Setup Instructions:</h3>
                    <ol>
                        <li><strong>Download Google Authenticator</strong> from the App Store (iOS) or Play Store (Android)</li>
                        <li><strong>Open the app</strong> and tap the "+" icon</li>
                        <li><strong>Choose "Scan a QR code"</strong> or "Enter a setup key"</li>
                    </ol>
                    
                    <div style="background-color: #f4f4f9; padding: 15px; border-radius: 5px; margin: 20px 0;">
                        <p style="margin: 0; font-weight: bold;">Your secret key:</p>
                        <p style="font-family: monospace; word-break: break-all; margin: 10px 0;">%s</p>
                        <p style="margin: 10px 0 0; font-size: 0.9em; color: #666;">If you can't scan the QR code, you can manually enter this key in your authenticator app.</p>
                    </div>
                    
                    <p>The next time you log in, you'll need to provide the 6-digit code from the Google Authenticator app along with your password.</p>
                    
                    <p style="margin-top: 30px; font-size: 0.9em; color: #666;">If you lose access to your authenticator app, please contact support for assistance.</p>
                    
                    <p>Thank you,<br>Personal Finance Tracker Team</p>
                </div>
            </body>
            </html>
        """.formatted(username, secret);
        
        sendEmail(to, subject, content);
    }
    
    /**
     * Get email rate limit statistics
     */
    public Map<String, Object> getEmailStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalEmailsSent", totalEmailsSent.get());
        stats.put("totalEmailsBlocked", totalEmailsBlocked.get());
        stats.put("rateLimitPerUser", MAX_EMAILS_PER_USER);
        stats.put("rateLimitWindowHours", RATE_LIMIT_WINDOW_HOURS);
        
        // Get current rate limit usage for all users
        Map<String, Object> userRateLimits = new HashMap<>();
        emailRateLimitCache.asMap().forEach((email, count) -> {
            Map<String, Object> userStats = new HashMap<>();
            userStats.put("count", count);
            userStats.put("remaining", MAX_EMAILS_PER_USER - count);
            userStats.put("percentUsed", (count * 100.0 / MAX_EMAILS_PER_USER));
            userRateLimits.put(email, userStats);
        });
        stats.put("userRateLimits", userRateLimits);
        
        // Top email recipients
        Map<String, Integer> topRecipients = new HashMap<>();
        emailsPerRecipient.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> topRecipients.put(entry.getKey(), entry.getValue()));
        stats.put("topRecipients", topRecipients);
        
        // Recent email logs
        stats.put("recentEmails", emailLogs);
        
        return stats;
    }
    
    /**
     * Reset email statistics (for testing)
     */
    public void resetEmailStats() {
        totalEmailsSent.set(0);
        totalEmailsBlocked.set(0);
        emailsPerRecipient.clear();
        emailLogs.clear();
        emailRateLimitCache.invalidateAll();
    }
} 