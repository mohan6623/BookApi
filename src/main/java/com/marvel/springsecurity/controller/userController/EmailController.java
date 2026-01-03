package com.marvel.springsecurity.controller.userController;

import com.marvel.springsecurity.dto.JwtResponse;
import com.marvel.springsecurity.dto.PasswordResetDto;
import com.marvel.springsecurity.service.security.EmailService;
import com.marvel.springsecurity.service.security.JwtService;
import com.marvel.springsecurity.service.security.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/")
public class EmailController {

    private final EmailService emailService;
    private final JwtService jwtService;
    private final UserService userService;

    public EmailController(EmailService emailService, JwtService jwtService, UserService userService) {
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.userService = userService;
    }


    //verifies the email
    @GetMapping("validate/verify-email")
    public ResponseEntity<JwtResponse> verifyEmail(@RequestParam String token){
        JwtResponse response = emailService.validateEmailVerification(token);
        if(response != null) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    //check for token validation alone
    @GetMapping("validate/forgot-password")
    public ResponseEntity<Void> validateForgotPasswordToken(@RequestParam String token){
        boolean isValid = emailService.validateForgotPasswordToken(token);
        if(!isValid) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok().build();
    }

    //updates the password
    @PutMapping("validate/forgot-password")
    public ResponseEntity<Void> updatePassword(@RequestBody PasswordResetDto passwordResetDto){
        boolean isValid = userService.updatePassword(passwordResetDto);
        if(!isValid) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok().build();
    }

}
