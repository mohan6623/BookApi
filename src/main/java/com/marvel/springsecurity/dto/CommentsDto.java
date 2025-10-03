package com.marvel.springsecurity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentsDto {
    private int id;
    private String comment;
    private int bookId;
    private String username;
    private LocalDateTime createdAt;

}
