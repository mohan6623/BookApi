package com.marvel.springsecurity.service.book;

import com.marvel.springsecurity.model.Book;
import com.marvel.springsecurity.model.Rating;
import com.marvel.springsecurity.repo.BookRepo;
import com.marvel.springsecurity.repo.ReviewRepo;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class BookService {

    private BookRepo bookRepo;
    private ReviewRepo reviewRepo;

    public BookService(BookRepo bookRepo, ReviewRepo reviewRepo) {
        this.bookRepo = bookRepo;
        this.reviewRepo = reviewRepo;
    }

    public void addBook(Book book, MultipartFile image) throws IOException {

        book.setImageName(image.getOriginalFilename());
        book.setImage(image.getBytes());
        book.setImageType(image.getContentType());
        bookRepo.save(book);
    }


    public List<Book> getBooks() {
        return bookRepo.findAll();
    }

    public boolean updateBook(int id, Book book, MultipartFile image) throws IOException {
        System.out.println("image : "+ image);
        var existing = bookRepo.findById(id);
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
        bookRepo.save(existing.get());
        return true;
    }


    public void deleteBook(int id) {
        bookRepo.deleteById(id);
    }

    public Book getBookById(int id) {
        return bookRepo.findById(id).orElse(null);
    }

    public List<Book> getBookByTitle(String title) {
        return bookRepo.findByBookTitle(title);
    }

    public List<Book> searchBooks(String title, String author, String category) {
        return bookRepo.searchBooks(title, author, category);
    }

    public void addReview(int id, Rating review) {
        Book book = new Book();
        book.setId(id);
        review.setBook(book);
        reviewRepo.save(review);
    }
}
