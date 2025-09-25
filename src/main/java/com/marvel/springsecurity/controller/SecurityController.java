package com.marvel.springsecurity.controller;


import com.marvel.springsecurity.model.User;
import com.marvel.springsecurity.service.JwtService;
import com.marvel.springsecurity.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecurityController {

    User user;
    @Autowired
    UserService service;
    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    JwtService jwtService;

//    @GetMapping("csrf-token")
//    public CsrfToken getCsrf(HttpServletRequest request){
//        return (CsrfToken) request.getAttribute("_csrf");
//    }

//JWT
    @PostMapping("register")
    public void register(@RequestBody User user){
        service.saveUser(user);
    }

    @PostMapping("login")
    public String login(@RequestBody User user){

        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(),user.getPassword()));

        return jwtService.generateToken(user.getUsername());

    }


}
