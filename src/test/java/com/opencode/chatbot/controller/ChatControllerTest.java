package com.opencode.chatbot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencode.chatbot.dto.ChatRequest;
import com.opencode.chatbot.entity.ChatMessage;
import com.opencode.chatbot.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    @Test
    void createSessionReturnsUuid() throws Exception {
        mockMvc.perform(post("/chat/session/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(matchesPattern("[0-9a-f-]{36}")));
    }

    @Test
    void sendMessageReturnsChatResponse() throws Exception {
        ChatMessage message = chatMessage(7L, "Hello", "Hi there");
        when(chatService.processMessage("session-1", "Hello")).thenReturn(message);

        mockMvc.perform(post("/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChatRequest("session-1", "Hello"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(7)))
                .andExpect(jsonPath("$.userMessage", is("Hello")))
                .andExpect(jsonPath("$.aiResponse", is("Hi there")));

        verify(chatService).processMessage("session-1", "Hello");
    }

    @Test
    void sendMessageReturnsInternalServerErrorWhenProcessingFails() throws Exception {
        when(chatService.processMessage(eq("session-1"), eq("Hello")))
                .thenThrow(new IOException("Ollama unavailable"));

        mockMvc.perform(post("/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChatRequest("session-1", "Hello"))))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error processing message: Ollama unavailable"));
    }

    @Test
    void getChatHistoryReturnsSessionMessages() throws Exception {
        when(chatService.getChatHistory("session-1"))
                .thenReturn(List.of(chatMessage(1L, "Hello", "Hi there")));

        mockMvc.perform(get("/chat/history/session-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].sessionId", is("session-1")));
    }

    @Test
    void healthCheckReflectsLlmAvailability() throws Exception {
        when(chatService.isLLMAvailable()).thenReturn(false);

        mockMvc.perform(get("/chat/health"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("LLM service is unavailable"));
    }

    private ChatMessage chatMessage(Long id, String userMessage, String aiResponse) {
        ChatMessage message = new ChatMessage();
        message.setId(id);
        message.setSessionId("session-1");
        message.setUserMessage(userMessage);
        message.setAiResponse(aiResponse);
        message.setTimestamp(LocalDateTime.of(2026, 1, 1, 12, 0));
        return message;
    }
}
