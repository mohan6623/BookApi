package com.marvel.springsecurity.controller;


import com.marvel.springsecurity.dto.JwtResponse;
import com.marvel.springsecurity.dto.User;
import com.marvel.springsecurity.service.security.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecurityController {

    User user;
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
        return ResponseEntity.status(201).build();
    }

    @PostMapping("login")
    public ResponseEntity<JwtResponse> login(@RequestBody User user){
        JwtResponse logined = service.login(user);
        if (logined.getToken() == null) return ResponseEntity.status(403).build();
        return ResponseEntity.accepted().body(logined);
    }


}
