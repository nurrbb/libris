package com.nurbb.libris.service.impl;

import com.nurbb.libris.model.entity.Author;
import com.nurbb.libris.repository.AuthorRepository;
import com.nurbb.libris.service.AuthorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthorServiceImpl implements AuthorService {

    private final AuthorRepository authorRepository;

    @Override
    public Author createAuthor(String name) {
        String trimmedName = name.trim();
        if (authorRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new IllegalArgumentException("Author with name '" + trimmedName + "' already exists.");
        }
        Author author = new Author();
        author.setName(trimmedName);
        return authorRepository.save(author);
    }

    @Override
    public List<Author> getAllAuthors() {
        return authorRepository.findAll();
    }

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
        String trimmed = name.trim();
        return authorRepository.findByNameIgnoreCase(trimmed)
                .orElseGet(() -> {
                    Author author = new Author();
                    author.setName(trimmed);
                    return authorRepository.save(author);
                });
    }
}
