package com.marvel.springsecurity.repo;

import com.marvel.springsecurity.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepo extends JpaRepository<Comment, Integer> {

    List<Comment> findAllByBookId(int id);

    void deleteAllByBookId(int id);
}

