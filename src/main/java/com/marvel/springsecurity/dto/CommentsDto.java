package com.marvel.springsecurity.dto;

import com.marvel.springsecurity.model.Comment;
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

    public CommentsDto(Comment newComment) {
        this.id = newComment.getId();
        this.comment = newComment.getComment();
        this.bookId = newComment.getBook().getId();
        this.username = newComment.getUser().getUsername();
        this.createdAt = newComment.getCreatedAt();
    }
}
