package com.example.personal_finance_tracker.app.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class GAService {
    private static final String ISSUER = "Personal Finance";

    public String generateKey() {
        log.info("Generating new TOTP secret key");
        try {
            GoogleAuthenticator gAuth = new GoogleAuthenticator();
            final GoogleAuthenticatorKey key = gAuth.createCredentials();
            return key.getKey();
        } catch (Exception e) {
            log.error("Error generating TOTP secret key", e);
            throw new RuntimeException("Failed to generate TOTP secret key", e);
        }
    }

    public boolean isValid(String secret, int code) {
        log.info("Validating TOTP code for secret");
        try {
            GoogleAuthenticator gAuth = new GoogleAuthenticator(
                    new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder().build()
            );
            boolean isValid = gAuth.authorize(secret, code);
            log.info("TOTP validation result: {}", isValid ? "valid" : "invalid");
            return isValid;
        } catch (Exception e) {
            log.error("Error validating TOTP code", e);
            throw new RuntimeException("Failed to validate TOTP code", e);
        }
    }

    public String generateQRUrl(String secret, String username) {
        log.info("Generating QR URL for username: {}", username);
        try {
            String url = GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(
                    ISSUER,
                    username,
                    new GoogleAuthenticatorKey.Builder(secret).build());
            try {
                return generateQRBase64(url);
            } catch (Exception e) {
                log.error("QR generation failed for {}: {}", username, e.getMessage());
                throw new RuntimeException("Failed to generate QR code", e);
            }
        } catch (Exception e) {
            log.error("Error generating QR URL for username: {}", username, e);
            throw new RuntimeException("Failed to generate QR URL", e);
        }
    }

    public static String generateQRBase64(String qrCodeText) {
        log.info("Generating QR code image");
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hintMap = new HashMap<>();
            hintMap.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeText, BarcodeFormat.QR_CODE, 200, 200, hintMap);
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            log.info("QR code image generated successfully");
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (WriterException e) {
            log.error("QR code writing error: {}", e.getMessage());
            throw new RuntimeException("Failed to write QR code", e);
        } catch (IOException e) {
            log.error("QR image generation IO error: {}", e.getMessage());
            throw new RuntimeException("Failed to generate QR image", e);
        } catch (Exception e) {
            log.error("Unexpected error in QR generation: {}", e.getMessage());
            throw new RuntimeException("Unexpected error in QR generation", e);
        }
    }
}
