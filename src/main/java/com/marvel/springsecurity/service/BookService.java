package com.marvel.springsecurity.service;

import com.marvel.springsecurity.model.BookModel;
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

    public void addBook(BookModel book, MultipartFile image) throws IOException {

        book.setImageName(image.getOriginalFilename());
        book.setImage(image.getBytes());
        book.setImageType(image.getContentType());
        repo.save(book);
    }


    public List<BookModel> getBooks() {
        return repo.findAll();
    }

    public boolean updateBook(int id, BookModel book, MultipartFile image) throws IOException {
        var existing = repo.findById(id);
        if(existing.isEmpty()) return false;

        existing.get().setImage(book.getImage());
        existing.get().setImageType(book.getImageType());
        existing.get().setImageName(book.getImageName());

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

    public BookModel getBookById(int id) {
        return repo.findById(id).orElse(null);
    }

    public List<BookModel> getBookByTitle(String title) {
        return repo.findByBookTitle(title);
    }
}
