package com.marvel.springsecurity.service.book;


import com.marvel.springsecurity.dto.BookDto;
import com.marvel.springsecurity.dto.CommentsDto;
import com.marvel.springsecurity.model.Book;
import com.marvel.springsecurity.model.Comment;
import com.marvel.springsecurity.model.Rating;
import com.marvel.springsecurity.model.User;
import com.marvel.springsecurity.repo.BookRepo;
import com.marvel.springsecurity.repo.CommentRepo;
import com.marvel.springsecurity.repo.RatingRepo;
import com.marvel.springsecurity.repo.UserRepository;
import com.marvel.springsecurity.service.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BookService {

    private final BookRepo bookRepo;
    private final RatingRepo ratingRepo;
    private final CommentRepo commentRepo;
    private final UserRepository userRepo;

    public BookService(BookRepo bookRepo, RatingRepo ratingRepo, CommentRepo commentRepo, UserRepository userRepo) {
        this.bookRepo = bookRepo;
        this.ratingRepo = ratingRepo;
        this.commentRepo = commentRepo;
        this.userRepo = userRepo;
    }

    public void addBook(Book book, MultipartFile image) throws IOException {

        book.setImageName(image.getOriginalFilename());
        book.setImage(image.getBytes());
        book.setImageType(image.getContentType());
        bookRepo.save(book);
    }

    public Book getBookById(int id) {
        return bookRepo.findById(id).orElse(null);
    }

    public List<BookDto> getBooks() {

        List<Book> books = bookRepo.findAll();
        List<Rating> ratings = ratingRepo.findAll();

        return books.stream()
                .map(b -> {
                    b.setImageBase64(b.getImageBase64());
                    double average = getRating(ratings, b.getId());
                    return new BookDto(b, average);
                }).toList();
    }

    private double getRating(List<Rating> ratings, int id){
        return ratings.isEmpty() ? 0.0 : ratings.stream()
                .filter(f -> f.getBook().getId() == id)
                .mapToInt(Rating::getRating)
                .average()
                .orElse(0.0);
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

//    public List<Book> getBookByTitle(String title) {
//        return bookRepo.findByBookTitle(title);
//    }

    public List<Book> searchBooks(String title, String author, String category) {
        return bookRepo.searchBooks(title, author, category);
    }

    public ResponseEntity<Void> addRating(int bookId, Rating rating) {
        if (getUserId() == -1) return ResponseEntity.status(401).build();
        Book book = new Book();
        book.setId(bookId);
        User user = new User();
        user.setId(getUserId());
        rating.setUser(user);
        rating.setBook(book);
        ratingRepo.save(rating);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<Void> addComment(int id, Comment comment) {
        if (getUserId() == -1) return ResponseEntity.status(401).build();
        Book book = new Book();
        book.setId(id);
        User user = new User();
        user.setId(getUserId());
        comment.setUser(user);
        comment.setBook(book);
        commentRepo.save(comment);
        return ResponseEntity.ok().build();
    }

    public List<CommentsDto> getComments(int id) {
        List<Comment> comments = commentRepo.findAllByBookId(id);

        return comments.stream()
                .map(c -> new CommentsDto(
                        c.getId(),
                        c.getComment(),
                        c.getUser().getUsername(),
                        c.getCreatedAt()
                ))
                .toList();

    }

    public Map<Integer, Integer> getRatings(int id) {
        List<Rating> ratings = ratingRepo.findAllByBookId(id);
        if (ratings.isEmpty()) return new HashMap<>();
        Map<Integer, Integer> ratingCount = new HashMap<>(Map.of(
            1, 0,
            2, 0,
            3, 0,
            4, 0,
            5, 0
        ));

        for(Rating r: ratings){
            switch (r.getRating()){
                case 1 -> ratingCount.put(1, ratingCount.get(1)+1);
                case 2 -> ratingCount.put(2, ratingCount.get(2)+1);
                case 3 -> ratingCount.put(3, ratingCount.get(3)+1);
                case 4 -> ratingCount.put(4, ratingCount.get(4)+1);
                case 5 -> ratingCount.put(5, ratingCount.get(5)+1);
                default -> {}
            }
        }
        return ratingCount;

    }



    private String getUsername(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null)? auth.getName() : null;
    }
    private int getUserId(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
            return userPrincipal.getUserId();
        }
        return -1;
    }

}



