package com.marvel.springsecurity;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebController {

    @GetMapping("/")
    public String home(HttpServletRequest request){
        return "Welcome to Marvel "+ request.getSession().getId();
    }



}
