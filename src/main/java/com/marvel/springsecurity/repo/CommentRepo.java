package com.marvel.springsecurity.repo;

import com.marvel.springsecurity.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepo extends JpaRepository<Comment, Integer> {

    Page<Comment> findAllByBook_BookId(int id, Pageable pageable);

    void deleteAllByBook_BookId(int id);

    void deleteByUser_UserId(int id);
}

