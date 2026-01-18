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
    private Integer id;

    @Column(nullable = false, length = 1000)
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public Comment(CommentsDto comment, int userId){
        this.comment = comment.getComment();
        this.book = new Book();
        this.user = new Users();
        this.book.setBookId(comment.getBookId());
        this.user.setUserId(userId);

    }

    public CommentsDto toDto() {
        return CommentsDto.builder()
                .id(this.id)
                .comment(this.comment)
                .bookId(this.book.getBookId())
                .username(this.user.getUsername())
                .createdAt(this.createdAt)
                .profilePic(this.user.getImageUrl())
                .build();
//        return new CommentsDto(
//                this.id,
//                this.comment,
//                this.book.getBookId(),
//                this.user.getUsername(),
//                this.createdAt,
//                this.user.getImageUrl()
//        );
    }
}
