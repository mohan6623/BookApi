package com.marvel.springsecurity.model;


import com.marvel.springsecurity.dto.CommentsDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, length = 1000)
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public Comment(CommentsDto comment, int userId){
        this.id = comment.getId();
        this.comment = comment.getComment();
        this.book = new Book();
        this.user = new User();
        this.book.setId(comment.getBookId());
        this.user.setId(userId);
        this.createdAt = comment.getCreatedAt();

    }
}
