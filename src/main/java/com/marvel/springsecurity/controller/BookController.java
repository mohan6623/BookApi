package com.marvel.springsecurity.controller;

import com.marvel.springsecurity.dto.BookDto;
import com.marvel.springsecurity.dto.CommentsDto;
import com.marvel.springsecurity.model.Book;
import com.marvel.springsecurity.model.Comment;
import com.marvel.springsecurity.model.Rating;
import com.marvel.springsecurity.service.book.BookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
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

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    @GetMapping("/books")  //, produces = {"application/json"})
    public ResponseEntity<List<BookDto>> getBooks(){
        List<BookDto> books = service.getBooks();
        if (books.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(books);
    }
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    @GetMapping("/book/{id}")
    public ResponseEntity<Book> getBook(@PathVariable int id){
        Book book = service.getBookById(id);
        if (book == null) {
            return ResponseEntity.notFound().build();
        }
        if (book.getImage() != null) {
            // Set imageBase64 for the book
            book.setImageBase64(book.getImageBase64());
        } else {
            System.out.println("No image found for book");
        }
        return ResponseEntity.ok(book);
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

    @PostMapping("/book/{id}/addrating")
    public ResponseEntity<Void> addRating(@PathVariable int id, @RequestBody Rating review){
        return service.addRating(id, review);
    }

    @PostMapping("/book/{id}/addcomment")
    public ResponseEntity<Void> addComment(@PathVariable int id,@RequestBody Comment comment){
        return service.addComment(id,comment);
    }

    @GetMapping("/book/{id}/getcomment")
    public ResponseEntity<List<CommentsDto>> getComments(@PathVariable int id){
        List<CommentsDto> comments = service.getComments(id);
        if(comments.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/book/{id}/getratings")
    public ResponseEntity<Map<Integer, Integer>> getRatings(@PathVariable int id){
        Map<Integer, Integer> ratings = service.getRatings(id);
        return ResponseEntity.ok(ratings);
    }


    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_USER')")
    @GetMapping("/books/search")
    public ResponseEntity<List<Book>> searchBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String category) {
        List<Book> books = service.searchBooks(title, author, category);
        if (books.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        // Set imageBase64 for each book in search results
        for (Book book : books) {
            if (book.getImage() != null) {
                book.setImageBase64(book.getImageBase64());
            }
        }
        return ResponseEntity.ok(books);
    }



}
