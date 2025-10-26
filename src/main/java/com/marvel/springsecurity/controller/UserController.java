package com.marvel.springsecurity.controller;


import com.marvel.springsecurity.dto.JwtResponse;
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
public class UserController {

    @Autowired
    UserService service;



//    @GetMapping("csrf-token")
//    public CsrfToken getCsrf(HttpServletRequest request){
//        return (CsrfToken) request.getAttribute("_csrf");
//    }

//JWT
    @PostMapping("register")
    public ResponseEntity<Void> register(@RequestBody User user){
        if (!service.saveUser(user)) return ResponseEntity.internalServerError().build();
                                   //Created
        return ResponseEntity.status(201).build();
    }

    @PostMapping("login")
    public ResponseEntity<JwtResponse> login(@RequestBody User user){
        JwtResponse jwt = service.login(user);
                                                    //Forbidden
        if (jwt == null) return ResponseEntity.status(403).build();
        return ResponseEntity.accepted().body(jwt);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    @PutMapping("user/{user_id}")
    public ResponseEntity<Void> updateUser(@PathVariable("user_id") int id, @RequestPart User user, @RequestPart(required = false) MultipartFile imageFile){
        boolean updated = false;
        try {
            System.out.println(user);
            updated = service.updateUser(id, user, imageFile);
        } catch (IOException e) {
                                             //500
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update User info " + e.getMessage());
        }
        if (updated) return ResponseEntity.accepted().build();
        else return ResponseEntity.notFound().build();
    }

//    @DeleteMapping("user/{user_id}")
//    public ResponseEntity<Void> deleteUser(@PathVariable("user_id") int id){
//        service.deleteUser(id);
//    }
}
