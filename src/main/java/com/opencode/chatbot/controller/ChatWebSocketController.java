package com.opencode.chatbot.controller;

import com.opencode.chatbot.dto.ChatRequest;
import com.opencode.chatbot.dto.ChatResponse;
import com.opencode.chatbot.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.io.IOException;

@Controller
@Slf4j
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Handle incoming chat messages via WebSocket
     */
    @MessageMapping("/chat")
    public void handleChatMessage(ChatRequest request) {
        try {
            log.info("Received WebSocket chat message for session {}", request.getSessionId());
            var chatMessage = chatService.processMessage(
                    request.getSessionId(),
                    request.getMessage()
            );

            ChatResponse response = new ChatResponse(
                    chatMessage.getId(),
                    chatMessage.getUserMessage(),
                    chatMessage.getAiResponse(),
                    chatMessage.getTimestamp()
            );

            // Send response back to the client
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + request.getSessionId(),
                    response
            );
        } catch (IOException e) {
            log.error("Failed to process WebSocket chat message for session {}",
                    request.getSessionId(), e);
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + request.getSessionId(),
                    new ChatResponse(null, request.getMessage(), "Error: " + e.getMessage(), null)
            );
        }
    }
}
