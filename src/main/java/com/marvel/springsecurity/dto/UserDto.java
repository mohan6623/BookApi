package com.marvel.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private int id;
    private String username;
    private String name;
    private String email;
    private Boolean emailVerified;
    private Instant createdAt;
    private Instant updatedAt;
    private String imagePublicId;
    private String imageUrl;
//    private String provider;
}
