package com.marvel.springsecurity.dto;

import com.marvel.springsecurity.model.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentsDto {
    private Integer id;
    private String comment;
    private Integer bookId;
    private String username;
    private LocalDateTime createdAt;
    private String profilePic;

    public CommentsDto(Comment newComment) {
        this.id = newComment.getId();
        this.comment = newComment.getComment();
        this.bookId = newComment.getBook().getBookId();
        this.username = newComment.getUser().getUsername();
        this.createdAt = newComment.getCreatedAt();
        this.profilePic = newComment.getUser().getImageUrl();
    }
}
