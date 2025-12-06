package com.marvel.springsecurity.controller;


import com.marvel.springsecurity.dto.JwtResponse;
import com.marvel.springsecurity.dto.UserDto;
import com.marvel.springsecurity.model.User;
import com.marvel.springsecurity.service.security.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    UserService service;



//    @GetMapping("csrf-token")
//    public CsrfToken getCsrf(HttpServletRequest request){
//        return (CsrfToken) request.getAttribute("_csrf");
//    }

//JWT
    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody User user){
//        System.out.println(user);
                                                                                                 //409
//        if(!service.usernameAndMailAvailable(user.getUsername(), user.getMail())) return ResponseEntity.status(HttpStatus.CONFLICT).build();
        if(!service.saveUser(user)) return ResponseEntity.internalServerError().build();
                                                //201
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/available/username")
    public ResponseEntity<Void> usernameAvailable(@RequestParam String username){
        if(username == null || username.isBlank()) return ResponseEntity.badRequest().build();
        boolean check = service.usernameAvailable(username);
        if(check) return ResponseEntity.ok().build();
                                                    //409
        else return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @GetMapping("/available/mail")
    public ResponseEntity<Void> mailAvailable(@RequestParam String mail){
        boolean check = service.mailAvailable(mail);
                                                                                    //409
        return check ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@RequestBody User user){
        JwtResponse jwt = service.login(user);
                                                                 //403
        if (jwt == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.accepted().body(jwt);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    @PutMapping("/user/{user_id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable("user_id") int id,
                                              @RequestPart User user,
                                              @RequestPart(required = false) MultipartFile imageFile){
        try {
            UserDto updated = service.updateUser(id, user, imageFile);
            return updated != null                    //202
                    ? ResponseEntity.status(HttpStatus.ACCEPTED).body(updated)
                    : ResponseEntity.notFound().build();
        } catch (IOException e) {
                                             //500
           return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



//    @DeleteMapping("user/{user_id}")
//    public ResponseEntity<Void> deleteUser(@PathVariable("user_id") int id){
//        service.deleteUser(id);
//    }
}
