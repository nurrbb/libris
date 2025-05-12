package com.nurbb.libris.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurbb.libris.model.dto.request.BorrowRequest;
import com.nurbb.libris.model.entity.*;
import com.nurbb.libris.model.entity.valueobject.Genre;
import com.nurbb.libris.model.entity.valueobject.Role;
import com.nurbb.libris.repository.AuthorRepository;
import com.nurbb.libris.repository.BookRepository;
import com.nurbb.libris.repository.UserRepository;
import com.nurbb.libris.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
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
class BorrowControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserDetailsService userDetailsService;
    @Autowired private BookRepository bookRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthorRepository authorRepository;

    private String librarianToken;
    private UUID testBookId;
    private String testPatronEmail;

    @BeforeEach
    void setup() {
        testPatronEmail = "patron_" + UUID.randomUUID() + "@libris.com";
        String librarianEmail = "librarian_" + UUID.randomUUID() + "@libris.com";
        String isbn = UUID.randomUUID().toString().substring(0, 13);

        // Patron
        User patron = new User();
        patron.setEmail(testPatronEmail);
        patron.setPassword("password");
        patron.setRole(Role.PATRON);
        patron.setFullName("Test Patron");
        patron.setScore(50);
        patron = userRepository.save(patron);

        // Librarian
        User librarian = new User();
        librarian.setEmail(librarianEmail);
        librarian.setPassword("password");
        librarian.setRole(Role.LIBRARIAN);
        librarian.setFullName("Librarian");
        userRepository.save(librarian);

        UserDetails librarianDetails = userDetailsService.loadUserByUsername(librarianEmail);
        librarianToken = "Bearer " + jwtUtil.generateToken(librarianDetails);

        // Author
        Author author = new Author();
        author.setName("Author Test");
        author = authorRepository.save(author);

        // Book
        Book book = new Book();
        book.setTitle("Test Book");
        book.setIsbn(isbn);
        book.setAuthor(author);
        book.setPageCount(300);
        book.setGenre(Genre.FANTASY);
        book.setCount(5);
        book.setAvailable(true);
        book.setPublishedDate(LocalDate.of(2020, 1, 1));
        book = bookRepository.save(book);
        testBookId = book.getId();
    }
    @Test
    void testReturnBookSuccessfully() throws Exception {
        // 1. Önce ödünç al
        BorrowRequest request = new BorrowRequest(
                testBookId,
                testPatronEmail,
                LocalDate.now(),
                LocalDate.now().plusDays(7)
        );

        String borrowResponse = mockMvc.perform(post("/api/borrows")
                        .header("Authorization", librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        UUID borrowId = UUID.fromString(objectMapper.readTree(borrowResponse).get("id").asText());

        // 2. Sonra iade et
        mockMvc.perform(put("/api/borrows/return/" + borrowId)
                        .header("Authorization", librarianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returned").value(true));
    }
    @Test
    void testReturnWithInvalidBorrowId() throws Exception {
        mockMvc.perform(put("/api/borrows/return/" + UUID.randomUUID())
                        .header("Authorization", librarianToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void testBorrowBookSuccessfully() throws Exception {
        BorrowRequest request = new BorrowRequest(
                testBookId,
                testPatronEmail,
                LocalDate.now(),
                LocalDate.now().plusDays(7)
        );

        mockMvc.perform(post("/api/borrows")
                        .header("Authorization", librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookTitle").value("Test Book"))
                .andExpect(jsonPath("$.userFullName").value("Test Patron"));
    }

    @Test
    void testBorrowWithInvalidBookId() throws Exception {
        BorrowRequest request = new BorrowRequest(
                UUID.randomUUID(), testPatronEmail,
                LocalDate.now(), LocalDate.now().plusDays(7)
        );

        mockMvc.perform(post("/api/borrows")
                        .header("Authorization", librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testBorrowWithInvalidEmail() throws Exception {
        BorrowRequest request = new BorrowRequest(
                testBookId, "notfound@mail.com",
                LocalDate.now(), LocalDate.now().plusDays(7)
        );

        mockMvc.perform(post("/api/borrows")
                        .header("Authorization", librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testBorrowSameBookTwice() throws Exception {
        BorrowRequest request = new BorrowRequest(
                testBookId, testPatronEmail,
                LocalDate.now(), LocalDate.now().plusDays(7)
        );

        // İlk ödünç alma başarılı
        mockMvc.perform(post("/api/borrows")
                        .header("Authorization", librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Aynı kitabı tekrar ödünç alma denemesi başarısız (henüz iade edilmedi)
        mockMvc.perform(post("/api/borrows")
                        .header("Authorization", librarianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetUserBorrowHistory() throws Exception {
        UUID userId = userRepository.findByEmail(testPatronEmail).orElseThrow().getId();

        mockMvc.perform(get("/api/borrows/user/" + userId)
                        .header("Authorization", librarianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetAllBorrows() throws Exception {
        mockMvc.perform(get("/api/borrows")
                        .header("Authorization", librarianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetOverdueBorrows() throws Exception {
        mockMvc.perform(get("/api/borrows/overdue")
                        .header("Authorization", librarianToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
