package com.nurbb.libris.service;

import com.nurbb.libris.model.dto.request.BookRequest;
import com.nurbb.libris.model.dto.response.BookResponse;
import com.nurbb.libris.model.entity.Genre;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@Service
public interface BookService {

    BookResponse addBook(BookRequest request);

    BookResponse getBookById(UUID id);

    List<BookResponse> getAllBooks();

    Page<BookResponse> searchBooks(String query, int page, int size);

    BookResponse updateBook(UUID id, BookRequest request);


    void deleteBook(UUID id);
}
