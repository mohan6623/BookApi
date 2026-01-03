package com.marvel.springsecurity.controller.userController;


import com.marvel.springsecurity.dto.EmailDto;
import com.marvel.springsecurity.dto.JwtResponse;
import com.marvel.springsecurity.dto.UserDto;
import com.marvel.springsecurity.model.Users;
import com.marvel.springsecurity.service.security.EmailService;
import com.marvel.springsecurity.service.security.JwtService;
import com.marvel.springsecurity.service.security.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;
    private final EmailService emailService;
    private final JwtService jwtService;

    public UserController(UserService userService, EmailService emailService, JwtService jwtService) {
        this.userService = userService;
        this.emailService = emailService;
        this.jwtService = jwtService;
    }

    //JWT

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Users user){

        Users existingUser = userService.findByEmail(user.getEmail());
        String token = jwtService.generateEmailToken(user.getEmail());
        if(existingUser != null){
            if(existingUser.isEmailVerified()){
                log.info("Email is already registered please login.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Email is already registered please login.");
            }else{
                existingUser.setVerificationToken(token);
                userService.saveUser(existingUser);
                emailService.sendVerificationEmail(user.getEmail(), token);
                log.warn("Email is already registered but not verified. Please check your email for verification link.");
                return ResponseEntity.status(HttpStatus.OK).body("Email is already registered but not verified. Please check your email for verification link.");
            }
        }
        user.setVerificationToken(token);
        userService.saveUser(user);
        emailService.sendVerificationEmail(user.getEmail(), token);
        log.info("User registered successfully. Please check your email for verification link.");
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully. Please check your email for verification link.");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Users user){
        JwtResponse jwt = userService.login(user);
                                                                 //403
        if (jwt == null) {
            log.warn("jwt null");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Email not verified");
        }
        return ResponseEntity.accepted().body(jwt);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    @PutMapping("/user/{user_id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable("user_id") int id,
                                              @RequestPart Users user,
                                              @RequestPart(required = false) MultipartFile imageFile){
        try {
            UserDto updated = userService.updateUser(id, user, imageFile);
            return updated != null                    //202
                    ? ResponseEntity.status(HttpStatus.ACCEPTED).body(updated)
                    : ResponseEntity.notFound().build();
        } catch (IOException e) {
            //500
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }






    // TODO: [LONG TERM] Implement magic link login (passwordless)
    // Endpoint: POST /api/auth/magic-link
    // Body: { "email": "user@example.com" }
    // Logic: Generate one-time token, send email with login link, auto-login on click

    // TODO: [LONG TERM] Add email change verification
    // Endpoint: POST /api/user/change-email
    // Body: { "newEmail": "newemail@example.com" }
    // Logic: Send verification to new email, verify before updating in database


    // TODO: [LONG TERM] Implement 2FA after email verification
    // Methods: TOTP (Google Authenticator), SMS, Email OTP
    // Endpoints: POST /api/auth/2fa/enable, POST /api/auth/2fa/verify


    @GetMapping("/available/username")
    public ResponseEntity<Void> usernameAvailable(@RequestParam String username){
        if(username == null || username.isBlank()) return ResponseEntity.badRequest().build();
        boolean check = userService.usernameAvailable(username);
        if(check) return ResponseEntity.ok().build();
                                                    //409
        else return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @GetMapping("/available/mail")
    public ResponseEntity<Void> mailAvailable(@RequestParam String mail){
        boolean check = userService.emailAvailable(mail);
                                                                                    //409
        return check ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }



    // TODO: [MEDIUM TERM] Add verification attempt counter (track failed attempts per user)
    // Recommendation: Lock verification after 3 failed attempts, require new token
    // TODO: [MEDIUM TERM] Implement email verification link expiry message
    // Return user-friendly message when token is expired instead of generic 403
    // TODO: [MEDIUM TERM] Log security events (failed verifications) for audit trail


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



//    @DeleteMapping("user/{user_id}")
//    public ResponseEntity<Void> deleteUser(@PathVariable("user_id") int id){
//        service.deleteUser(id);
//    }
}
