package com.marvel.springsecurity.service.book;


import com.marvel.springsecurity.dto.AvgAndCountProjection;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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

    public BookDto getBookById(int bookId) {
        Book book = bookRepo.findById(bookId).orElse(null);
        if(book == null) return null;
        AvgAndCountProjection rating = getAvgAndCountRating(bookId);
//        if(rating != null) {
//            System.out.println(rating.getAverage()+" "+rating.getCount());
            return new BookDto(book, rating);
//        }
//        else {
//            var dto = new BookDto(book);
//            dto.setAverageRating(0.0);
//            dto.setNoOfRatings(0);
//            return dto;
//        }
    }

    public Page<BookDto> getBooks(int page, int size) {
        var pageable = PageRequest.of(page, size);
        Page<Object[]> data = bookRepo.findBooksWithRatings(pageable);
//        test();
        return data.map(BookDto::new);
    }

    public void test(){
        AvgAndCountProjection o = getAvgAndCountRating(17);
        System.out.println(o.getAverage());
        System.out.println(o.getCount());
//        System.out.println(o.);
//        System.out.println(o[1]);
    }
    private AvgAndCountProjection getAvgAndCountRating(int bookId){
//        System.out.println("bookId = " + bookId);
        return ratingRepo.AverageAndCountByBookId(bookId);
    }

    public boolean updateBook(int id, Book book, MultipartFile image) throws IOException {
        var existing = bookRepo.findById(id);
        if(existing.isEmpty()) {
//            System.out.println("Book not found with id: " + id);
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

    @Transactional
    public void deleteBook(int id) {
        commentRepo.deleteAllByBookId(id);
        ratingRepo.deleteAllByBookId(id);
        bookRepo.deleteById(id);

    }

    public Page<BookDto> searchBooks(String title, String author, String category, int page, int size) {
        var pageable = PageRequest.of(page, size);
        Page<Object[]> data = bookRepo.searchBooks(title, author, category, pageable);
        return data.map(line ->
                new BookDto((Book) line[0],(Double) line[1]));
    }

    public ResponseEntity<Void> addRating(int bookId, Rating rating) {
        int userId = getUserId();
        System.out.println(userId);
        if (userId == -1) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        // Check if book exists
        if (!bookRepo.existsById(bookId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found");
        }
        // Validate rating value
        if (rating.getRating() < 1 || rating.getRating() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5");
        }
        var oldRating = ratingRepo.findByUserIdAndBookId(userId, bookId);
        if(oldRating != null){
            oldRating.setRating(rating.getRating());
            ratingRepo.save(oldRating);
            return ResponseEntity.ok().build();
        }
            Book book = new Book();
            book.setId(bookId);
            User user = new User();
            user.setId(userId);
            rating.setUser(user);
            rating.setBook(book);

        ratingRepo.save(rating);
        return ResponseEntity.ok().build();
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


    public Comment addComment(int id, CommentsDto comment) {
        int userId = getUserId();
        if (userId == -1) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        // Check if book exists
        if (!bookRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found");
        }

        // Validate comment content
        if (comment.getComment() == null || comment.getComment().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment cannot be empty");
        }

        if (comment.getComment().length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment too long (max 1000 characters)");
        }

        return commentRepo.save(new Comment(comment, userId));
    }

    public Page<CommentsDto> getComments(int id, int page, int size) {
        var pageable = PageRequest.of(page, size);
        Page<Comment> comments = commentRepo.findAllByBookId(id, pageable);

        return comments.map(c ->
                        new CommentsDto(
                            c.getId(),
                            c.getComment(),
                            c.getBook().getId(),
                            c.getUser().getUsername(),
                            c.getCreatedAt()
                ));

    }

    public Comment updateComment(CommentsDto comment) {
        int userId = getUserId();
        if (userId == -1) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        // Fetch existing comment and verify ownership
        Comment existingComment = commentRepo.findById(comment.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        if (existingComment.getUser().getId() != userId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to edit this comment");
        }

        // Validate comment content
        if (comment.getComment() == null || comment.getComment().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment cannot be empty");
        }

        if (comment.getComment().length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment too long (max 1000 characters)");
        }

        // Update only the comment text, preserve other fields
        existingComment.setComment(comment.getComment());
        return commentRepo.save(existingComment);
    }

    public void deleteComment(int id) {
        int userId = getUserId();
        if (userId == -1) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        // Fetch comment and verify ownership
        Comment comment = commentRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        // Allow deletion if user is the owner or has ADMIN role
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (comment.getUser().getId() != userId && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to delete this comment");
        }

        commentRepo.deleteById(id);
    }


    private String getUsername(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null)? auth.getName() : null;
    }
    public int getUserId(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
            return userPrincipal.getUserId();
        }
        return -1;
    }

}
