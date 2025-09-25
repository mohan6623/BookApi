package com.marvel.springsecurity.repo;

import com.marvel.springsecurity.model.BookModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepo extends JpaRepository<BookModel, Integer> {
// PostgreSQL specific query
//    @Query(value = "SELECT * FROM book_model b WHERE b.title ~* ?1", nativeQuery = true) // for case-insensitive search "~*".
// JPQL query
//    @Query("SELECT b FROM BookModel b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%',?1,'%'))")  // for case-insensitive search use "LOWER()".
    @Query(value = "SELECT * FROM book_model b WHERE b.title ILIKE CONCAT('%', ?1, '%')", nativeQuery = true)
    List<BookModel> findByBookTitle(String title);

}
