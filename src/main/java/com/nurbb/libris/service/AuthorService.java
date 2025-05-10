package com.nurbb.libris.service;

import com.nurbb.libris.model.entity.Author;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthorService
{
    Author createAuthor(String name);

    List<Author> getAllAuthors();

    Optional<Author> getAuthorById(UUID id);

    Optional<Author> getAuthorByName(String name);

    Author getAuthorByNameOrCreate(String name);

    void deleteAuthor(UUID id);

}
