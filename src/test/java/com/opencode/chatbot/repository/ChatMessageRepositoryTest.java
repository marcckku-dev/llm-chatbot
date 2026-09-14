package com.opencode.chatbot.repository;

import com.opencode.chatbot.entity.ChatMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
class ChatMessageRepositoryTest {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Test
    void saveAssignsIdAndTimestamp() {
        ChatMessage message = message("session-1", "Hello", LocalDateTime.now());

        ChatMessage savedMessage = chatMessageRepository.saveAndFlush(message);

        assertNotNull(savedMessage.getId());
        assertNotNull(savedMessage.getTimestamp());
    }

    @Test
    void findBySessionReturnsMessagesInTimestampOrder() {
        ChatMessage older = message("session-1", "First", LocalDateTime.of(2026, 1, 1, 10, 0));
        ChatMessage newer = message("session-1", "Second", LocalDateTime.of(2026, 1, 2, 10, 0));
        ChatMessage otherSession = message("session-2", "Other", LocalDateTime.of(2026, 1, 1, 9, 0));
        chatMessageRepository.save(older);
        chatMessageRepository.save(newer);
        chatMessageRepository.save(otherSession);

        List<ChatMessage> history =
                chatMessageRepository.findBySessionIdOrderByTimestampAsc("session-1");

        assertEquals(List.of("First", "Second"),
                history.stream().map(ChatMessage::getUserMessage).toList());
    }

    private ChatMessage message(String sessionId, String userMessage, LocalDateTime timestamp) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setUserMessage(userMessage);
        message.setAiResponse("Response");
        message.setTimestamp(timestamp);
        return message;
    }
}
