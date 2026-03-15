package com.expensetracker.controller;

import java.util.Map;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.expensetracker.service.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Controller tests for AuthController.
 * Covers: AC-01 through AC-04 (TDD §4.3)
 * Requirements: FR-AUTH-01, FR-AUTH-03, FR-AUTH-04
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    // AC-01
    @Test
    void login_WithValidCredentials_Returns200AndToken() throws Exception {
        when(authenticationService.login("alice", "pass")).thenReturn("jwt-token-xyz");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "alice", "password", "pass"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-xyz"));
    }

    // AC-02
    @Test
    void login_WithInvalidCredentials_Returns401() throws Exception {
        when(authenticationService.login(anyString(), anyString()))
                .thenThrow(new RuntimeException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "alice", "password", "wrong"))))
                .andExpect(status().isUnauthorized());
    }

    // AC-03
    @Test
    void register_WithNewUser_Returns200() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "newuser", "password", "pass"))))
                .andExpect(status().isOk());
    }

    // AC-04
    @Test
    void register_WithDuplicateUsername_Returns400() throws Exception {
        when(authenticationService.register(anyString(), anyString()))
                .thenThrow(new RuntimeException("Username already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "alice", "password", "pass"))))
                .andExpect(status().isBadRequest());
    }
}
