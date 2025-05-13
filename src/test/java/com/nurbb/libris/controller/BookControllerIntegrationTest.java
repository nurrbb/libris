package com.nurbb.libris.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurbb.libris.model.dto.request.BookRequest;
import com.nurbb.libris.model.entity.valueobject.Genre;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)

class BookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private BookRequest bookRequest;

    @BeforeEach
    void setUp() {
        bookRequest = new BookRequest();
        bookRequest.setTitle("Test Kitabı");
        bookRequest.setAuthorName("Yazar Test");
        bookRequest.setIsbn(UUID.randomUUID().toString());
        bookRequest.setPublishedDate(LocalDate.of(2020, 5, 10));
        bookRequest.setGenre(Genre.FICTION);
        bookRequest.setPageCount(300);
        bookRequest.setCount(5);
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN") // → Yetkili kullanıcı simülasyonu
    void shouldAddBookAndReturnCreated() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Kitabı"))
                .andExpect(jsonPath("$.authorName").value("Yazar Test"));
    }
    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void shouldGetBookById() throws Exception {
        String response = mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String bookId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/books/" + bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Kitabı"));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void shouldUpdateBook() throws Exception {
        String created = mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookRequest)))
                .andReturn().getResponse().getContentAsString();

        String bookId = objectMapper.readTree(created).get("id").asText();

        bookRequest.setTitle("Güncellenmiş Kitap");
        bookRequest.setPageCount(123);

        mockMvc.perform(put("/api/books/" + bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Güncellenmiş Kitap"))
                .andExpect(jsonPath("$.pageCount").value(123));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void shouldDeleteBook() throws Exception {
        String created = mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookRequest)))
                .andReturn().getResponse().getContentAsString();

        String bookId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(delete("/api/books/" + bookId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN") // → Yetkili kullanıcı simülasyonu
    void shouldGetBooksAfterAddingOne() throws Exception {
        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
