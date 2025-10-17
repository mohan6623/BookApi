package com.marvel.springsecurity.controller;

import com.marvel.springsecurity.dto.BookDto;
import com.marvel.springsecurity.dto.CommentsDto;
import com.marvel.springsecurity.model.Book;
import com.marvel.springsecurity.model.Rating;
import com.marvel.springsecurity.service.book.BookService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
//@RequestMapping("/api")
public class BookController {

    private final BookService service;
    //for constructor injection
    public BookController(BookService service) {
        this.service = service;
    }
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping(path = "/addbook")
    public ResponseEntity<String> addBook(@RequestPart Book book, @RequestPart MultipartFile imageFile) {
        try {
//            System.out.println("book = " + book);
            service.addBook(book, imageFile);
            return ResponseEntity.status(HttpStatus.CREATED).body("Book added successfully");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add book: " + e.getMessage());
        }
    }

//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    @GetMapping("/books")  //, produces = {"application/json"})
    public ResponseEntity<Page<BookDto>> getBooks(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size){
        Page<BookDto> books = service.getBooks(page, size);
        if (books.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(books);
    }

//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    @GetMapping("/bookid/{id}")
    public ResponseEntity<BookDto> getBook(@PathVariable int id){
        BookDto book = service.getBookById(id);
        if (book != null) return ResponseEntity.ok(book);
        return ResponseEntity.notFound().build();
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/book/{id}")
    public ResponseEntity<String> updateBook(@PathVariable int id, @RequestPart Book book, @RequestPart(required = false) MultipartFile imageFile) {
        try {
            boolean updated = service.updateBook(id, book, imageFile);
            if (updated) {
                return ResponseEntity.ok("Book updated successfully");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Book not found");
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update book: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/book/{id}")
    public ResponseEntity<String> deleteBook(@PathVariable int id) {
        service.deleteBook(id);
        return ResponseEntity.ok("Book deleted successfully");
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    @PostMapping("/book/{id}/rating")
    public ResponseEntity<Void> addRating(@PathVariable int id, @RequestBody Rating review){
        return service.addRating(id, review);
    }


    @GetMapping("/book/{id}/ratings")
    public ResponseEntity<Map<Integer, Integer>> getRatings(@PathVariable int id){
        Map<Integer, Integer> ratings = service.getRatings(id);
        return ResponseEntity.ok(ratings);
    }

//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    @GetMapping("/book/{id}/comment")
    public ResponseEntity<Page<CommentsDto>> getComments(@PathVariable int id, @RequestParam int page, @RequestParam int size){
        Page<CommentsDto> comments = service.getComments(id, page, size);
        if(comments.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(comments);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    @PostMapping("/book/{id}/comment")
    public ResponseEntity<CommentsDto> addComment(@PathVariable int id,@RequestBody CommentsDto comment){
        if(comment == null) return ResponseEntity.status(406).build();
        var newComment = service.addComment(id,comment);
        return ResponseEntity.ok(new CommentsDto(newComment));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    @PutMapping("/book/{id}/comment")
    public ResponseEntity<CommentsDto> updateComment(@RequestBody(required = true) CommentsDto comment){
        if(comment == null) return ResponseEntity.status(406).build();
        var updated = service.updateComment(comment);
        return ResponseEntity.ok(new CommentsDto(updated));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    @DeleteMapping("comment/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable int commentId){
        service.deleteComment(commentId);
        return ResponseEntity.ok().build();
    }

//    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    @GetMapping("/books/search")
    public ResponseEntity<Page<BookDto>> searchBooks(
        @RequestParam(required = false) String title,
        @RequestParam(required = false) String author,
        @RequestParam(required = false) String category,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "30") int size) {
        Page<BookDto> books = service.searchBooks(title, author, category, page, size);
        if (books == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(books);
    }



}
