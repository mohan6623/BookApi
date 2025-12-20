package com.marvel.springsecurity.controller.userController;


import com.marvel.springsecurity.dto.JwtResponse;
import com.marvel.springsecurity.dto.PasswordResetDto;
import com.marvel.springsecurity.dto.UserDto;
import com.marvel.springsecurity.model.Users;
import com.marvel.springsecurity.service.security.EmailService;
import com.marvel.springsecurity.service.security.JwtService;
import com.marvel.springsecurity.service.security.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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


//    @GetMapping("csrf-token")
//    public CsrfToken getCsrf(HttpServletRequest request){
//        return (CsrfToken) request.getAttribute("_csrf");
//    }



//JWT
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Users user){

        Users existingUser = userService.findByEmail(user.getEmail());
        String token = jwtService.generateEmailToken(user.getEmail());
        if(existingUser != null){
            if(existingUser.getEmailVerified()){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email is already registered please login.");
            }else{
                user.setVerificationToken(token);
                userService.saveUser(user);
                emailService.sendVerificationEmail(user.getEmail(), token);
                return ResponseEntity.status(HttpStatus.OK).body("Email is already registered but not verified. Please check your email for verification link.");
            }
        }
        user.setVerificationToken(token);
        userService.saveUser(user);
        emailService.sendVerificationEmail(user.getEmail(), token);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully. Please check your email for verification link.");
    }

    // TODO: [SHORT TERM] Add rate limiting on email verification to prevent brute force attacks
    // Recommendation: Limit to 5 verification attempts per IP per hour
    // TODO: [MEDIUM TERM] Add verification attempt counter (track failed attempts per user)
    // Recommendation: Lock verification after 3 failed attempts, require new token
    // TODO: [MEDIUM TERM] Implement email verification link expiry message
    // Return user-friendly message when token is expired instead of generic 403
    // TODO: [MEDIUM TERM] Log security events (failed verifications) for audit trail
    @GetMapping("register/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token){
        String email = jwtService.extractEmail(token);
        Users existingUser = userService.findByEmail(email);
        if(existingUser == null || existingUser.getVerificationToken() == null){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification link invalid or already used");
        }
        if(!jwtService.validateToken(token) || !existingUser.getVerificationToken().equals(token)){
            // TODO: [MEDIUM TERM] Log failed verification attempt with email, IP, and timestamp
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification link expired. Please request a new one.");
        }
        JwtResponse jwtResponse = userService.saveVerifiedUser(existingUser);
        return ResponseEntity.status(HttpStatus.OK).body(jwtResponse);

    }

    // TODO: Rate limit: 1 request per 5 minutes per email
    @PostMapping("/api/register/resend-verification")
    public ResponseEntity<String> resendEmailVerificationLink(@RequestBody UserDto user){
        Users existingUser = userService.findByEmail(user.getEmail());
        if(existingUser == null || !existingUser.getEmailVerified()) {
            return ResponseEntity.badRequest().body("Email is already registered and verified please login.");
        }

        String token = jwtService.generateEmailToken(user.getEmail());
        emailService.sendVerificationEmail(user.getEmail(), token);
        return ResponseEntity.ok("Email verification link sent again.");
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
        boolean check = userService.mailAvailable(mail);
                                                                                    //409
        return check ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@RequestBody Users user){
        JwtResponse jwt = userService.login(user);
                                                                 //403
        if (jwt == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
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

    @PostMapping("/update/reset-password")
    private ResponseEntity<Void> resetPassword(@RequestBody PasswordResetDto passwordResetDto){
        String password = passwordResetDto.getPassword();
        String email = jwtService.extractEmailFromResetToken(passwordResetDto.getToken());
        Users user = userService.findByEmail(email);
        if(email == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if(!jwtService.validateToken(passwordResetDto.getToken()) || user.getVerificationToken().equals(password)){
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        user.setPassword(password);
        userService.saveUser(user);
        return ResponseEntity.ok().build();
    }



//    @DeleteMapping("user/{user_id}")
//    public ResponseEntity<Void> deleteUser(@PathVariable("user_id") int id){
//        service.deleteUser(id);
//    }
}
