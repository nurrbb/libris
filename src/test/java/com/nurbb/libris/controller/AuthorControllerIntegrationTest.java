package com.nurbb.libris.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurbb.libris.model.dto.request.AuthorRequest;
import com.nurbb.libris.model.dto.request.BookRequest;
import com.nurbb.libris.model.entity.valueobject.Genre;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private AuthorRequest authorRequest;

    @BeforeEach
    void setUp() {
        authorRequest = new AuthorRequest();
        authorRequest.setName("Test Author " + UUID.randomUUID()); // ÇAKIŞMAYI ÖNLÜYORUZ
    }

    @Test
    @WithMockUser(roles = {"LIBRARIAN"})
    void shouldCreateAuthorSuccessfully() throws Exception {
        mockMvc.perform(post("/api/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authorRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(authorRequest.getName()));
    }

    @Test
    @WithMockUser(roles = {"LIBRARIAN"})
    void shouldGetAllAuthorsSuccessfully() throws Exception {
        mockMvc.perform(get("/api/authors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = {"LIBRARIAN"})
    void shouldGetAuthorByIdSuccessfully() throws Exception {
        String response = mockMvc.perform(post("/api/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authorRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String authorId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/authors/" + authorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(authorRequest.getName()));
    }

    @Test
    @WithMockUser(roles = {"LIBRARIAN"})
    void shouldDeleteAuthorSuccessfully() throws Exception {
        String response = mockMvc.perform(post("/api/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authorRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String authorId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(delete("/api/authors/" + authorId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"LIBRARIAN"})
    void shouldReturn404IfAuthorNotFound() throws Exception {
        mockMvc.perform(get("/api/authors/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"LIBRARIAN"})
    void shouldReturnBadRequestIfTryingToDeleteAuthorWithBooks() throws Exception {
        // 1. Benzersiz yazar oluştur
        String response = mockMvc.perform(post("/api/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authorRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String authorId = objectMapper.readTree(response).get("id").asText();
        String authorName = objectMapper.readTree(response).get("name").asText();

        // 2. Kitap oluştur (bu yazar adına)
        BookRequest bookRequest = new BookRequest();
        bookRequest.setTitle("Test Book");
        bookRequest.setAuthorName(authorName);
        bookRequest.setIsbn("1234567890123");
        bookRequest.setPublishedDate(LocalDate.now());
        bookRequest.setGenre(Genre.FICTION);
        bookRequest.setPageCount(100);
        bookRequest.setCount(3);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isCreated());

        // 3. Silmeye çalış (başarısız olmalı)
        mockMvc.perform(delete("/api/authors/" + authorId))
                .andExpect(status().isBadRequest());
    }
}
