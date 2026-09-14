package com.opencode.chatbot.controller;

import com.opencode.chatbot.dto.ChatRequest;
import com.opencode.chatbot.dto.ChatResponse;
import com.opencode.chatbot.entity.ChatMessage;
import com.opencode.chatbot.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Create a new chat session
     */
    @PostMapping("/session/new")
    public ResponseEntity<String> createSession() {
        String sessionId = UUID.randomUUID().toString();
        log.info("Created new chat session {}", sessionId);
        return ResponseEntity.ok(sessionId);
    }

    /**
     * Send a message and get response
     */
    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(@RequestBody ChatRequest request) {
        try {
            log.info("Received REST chat message for session {}", request.getSessionId());
            ChatMessage chatMessage = chatService.processMessage(
                    request.getSessionId(),
                    request.getMessage()
            );

            ChatResponse response = new ChatResponse(
                    chatMessage.getId(),
                    chatMessage.getUserMessage(),
                    chatMessage.getAiResponse(),
                    chatMessage.getTimestamp()
            );

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Failed to process REST chat message for session {}", request.getSessionId(), e);
            return ResponseEntity.status(500).body("Error processing message: " + e.getMessage());
        }
    }

    /**
     * Get chat history
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable String sessionId) {
        log.debug("Retrieving chat history for session {}", sessionId);
        return ResponseEntity.ok(chatService.getChatHistory(sessionId));
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        boolean isAvailable = chatService.isLLMAvailable();
        log.info("Health check completed; LLM available: {}", isAvailable);
        if (isAvailable) {
            return ResponseEntity.ok("LLM service is available");
        } else {
            return ResponseEntity.status(503).body("LLM service is unavailable");
        }
    }
}
