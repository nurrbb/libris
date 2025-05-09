package com.nurbb.libris.service.impl;

import com.nurbb.libris.exception.InvalidRequestException;
import com.nurbb.libris.exception.NotFoundException;
import com.nurbb.libris.model.dto.request.BookRequest;
import com.nurbb.libris.model.dto.response.BookResponse;
import com.nurbb.libris.model.entity.Author;
import com.nurbb.libris.model.entity.Book;
import com.nurbb.libris.model.mapper.BookMapper;
import com.nurbb.libris.repository.BookRepository;
import com.nurbb.libris.service.AuthorService;
import com.nurbb.libris.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final AuthorService authorService;
    private final BookMapper bookMapper;

    @Override
    @Transactional
    public BookResponse addBook(BookRequest request) {
        validateBookInput(request);

        if (bookRepository.existsByIsbn(request.getIsbn())) {
            log.warn("Book with ISBN '{}' already exists.", request.getIsbn());
            throw new InvalidRequestException("Book with the same ISBN already exists.");
        }

        log.info("Book '{}' added to the library by author '{}'", request.getTitle(), request.getAuthorName());

        Author author = authorService.getAuthorByNameOrCreate(request.getAuthorName());
        Book book = bookMapper.toEntity(request, author);
        Book saved = bookRepository.save(book);

        return bookMapper.toResponse(saved);
    }

    @Override
    public BookResponse getBookById(UUID id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book not found with id: " + id));
        return bookMapper.toResponse(book);
    }

    @Override
    public List<BookResponse> getAllBooks() {
        return bookRepository.findAll().stream()
                .map(bookMapper::toResponse)
                .toList();
    }

    @Override
    public Page<BookResponse> searchBooks(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Set<Book> mergedResults = Stream.of(
                bookRepository.findByTitleContainingIgnoreCase(query, pageable),
                bookRepository.findByAuthor_NameContainingIgnoreCase(query, pageable),
                bookRepository.findByIsbnContainingIgnoreCase(query, pageable)
        ).flatMap(p -> p.getContent().stream()).collect(Collectors.toSet());

        List<BookResponse> responseList = mergedResults.stream()
                .map(bookMapper::toResponse)
                .toList();

        return new PageImpl<>(responseList, pageable, responseList.size());
    }

    @Override
    @Transactional
    public BookResponse updateBook(UUID id, BookRequest request) {
        validateBookInput(request);

        Book existing = bookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book not found with id: " + id));

        Author author = authorService.getAuthorByNameOrCreate(request.getAuthorName());

        existing.setTitle(request.getTitle());
        existing.setIsbn(request.getIsbn());
        existing.setPublishedDate(request.getPublishedDate());
        existing.setGenre(request.getGenre());
        existing.setCount(request.getCount());
        existing.setAvailable(request.getCount() > 0);
        existing.setAuthor(author);
        existing.setPageCount(request.getPageCount());
        log.info("Book with ID '{}' updated. New title: '{}', New count: {}", id, request.getTitle(), request.getCount());
        return bookMapper.toResponse(bookRepository.save(existing));
    }

    @Override
    @Transactional
    public void deleteBook(UUID id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book not found with id: " + id));
        log.info("Book with ID '{}' and title '{}' deleted from the library", book.getId(), book.getTitle());
        bookRepository.delete(book);
    }


    private void validateBookInput(BookRequest request) {
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new InvalidRequestException("Book title cannot be empty");
        }
        if (request.getIsbn() == null || request.getIsbn().isBlank()) {
            throw new InvalidRequestException("ISBN cannot be empty");
        }
        if (request.getAuthorName() == null || request.getAuthorName().isBlank()) {
            throw new InvalidRequestException("Author name must be specified");
        }
        if (request.getCount() == null || request.getCount() < 0) {
            throw new InvalidRequestException("Book count must be 0 or greater");
        }
        if (request.getPublishedDate() == null) {
            throw new InvalidRequestException("Published date cannot be null");
        }
        if (request.getGenre() == null) {
            throw new InvalidRequestException("Genre must be provided");
        }
        if (request.getPageCount() <= 0) {
            throw new InvalidRequestException("Page count must be greater than 0");
        }
    }

}
