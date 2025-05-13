package com.nurbb.libris.service.impl;

import com.nurbb.libris.exception.InvalidRequestException;
import com.nurbb.libris.exception.NotFoundException;
import com.nurbb.libris.model.entity.Author;
import com.nurbb.libris.repository.AuthorRepository;
import com.nurbb.libris.service.AuthorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorServiceImpl implements AuthorService {

    private final AuthorRepository authorRepository;

    @Override
    public Author createAuthor(String name) {

        if (name == null || name.trim().isBlank()) {
            throw new InvalidRequestException("Author name cannot be empty.");
        }

        String trimmedName = name.trim();

        if (authorRepository.existsByNameIgnoreCase(trimmedName)) {
            log.warn("Author creation failed. '{}' already exists.", trimmedName);
            throw new InvalidRequestException("Author with name '" + trimmedName + "' already exists.");
        }
        Author author = new Author();
        author.setName(trimmedName);
        log.info("Author '{}' created successfully.", trimmedName);
        return authorRepository.save(author);
    }

    @Cacheable(value = "authorList")
    @Override
    public List<Author> getAllAuthors() {
        return authorRepository.findAll();
    }

    @Cacheable(value = "authorById", key = "#id")
    @Override
    public Optional<Author> getAuthorById(UUID id) {
        return authorRepository.findById(id);
    }

    @Override
    public Optional<Author> getAuthorByName(String name) {
        return authorRepository.findByNameIgnoreCase(name.trim());
    }

    @Override
    public Author getAuthorByNameOrCreate(String name) {

        if (name == null || name.trim().isBlank()) {
            throw new InvalidRequestException("Author name cannot be empty.");
        }

        String trimmed = name.trim();
        return authorRepository.findByNameIgnoreCase(trimmed)
                .orElseGet(() -> {
                    Author author = new Author();
                    author.setName(trimmed);
                    log.info("Author '{}' not found. Created new author.", trimmed);
                    return authorRepository.save(author);
                });
    }

// Deletes the author by ID if they have no assigned books (evicts cache).

    @CacheEvict(value = { "authorList", "authorById" }, key = "#id")
    @Override
    public void deleteAuthor(UUID id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Author not found with id: " + id));

        if (!author.getBooks().isEmpty()) {
            throw new InvalidRequestException("Cannot delete author who has books assigned.");
        }

        authorRepository.delete(author);
        log.info("Author with ID '{}' and name '{}' has been deleted.", author.getId(), author.getName());
    }
}
