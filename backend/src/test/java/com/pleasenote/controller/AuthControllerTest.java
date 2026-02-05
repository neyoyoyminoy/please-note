package com.pleasenote.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testRegisterSuccess() throws Exception {
        Map<String, String> registerRequest = Map.of(
            "username", "newuser",
            "email", "new@example.com",
            "password", "password123"
        );

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    public void testRegisterDuplicateUsername() throws Exception {
        // First registration
        Map<String, String> firstRequest = Map.of(
            "username", "duplicate",
            "email", "first@example.com",
            "password", "password123"
        );
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)))
            .andExpect(status().isCreated());

        // Second registration with same username
        Map<String, String> secondRequest = Map.of(
            "username", "duplicate",
            "email", "second@example.com",
            "password", "password123"
        );
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondRequest)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("Username already taken"));
    }

    @Test
    public void testLoginSuccess() throws Exception {
        // Register a user first
        Map<String, String> registerRequest = Map.of(
            "username", "loginuser",
            "email", "login@example.com",
            "password", "password123"
        );
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated());

        // Now login
        Map<String, String> loginRequest = Map.of(
            "username", "loginuser",
            "password", "password123"
        );
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    public void testLoginInvalidCredentials() throws Exception {
        Map<String, String> loginRequest = Map.of(
            "username", "nonexistent",
            "password", "wrongpassword"
        );
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }
}
