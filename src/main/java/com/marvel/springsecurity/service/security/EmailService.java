package com.marvel.springsecurity.service.security;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String email, String token){
        String subject = "Email Verification";
        String path = "/api/register/verify-email";
        String body = "Please click the following link to verify your email: ";
        sendEmail(email, token, subject, path, body);
    }

    private void sendResetPasswordEmail(String email, String token){
        String subject = "Password Reset Request";
        String path = "/api/update/reset-password";
        String body = "Please click the following link to reset your password: ";
        sendEmail(email, token, subject, path, body);

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

            String verificationLink = ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path(path)
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
                    """.formatted(subject, body, verificationLink, verificationLink);

            helper.setTo(email);
            helper.setSubject(subject);
            helper.setFrom(from);
            helper.setText(content, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Failed to send email " + e.getMessage());
        }

    }
}
