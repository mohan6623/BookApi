package com.marvel.springsecurity.controller;

import com.marvel.springsecurity.dto.BookDto;
import com.marvel.springsecurity.service.book.BookService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
//@RequestMapping("/api")
public class BookController {

    private final BookService service;
//for constructor injection
    public BookController(BookService service) {
        this.service = service;
    }
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping(path = "/addbook")
    public ResponseEntity<String> addBook(@RequestPart BookDto book, @RequestPart MultipartFile imageFile) {
        try {
//            System.out.println("book = " + book);
            service.addBook(book, imageFile);
            return ResponseEntity.status(HttpStatus.CREATED).body("Book added successfully");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to add book: " + e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    @GetMapping("/books")  //, produces = {"application/json"})
    public ResponseEntity<List<BookDto>> getBooks(){
        List<BookDto> books = service.getBooks();
        if (books.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        // Set imageBase64 for each book
        for (BookDto book : books) {
            if (book.getImage() != null) {
                book.setImageBase64(book.getImageBase64());
            }
        }
        return ResponseEntity.ok(books);
    }
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    @GetMapping("/book/{id}")
    public ResponseEntity<BookDto> getBook(@PathVariable int id){
        BookDto book = service.getBookById(id);
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

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/book/{id}")
    public ResponseEntity<String> updateBook(@PathVariable int id, @RequestPart BookDto book, @RequestPart(required = false) MultipartFile imageFile) {
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

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/book/{id}")
    public ResponseEntity<String> deleteBook(@PathVariable int id) {
        service.deleteBook(id);
        return ResponseEntity.ok("Book deleted successfully");
    }

//    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
//    @GetMapping("/title/{title}")
//    public ResponseEntity<List<BookDto>> getBookByTitle(@PathVariable String title){
//        List<BookDto> books = service.getBookByTitle(title);
//        if(books.isEmpty()){
//            return ResponseEntity.noContent().build();
//        }
//        return ResponseEntity.ok(books);
//    }

    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    @GetMapping("/books/search")
    public ResponseEntity<List<BookDto>> searchBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String category) {
        List<BookDto> books = service.searchBooks(title, author, category);
        if (books.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        // Set imageBase64 for each book in search results
        for (BookDto book : books) {
            if (book.getImage() != null) {
                book.setImageBase64(book.getImageBase64());
            }
        }
        return ResponseEntity.ok(books);
    }
}
