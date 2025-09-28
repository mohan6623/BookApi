package com.marvel.springsecurity.service.book;

import com.marvel.springsecurity.dto.BookDto;
import com.marvel.springsecurity.repo.BookRepo;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class BookService {

    private BookRepo repo;

    public BookService(BookRepo repo) {
        this.repo = repo;    }

    public void addBook(BookDto book, MultipartFile image) throws IOException {

        book.setImageName(image.getOriginalFilename());
        book.setImage(image.getBytes());
        book.setImageType(image.getContentType());
        repo.save(book);
    }


    public List<BookDto> getBooks() {
        return repo.findAll();
    }

    public boolean updateBook(int id, BookDto book, MultipartFile image) throws IOException {
        System.out.println("image : "+ image);
        var existing = repo.findById(id);
        if(existing.isEmpty()) {
            System.out.println("Book not found with id: " + id);
            return false;
        }
        existing.get().setTitle(book.getTitle());
        existing.get().setAuthor(book.getAuthor());
        existing.get().setDescription(book.getDescription());
        existing.get().setCategory(book.getCategory());


        if (image != null && !image.isEmpty()) {
            existing.get().setImageName(image.getOriginalFilename());
            existing.get().setImageType(image.getContentType());
            existing.get().setImage(image.getBytes());
        }
        repo.save(existing.get());
        return true;
    }


    public void deleteBook(int id) {
        repo.deleteById(id);
    }

    public BookDto getBookById(int id) {
        return repo.findById(id).orElse(null);
    }

    public List<BookDto> getBookByTitle(String title) {
        return repo.findByBookTitle(title);
    }

    public List<BookDto> searchBooks(String title, String author, String category) {
        return repo.searchBooks(title, author, category);
    }
}
