package com.marvel.springsecurity.controller;

import com.marvel.springsecurity.service.security.rateLimiting.CaffeineRateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    @Autowired
    private CaffeineRateLimiter rateLimiter;

    @GetMapping("/rate-limit-status")
    public ResponseEntity<String> getRateLimitStatus(){
        return ResponseEntity.ok(rateLimiter.getCacheStats());
    }
}
