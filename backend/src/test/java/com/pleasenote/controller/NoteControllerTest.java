package com.pleasenote.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class NoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;

    @BeforeEach
    public void setup() throws Exception {
        // Register and get token before each test
        Map<String, String> registerRequest = Map.of(
            "username", "testuser" + System.currentTimeMillis(),
            "email", "test" + System.currentTimeMillis() + "@example.com",
            "password", "password123"
        );
        
        MvcResult result = mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        Map<String, Object> response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            Map.class
        );
        authToken = (String) response.get("accessToken");
    }

    @Test
    public void testCreateNote() throws Exception {
        Map<String, String> noteRequest = Map.of(
            "title", "Test Note",
            "content", "Test content"
        );

        mockMvc.perform(post("/notes")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(noteRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.noteId").exists())
            .andExpect(jsonPath("$.revisionId").exists())
            .andExpect(jsonPath("$.revisionNumber").value(1));
    }

    @Test
    public void testUpdateNoteCreatesRevision() throws Exception {
        // Create note
        Map<String, String> createRequest = Map.of(
            "title", "Original Title",
            "content", "Original content"
        );
        MvcResult createResult = mockMvc.perform(post("/notes")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        Map<String, Object> createResponse = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            Map.class
        );
        int noteId = (int) createResponse.get("noteId");
        int revisionId = (int) createResponse.get("revisionId");

        // Update note
        Map<String, Object> updateRequest = Map.of(
            "lastRevisionId", revisionId,
            "title", "Updated Title",
            "content", "Updated content"
        );
        mockMvc.perform(put("/notes/" + noteId)
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.revisionNumber").value(2));
    }

    @Test
    public void testOptimisticConcurrencyConflict() throws Exception {
        // Create note
        Map<String, String> createRequest = Map.of(
            "title", "Test Note",
            "content", "Original content"
        );
        MvcResult createResult = mockMvc.perform(post("/notes")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        Map<String, Object> createResponse = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            Map.class
        );
        int noteId = (int) createResponse.get("noteId");
        int revisionId = (int) createResponse.get("revisionId");

        // First update (succeeds)
        Map<String, Object> firstUpdate = Map.of(
            "lastRevisionId", revisionId,
            "content", "First update"
        );
        mockMvc.perform(put("/notes/" + noteId)
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstUpdate)))
            .andExpect(status().isOk());

        // Second update with stale revisionId (should conflict)
        Map<String, Object> secondUpdate = Map.of(
            "lastRevisionId", revisionId,  // Using old revision ID
            "content", "Second update"
        );
        mockMvc.perform(put("/notes/" + noteId)
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondUpdate)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("Conflict: note was modified by another session"));
    }

    @Test
    public void testGetRevisions() throws Exception {
        // Create note
        Map<String, String> createRequest = Map.of(
            "title", "Test Note",
            "content", "Original content"
        );
        MvcResult createResult = mockMvc.perform(post("/notes")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andReturn();

        Map<String, Object> createResponse = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            Map.class
        );
        int noteId = (int) createResponse.get("noteId");

        // Get revisions
        mockMvc.perform(get("/notes/" + noteId + "/revisions")
                .header("Authorization", "Bearer " + authToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].revisionNumber").value(1))
            .andExpect(jsonPath("$[0].content").value("Original content"));
    }

    @Test
    public void testUnauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(post("/notes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Test\",\"content\":\"Test\"}"))
            .andExpect(status().isUnauthorized());
    }
}
