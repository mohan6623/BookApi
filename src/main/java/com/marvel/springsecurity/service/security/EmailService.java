package com.marvel.springsecurity.service.security;

import com.marvel.springsecurity.dto.JwtResponse;
import com.marvel.springsecurity.dto.PasswordResetDto;
import com.marvel.springsecurity.model.Users;
import com.marvel.springsecurity.repo.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.UnsupportedEncodingException;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final JwtService jwtService;
    private final UserRepository userRepo;
    private final BCryptPasswordEncoder encoder;
    private UserService userService;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${mail.from.address:${spring.mail.username}}")
    private String fromAddress;

    @Value("${mail.from.name:Book Forum}")
    private String fromName;

    @Value("${app.frontend.url}")
    private String frontend;


    public EmailService(JavaMailSender mailSender, JwtService jwtService, UserRepository userRepo) {
        this.mailSender = mailSender;
        this.jwtService = jwtService;
        this.userRepo = userRepo;
        this.encoder = new BCryptPasswordEncoder(12);
    }

    @Autowired
    public void setUserService(@Lazy UserService userService) {
        this.userService = userService;
    }

    public void sendVerificationEmail(String email, String token) {
        String subject = "Email Verification";
        String path = frontend + "/verify-email";
        String body = "Please click the following link to verify your email: ";
        sendEmail(email, token, subject, path, body);
    }

    public void sendForgotPasswordEmail(String email, String token) {
        String subject = "Password Reset Request";
        String path = frontend + "/forgot-password";
        String body = "Please click the following link to reset your password: ";
        sendEmail(email, token, subject, path, body);

    }

    public JwtResponse validateEmailVerification(String token) {
        log.debug("Validating email verification token");
        String email = jwtService.extractEmail(token);
        if (email == null) {
            log.warn("Failed to extract email from token - token may be expired or invalid");
            return null;
        }
        log.debug("Extracted email: {}", email);
        Users existingUser = userService.findByEmail(email);
        if (existingUser == null) {
            log.warn("User not found for email: {}", email);
            return null;
        }
        if (existingUser.getVerificationToken() == null) {
            log.warn("User has no verification token stored");
            return null;
        }
        if (!token.equals(existingUser.getVerificationToken())) {
            log.warn("Token mismatch - stored token differs from provided token");
            return null;
        }
        log.info("Email verification successful for: {}", email);
        return userService.saveVerifiedUser(existingUser);
    }

    // Reset password using email(forgot-password)
    public boolean updatePassword(PasswordResetDto resetDto) {
        String token = resetDto.getToken();
        String email = jwtService.extractEmailFromResetToken(token);
        if (email == null)
            return false;
        Users existingUser = userService.findByEmail(email);
        if (existingUser == null || !token.equals(existingUser.getVerificationToken())) {
            return false;
        }
        existingUser.setPassword(encoder.encode(resetDto.getPassword()));
        existingUser.setVerificationToken(null);
        userRepo.save(existingUser);
        return true;
    }

    // for email password reset validate from email
    public boolean validateForgotPasswordToken(String token) {
        String email = jwtService.extractEmailFromResetToken(token);
        if (email == null)
            return false;
        Users validUser = userService.findByEmail(email);
        return validUser != null && token.equals(validUser.getVerificationToken());

    }
    // TODO: [MEDIUM TERM] Add method to send verification link expiry notification
    // public void sendVerificationExpiredEmail(String email)
    // Inform user that their verification link expired and provide resend option

    // TODO: [LONG TERM] Add method to send magic link login email
    // public void sendMagicLinkEmail(String email, String token)
    // Send one-time login link for passwordless authentication

    // TODO: [LONG TERM] Add method to send email change verification
    // public void sendEmailChangeVerification(String newEmail, String token)
    // Verify new email address before updating in database

    // TODO: [LONG TERM] Add method to send 2FA setup email
    // public void send2FASetupEmail(String email, String qrCodeUrl)
    // Send QR code and backup codes for 2FA setup

    // TODO: [LONG TERM] Add method to send 2FA OTP email
    // public void send2FACodeEmail(String email, String otp)
    // Send one-time password for 2FA verification

    private void sendEmail(String email, String token, String subject, String path, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            String verificationLink = UriComponentsBuilder
                    .fromUriString(path)
                    .queryParam("token", token)
                    .toUriString();

            String content = """
                        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border-radius: 8px; background-color: #f9f9f9; text-align: center;">
                            <h2 style="color: #333;">%s</h2>
                            <p style="font-size: 16px; color: #555;">%s</p>
                            <a href="%s" style="display: inline-block; margin: 20px 0; padding: 10px 20px; font-size: 16px; color: #fff; background-color: #007bff; text-decoration: none; border-radius: 5px;">Proceed</a>
                            <p style="font-size: 14px; color: #777;">Or copy and paste this link into your browser:</p>
                            <p style="font-size: 14px; color: #007bff;">%s</p>
                            <p style="font-size: 12px; color: #aaa;">This is an automated message. Please do not reply.</p>
                        </div>
                    """
                    .formatted(subject, body, verificationLink, verificationLink);

            helper.setTo(email);
            helper.setSubject(subject);
            helper.setFrom(fromAddress, fromName);
            helper.setReplyTo(fromAddress); 
            helper.setText(content, true);
            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.warn("Failed to send email {}", e.getMessage());
        }
        // TODO: handle email failer
        // Log to proper logging system
        // Return failure status to caller
        // Add retry logic for transient failures
    }

}
