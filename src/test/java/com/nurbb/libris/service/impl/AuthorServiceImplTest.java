package com.nurbb.libris.service.impl;

import com.nurbb.libris.exception.InvalidRequestException;
import com.nurbb.libris.exception.NotFoundException;
import com.nurbb.libris.model.entity.Author;
import com.nurbb.libris.model.entity.Book;
import com.nurbb.libris.repository.AuthorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorServiceImplTest {

    @InjectMocks
    private AuthorServiceImpl authorService;

    @Mock
    private AuthorRepository authorRepository;

    private UUID authorId;
    private Author author;

    @BeforeEach
    void setUp() {
        authorId = UUID.randomUUID();
        author = new Author();
        author.setId(authorId);
        author.setName("Jane Austen");
        author.setBooks(Collections.emptyList());
    }

    @Test
    void createAuthor_shouldSave_whenNameIsUnique() {
        String name = "  Jane Austen ";
        when(authorRepository.existsByNameIgnoreCase("Jane Austen")).thenReturn(false);
        when(authorRepository.save(any())).thenReturn(author);

        Author result = authorService.createAuthor(name);

        assertNotNull(result);
        assertEquals("Jane Austen", result.getName());
        verify(authorRepository).save(any());
    }

    @Test
    void createAuthor_shouldThrow_whenAuthorAlreadyExists() {
        when(authorRepository.existsByNameIgnoreCase("Jane Austen")).thenReturn(true);

        assertThrows(InvalidRequestException.class,
                () -> authorService.createAuthor("Jane Austen"));
    }

    @Test
    void getAllAuthors_shouldReturnList() {
        when(authorRepository.findAll()).thenReturn(List.of(author));

        List<Author> result = authorService.getAllAuthors();

        assertEquals(1, result.size());
        assertEquals("Jane Austen", result.get(0).getName());
    }

    @Test
    void getAuthorById_shouldReturnOptional() {
        when(authorRepository.findById(authorId)).thenReturn(Optional.of(author));

        Optional<Author> result = authorService.getAuthorById(authorId);

        assertTrue(result.isPresent());
        assertEquals(authorId, result.get().getId());
    }

    @Test
    void getAuthorByName_shouldReturnOptional() {
        when(authorRepository.findByNameIgnoreCase("Jane Austen")).thenReturn(Optional.of(author));

        Optional<Author> result = authorService.getAuthorByName("Jane Austen");

        assertTrue(result.isPresent());
    }

    @Test
    void getAuthorByNameOrCreate_shouldReturnExistingAuthor() {
        when(authorRepository.findByNameIgnoreCase("Jane Austen")).thenReturn(Optional.of(author));

        Author result = authorService.getAuthorByNameOrCreate("  Jane Austen  ");

        assertEquals(author.getId(), result.getId());
        verify(authorRepository, never()).save(any());
    }

    @Test
    void getAuthorByNameOrCreate_shouldCreateNewAuthor_whenNotFound() {
        when(authorRepository.findByNameIgnoreCase("Jane Austen")).thenReturn(Optional.empty());
        when(authorRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Author result = authorService.getAuthorByNameOrCreate("Jane Austen");

        assertEquals("Jane Austen", result.getName());
        verify(authorRepository).save(any());
    }

    @Test
    void deleteAuthor_shouldDelete_whenNoBooksAssigned() {
        when(authorRepository.findById(authorId)).thenReturn(Optional.of(author));

        assertDoesNotThrow(() -> authorService.deleteAuthor(authorId));
        verify(authorRepository).delete(author);
    }

    @Test
    void deleteAuthor_shouldThrow_whenBooksExist() {
        Book book = new Book();
        author.setBooks(List.of(book)); // ✅ doğru tip

        when(authorRepository.findById(authorId)).thenReturn(Optional.of(author));

        assertThrows(InvalidRequestException.class, () -> authorService.deleteAuthor(authorId));
    }

    @Test
    void deleteAuthor_shouldThrow_whenAuthorNotFound() {
        when(authorRepository.findById(authorId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> authorService.deleteAuthor(authorId));
    }

    @Test
    void createAuthor_shouldThrow_whenNameIsBlank() {
        assertThrows(InvalidRequestException.class, () -> authorService.createAuthor("   "));
    }

    @Test
    void createAuthor_shouldThrow_whenNameIsNull() {
        assertThrows(InvalidRequestException.class, () -> authorService.createAuthor(null));
    }

    @Test
    void getAuthorByNameOrCreate_shouldThrow_whenNameIsBlank() {
        assertThrows(InvalidRequestException.class, () -> authorService.getAuthorByNameOrCreate("   "));
    }

    @Test
    void getAuthorByNameOrCreate_shouldThrow_whenNameIsNull() {
        assertThrows(InvalidRequestException.class, () -> authorService.getAuthorByNameOrCreate(null));
    }
}
