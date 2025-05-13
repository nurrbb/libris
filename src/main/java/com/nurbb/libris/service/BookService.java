package com.nurbb.libris.service;

import com.nurbb.libris.model.dto.request.BookRequest;
import com.nurbb.libris.model.dto.response.BookDeleteResponse;
import com.nurbb.libris.model.dto.response.BookResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface BookService {

    BookResponse addBook(BookRequest request);

    BookResponse getBookById(UUID id);

    List<BookResponse> getAllBooks();

    Page<BookResponse> searchBooks(String query, int page, int size);

    BookResponse updateBook(UUID id, BookRequest request);

    BookDeleteResponse deleteBook(UUID id);

}
