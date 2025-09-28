package com.marvel.springsecurity.repo;

import com.marvel.springsecurity.dto.BookDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepo extends JpaRepository<BookDto, Integer> {
// PostgreSQL specific query
//    @Query(value = "SELECT * FROM book_model b WHERE b.title ~* ?1", nativeQuery = true) // for case-insensitive search "~*".
// JPQL query
//    @Query("SELECT b FROM BookModel b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%',?1,'%'))")  // for case-insensitive search use "LOWER()".
    @Query(value = "SELECT * FROM book_model b WHERE b.title ILIKE CONCAT('%', ?1, '%')", nativeQuery = true)
    List<BookDto> findByBookTitle(String title);

    @Query(value = "SELECT * FROM book_dto b WHERE " +
        "(:title IS NULL OR b.title ILIKE CONCAT('%', :title, '%')) AND " +
        "(:author IS NULL OR b.author ILIKE CONCAT('%', :author, '%')) AND " +
        "(:category IS NULL OR b.category ILIKE CONCAT('%', :category, '%'))",
        nativeQuery = true)
    List<BookDto> searchBooks(String title, String author, String category);

}
