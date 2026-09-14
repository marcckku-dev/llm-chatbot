package com.opencode.chatbot.service;

import com.opencode.chatbot.entity.ChatMessage;
import com.opencode.chatbot.repository.ChatMessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.List;

@Service
@Slf4j
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final OllamaLLMService ollamaLLMService;

    public ChatService(ChatMessageRepository chatMessageRepository, OllamaLLMService ollamaLLMService) {
        this.chatMessageRepository = chatMessageRepository;
        this.ollamaLLMService = ollamaLLMService;
    }

    /**
     * Process user message and generate AI response
     */
    public ChatMessage processMessage(String sessionId, String userMessage) throws IOException {
        log.info("Processing chat message for session {}", sessionId);
        // Generate response from LLM
        String aiResponse = ollamaLLMService.generateResponse(userMessage);

        // Save to database
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSessionId(sessionId);
        chatMessage.setUserMessage(userMessage);
        chatMessage.setAiResponse(aiResponse);

        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        log.info("Chat message persisted with id {} for session {}", savedMessage.getId(), sessionId);
        return savedMessage;
    }

    /**
     * Retrieve chat history for a session
     */
    public List<ChatMessage> getChatHistory(String sessionId) {
        List<ChatMessage> history = chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        log.debug("Retrieved {} messages for session {}", history.size(), sessionId);
        return history;
    }

    /**
     * Check if LLM service is available
     */
    public boolean isLLMAvailable() {
        boolean available = ollamaLLMService.isOllamaAvailable();
        log.debug("LLM availability check result: {}", available);
        return available;
    }
}
