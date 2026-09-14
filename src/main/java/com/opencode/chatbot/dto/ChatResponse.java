package com.opencode.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private Long id;
    private String userMessage;
    private String aiResponse;
    private LocalDateTime timestamp;
}
