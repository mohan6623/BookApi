package com.marvel.springsecurity.controller.userController;

import com.marvel.springsecurity.dto.EmailDto;
import com.marvel.springsecurity.dto.JwtResponse;
import com.marvel.springsecurity.dto.PasswordResetDto;
import com.marvel.springsecurity.dto.UserDto;
import com.marvel.springsecurity.model.Users;
import com.marvel.springsecurity.service.security.EmailService;
import com.marvel.springsecurity.service.security.JwtService;
import com.marvel.springsecurity.service.security.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class EmailController {

    private final EmailService emailService;
    private final JwtService jwtService;
    private final UserService userService;

    public EmailController(EmailService emailService, JwtService jwtService, UserService userService) {
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    // Rate limit: 1 request per 5 minutes per email
    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendEmailVerificationLink(@RequestBody EmailDto user){
        Users existingUser = userService.findByEmail(user.getEmail());
        if(existingUser == null) {
            return ResponseEntity.badRequest().body("User not register");
        }
        if(existingUser.isEmailVerified()){
            return ResponseEntity.badRequest().body("User already verified");
        }
        String token = jwtService.generateEmailToken(user.getEmail());
        existingUser.setVerificationToken(token);
        userService.save(existingUser);
        emailService.sendVerificationEmail(user.getEmail(), token);
        return ResponseEntity.ok("Email verification link sent again.");
    }

    // sends reset password mail
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody UserDto userDto){
        String email = userDto.getEmail();
        Users existingUser = userService.findByEmail(email);
        if(existingUser == null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email not register, please register first");
        }
        if(!existingUser.isEmailVerified()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email not verified, please verify email first");
        }

        String token = jwtService.generatePasswordResetToken(email);
        existingUser.setVerificationToken(token);
        userService.save(existingUser);
        emailService.sendForgotPasswordEmail(email, token);
        return ResponseEntity.ok("Password reset link sent to your email");
    }


    //verifies the email
    @GetMapping("/validate/verify-email")
    public ResponseEntity<JwtResponse> verifyEmail(@RequestParam String token){
        JwtResponse response = emailService.validateEmailVerification(token);
        if(response != null) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    //check for token validation alone
    @GetMapping("/validate/forgot-password")
    public ResponseEntity<Void> validateForgotPasswordToken(@RequestParam String token){
        boolean isValid = emailService.validateForgotPasswordToken(token);
        if(!isValid) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok().build();
    }

    //updates the password
    @PutMapping("/validate/forgot-password")
    public ResponseEntity<Void> updatePassword(@RequestBody PasswordResetDto passwordResetDto){
        boolean isValid = emailService.updatePassword(passwordResetDto);
        if(!isValid) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok().build();
    }

}
