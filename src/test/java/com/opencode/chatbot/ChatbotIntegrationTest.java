package com.opencode.chatbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencode.chatbot.dto.ChatRequest;
import com.opencode.chatbot.service.OllamaLLMService;
import com.opencode.chatbot.controller.ChatWebSocketController;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = ChatbotIntegrationTest.TestApplication.class,
        properties = "spring.devtools.restart.enabled=false"
)
@AutoConfigureMockMvc
class ChatbotIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OllamaLLMService ollamaLLMService;

    @Test
    void sendMessagePersistsAndReturnsMessageInChatHistory() throws Exception {
        when(ollamaLLMService.generateResponse("Hello")).thenReturn("Hi there");

        mockMvc.perform(post("/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChatRequest("integration-session", "Hello"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userMessage", is("Hello")))
                .andExpect(jsonPath("$.aiResponse", is("Hi there")))
                .andExpect(jsonPath("$.id").isNumber());

        mockMvc.perform(get("/chat/history/integration-session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sessionId", is("integration-session")))
                .andExpect(jsonPath("$[0].userMessage", is("Hello")))
                .andExpect(jsonPath("$[0].aiResponse", is("Hi there")));
    }

    @Test
    void sendMessageReturnsServerErrorWhenLlmFails() throws Exception {
        when(ollamaLLMService.generateResponse("Hello"))
                .thenThrow(new IOException("Ollama unavailable"));

        mockMvc.perform(post("/chat/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChatRequest("failed-session", "Hello"))))
                .andExpect(status().isInternalServerError());

        mockMvc.perform(get("/chat/history/failed-session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Configuration
    @EnableAutoConfiguration
    @ComponentScan(
            basePackages = {
                    "com.opencode.chatbot.controller",
            "com.opencode.chatbot.service",
            "com.opencode.chatbot.repository"
            },
            excludeFilters = @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = ChatWebSocketController.class
            )
    )
    static class TestApplication {
    }
}
