package com.nurbb.libris.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nurbb.libris.model.dto.request.UserRequest;
import com.nurbb.libris.model.entity.valueobject.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private UserRequest userRequest;

    @BeforeEach
    void setUp() {
        userRequest = new UserRequest();
        userRequest.setFullName("Test Kullanıcı");
        userRequest.setEmail("test" + UUID.randomUUID() + "@mail.com");
        userRequest.setPassword("Test1234!");
        userRequest.setPhone("+905321234567");
        userRequest.setRole(Role.PATRON);
    }

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated()) // Kullanıcı kaydının başarılı olduğunu belirtmek için '201 Created'
                .andExpect(jsonPath("$.email").value(userRequest.getEmail()))
                .andExpect(jsonPath("$.fullName").value("Test Kullanıcı"));
    }
    @Test
    @WithMockUser(roles = {"LIBRARIAN"})
    void shouldReturn404WhenGettingStatsOfNonExistentUser() throws Exception {
        mockMvc.perform(get("/api/users/" + UUID.randomUUID() + "/stats"))
                .andExpect(status().isNotFound());
    }
    @Test
    @WithMockUser(roles = {"LIBRARIAN"})
    void shouldReturn404WhenGettingNonExistentUserById() throws Exception {
        mockMvc.perform(get("/api/users/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
    @Test
    @WithMockUser(username = "nopermission@mail.com", roles = {})
    void shouldReturn403WhenNoRoleAccessingUserById() throws Exception {
        // Kullanıcıyı oluştur
        userRequest.setEmail("nopermission@mail.com");
        String response = mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String userId = objectMapper.readTree(response).get("id").asText();

        // Bu kullanıcıda hiçbir yetki yok → 403 dönmeli
        mockMvc.perform(get("/api/users/" + userId))
                .andExpect(status().isForbidden());
    }
    @Test
    @WithMockUser(username = "patron2@mail.com", roles = {"PATRON"})
    void shouldAllowPatronToAccessOwnStats() throws Exception {
        // Kullanıcıyı kaydet
        userRequest.setEmail("patron2@mail.com");
        String response = mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String userId = objectMapper.readTree(response).get("id").asText();

        // Kendi istatistiklerine erişmesine izin verilmeli
        mockMvc.perform(get("/api/users/" + userId + "/stats"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"LIBRARIAN"}) // Librarian rolü ile test
    void shouldGetUserByIdAfterRegistration() throws Exception {
        String response = mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated()) // 'Created' yanıtı alacağımızı belirtelim
                .andReturn().getResponse().getContentAsString();

        String userId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(userRequest.getEmail()));
    }

    @Test
    @WithMockUser(roles = {"LIBRARIAN"}) // Librarian rolü ile test
    void shouldUpdateUserSuccessfully() throws Exception {
        String response = mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated()) // 'Created' yanıtı alacağımızı belirtelim
                .andReturn().getResponse().getContentAsString();

        String userId = objectMapper.readTree(response).get("id").asText();

        // Güncellenmiş kullanıcı verisi
        userRequest.setFullName("Updated Kullanıcı");
        userRequest.setPhone("+905321234568");

        mockMvc.perform(put("/api/users/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Kullanıcı"))
                .andExpect(jsonPath("$.phone").value("+905321234568"));
    }
    @Test
    @WithMockUser(roles = {"LIBRARIAN"})
    void shouldGetAllUsersSuccessfully() throws Exception {
        // En az bir kullanıcı kaydı ekle
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated());

        // Tüm kullanıcıları çek
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].email").value(org.hamcrest.Matchers.hasItem(userRequest.getEmail())));
    }

    @Test
    @WithMockUser(roles = {"LIBRARIAN"}) // Librarian rolü ile test
    void shouldDeleteUserSuccessfully() throws Exception {
        String response = mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated()) // 'Created' yanıtı alacağımızı belirtelim
                .andReturn().getResponse().getContentAsString();

        String userId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(delete("/api/users/" + userId))
                .andExpect(status().isNoContent()); // 204 No Content
    }

    @Test
    @WithMockUser(username = "self@mail.com", roles = {"PATRON"})
    void shouldAllowPatronToAccessOwnData() throws Exception {
        // Kullanıcıyı "self@mail.com" olarak kaydet
        userRequest.setEmail("self@mail.com");
        String response = mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String userId = objectMapper.readTree(response).get("id").asText();

        // Kendi verisini çekmesine izin verilmeli
        mockMvc.perform(get("/api/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("self@mail.com"));
    }

    @Test
    @WithMockUser(username = "patron1@mail.com", roles = {"PATRON"})
    void shouldReturn403WhenPatronTriesToAccessAnotherUserById() throws Exception {
        // 1. patron1'i kayıt et (test kullanıcısı zaten bu maille geliyor)
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated());

        // 2. patron2 oluştur
        UserRequest otherUser = new UserRequest();
        otherUser.setEmail("otheruser@mail.com");
        otherUser.setFullName("Diğer Kullanıcı");
        otherUser.setPassword("Test1234!");
        otherUser.setPhone("+905321234569");
        otherUser.setRole(Role.PATRON);

        String response = mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherUser)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String otherUserId = objectMapper.readTree(response).get("id").asText();

        // 3. patron1, patron2'nin bilgilerine erişmeye çalışıyor → 403
        mockMvc.perform(get("/api/users/" + otherUserId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "patron1@mail.com", roles = {"PATRON"})
    void shouldReturn403WhenPatronTriesToAccessAnotherUserStats() throws Exception {
        // Aynı kullanıcıları oluştur

        UserRequest otherUser = new UserRequest();
        otherUser.setEmail("otheruser_" + UUID.randomUUID() + "@mail.com");
        otherUser.setFullName("Diğer Kullanıcı");
        otherUser.setPassword("Test1234!");
        otherUser.setPhone("+905321234569");
        otherUser.setRole(Role.PATRON);

        String response = mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherUser)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String otherUserId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/users/" + otherUserId + "/stats"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"LIBRARIAN"}) // Librarian rolü ile test
    void shouldGetUserStatisticsSuccessfully() throws Exception {
        String response = mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated()) // 'Created' yanıtı alacağımızı belirtelim
                .andReturn().getResponse().getContentAsString();

        String userId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/users/" + userId + "/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReadPages").value(0))  // totalReadPages alanını kontrol et
                .andExpect(jsonPath("$.totalReadingDays").value(0))  // Başlangıçta 0 okuma günü
                .andExpect(jsonPath("$.avgPagesPerDay").value(0.0)) // Ortalama günlük okunan sayfa sayısı
                .andExpect(jsonPath("$.avgReturnDuration").value(0.0)); // Ortalama iade süresi
    }

}
