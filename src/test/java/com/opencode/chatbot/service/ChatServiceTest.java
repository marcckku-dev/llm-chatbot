package com.opencode.chatbot.service;

import com.opencode.chatbot.entity.ChatMessage;
import com.opencode.chatbot.repository.ChatMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private OllamaLLMService ollamaLLMService;

    @InjectMocks
    private ChatService chatService;

    @Test
    void processMessageGeneratesAndPersistsChatMessage() throws IOException {
        ChatMessage savedMessage = new ChatMessage();
        savedMessage.setId(1L);
        savedMessage.setSessionId("session-1");
        savedMessage.setUserMessage("Hello");
        savedMessage.setAiResponse("Hi there");

        when(ollamaLLMService.generateResponse("Hello")).thenReturn("Hi there");
        when(chatMessageRepository.save(org.mockito.ArgumentMatchers.any(ChatMessage.class)))
                .thenReturn(savedMessage);

        ChatMessage result = chatService.processMessage("session-1", "Hello");

        assertSame(savedMessage, result);
        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(messageCaptor.capture());
        ChatMessage persistedMessage = messageCaptor.getValue();
        assertEquals("session-1", persistedMessage.getSessionId());
        assertEquals("Hello", persistedMessage.getUserMessage());
        assertEquals("Hi there", persistedMessage.getAiResponse());
    }

    @Test
    void processMessagePropagatesLlmErrorWithoutPersisting() throws IOException {
        IOException failure = new IOException("Ollama unavailable");
        when(ollamaLLMService.generateResponse("Hello")).thenThrow(failure);

        IOException thrown = assertThrows(IOException.class,
                () -> chatService.processMessage("session-1", "Hello"));

        assertSame(failure, thrown);
        verify(chatMessageRepository, never()).save(org.mockito.ArgumentMatchers.any(ChatMessage.class));
    }

    @Test
    void getChatHistoryReturnsMessagesForSession() {
        List<ChatMessage> history = List.of(new ChatMessage());
        when(chatMessageRepository.findBySessionIdOrderByTimestampAsc("session-1")).thenReturn(history);

        assertSame(history, chatService.getChatHistory("session-1"));
        verify(chatMessageRepository).findBySessionIdOrderByTimestampAsc("session-1");
    }

    @Test
    void isLlmAvailableDelegatesToLlmService() {
        when(ollamaLLMService.isOllamaAvailable()).thenReturn(true);

        assertEquals(true, chatService.isLLMAvailable());
        verify(ollamaLLMService).isOllamaAvailable();
    }
}
