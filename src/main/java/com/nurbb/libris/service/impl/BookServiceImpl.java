package com.nurbb.libris.service.impl;

import com.nurbb.libris.exception.NotFoundException;
import com.nurbb.libris.model.dto.request.AuthorRequest;
import com.nurbb.libris.model.dto.request.BookRequest;
import com.nurbb.libris.model.dto.response.BookResponse;
import com.nurbb.libris.model.entity.Author;
import com.nurbb.libris.model.entity.Book;
import com.nurbb.libris.model.mapper.AuthorMapper;
import com.nurbb.libris.model.mapper.BookMapper;
import com.nurbb.libris.repository.AuthorRepository;
import com.nurbb.libris.repository.BookRepository;
import com.nurbb.libris.service.BookService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final BookMapper bookMapper;
    private final AuthorMapper authorMapper;

    private final List<Consumer<Book>> bookCreationListeners = new ArrayList<>();

    @Override
    @Transactional
    public BookResponse addBook(BookRequest request) {
        validateBookInput(request);

        Set<Author> authors = resolveAuthorsByName(request.getAuthors());
        Book book = bookMapper.toEntity(request, authors);
        book.setAvailable(book.getCount() > 0);

        Book saved = bookRepository.save(book);
        bookCreationListeners.forEach(listener -> listener.accept(saved));

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
        return bookRepository.findAll()
                .stream()
                .map(bookMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<BookResponse> searchBooks(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Set<Book> combinedResults = new HashSet<>();
        combinedResults.addAll(bookRepository.findByTitleContainingIgnoreCase(query, pageable).getContent());
        combinedResults.addAll(bookRepository.findByIsbnContainingIgnoreCase(query, pageable).getContent());
        combinedResults.addAll(bookRepository.findByAuthorName(query, pageable).getContent());

        List<BookResponse> responseList = combinedResults.stream()
                .map(bookMapper::toResponse)
                .toList();

        return new PageImpl<>(responseList, pageable, responseList.size()); // Manuel page oluşturduk
    }
    @Override
    @Transactional
    public BookResponse updateBook(UUID id, BookRequest request) {
        Book existing = bookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book not found"));

        Set<Author> authors = resolveAuthorsByName(request.getAuthors());

        existing.setTitle(request.getTitle());
        existing.setIsbn(request.getIsbn());
        existing.setPublishedDate(request.getPublishedDate());
        existing.setGenre(request.getGenre());
        existing.setAvailable(request.getCount()> 0);
        existing.setAuthors(authors);

        return bookMapper.toResponse(bookRepository.save(existing));
    }

    @Override
    @Transactional
    public void deleteBook(UUID id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book not found"));
        bookRepository.delete(book);
    }

    private Set<Author> resolveAuthorsByName(Set<String> names) {
        return names.stream()
                .map(name -> authorRepository.findByNameIgnoreCase(name)
                        .orElseGet(() -> authorRepository.save(authorMapper.toEntity(new AuthorRequest(name)))))
                .collect(Collectors.toSet());
    }

//    private Set<Author> resolveAuthorsById(Set<UUID> authorIds) {
//        return authorIds.stream()
//                .map(id -> authorRepository.findById(id)
//                        .orElseThrow(() -> new NotFoundException("Author not found with id: " + id)))
//                .collect(Collectors.toSet());
//    }

    public void addBookCreationListener(Consumer<Book> listener) {
        bookCreationListeners.add(listener);
    }


    private void validateBookInput(BookRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Book title cannot be empty");
        }

        if (request.getIsbn() == null || request.getIsbn().trim().isEmpty()) {
            throw new IllegalArgumentException("ISBN cannot be empty");
        }

        if (request.getAuthors() == null || request.getAuthors().isEmpty()) {
            throw new IllegalArgumentException("At least one author must be specified");
        }

        if (request.getCount() == null || request.getCount() < 0) {
            throw new IllegalArgumentException("Book count must be 0 or greater");
        }

        if (request.getPublishedDate() == null) {
            throw new IllegalArgumentException("Published date cannot be null");
        }

        if (request.getGenre() == null) {
            throw new IllegalArgumentException("Genre must be provided");
        }
    }


}
