package com.audit.platform.modules.chat.controller;

import com.audit.platform.config.SecurityUserDetails;
import com.audit.platform.modules.chat.dto.ChatMessageResponse;
import com.audit.platform.modules.chat.dto.ChatRoomResponse;
import com.audit.platform.modules.chat.dto.SendChatMessageRequest;
import com.audit.platform.modules.chat.service.ChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatController Unit Tests")
class ChatControllerTest {

    @Mock private ChatService chatService;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @InjectMocks private ChatController controller;

    private Authentication mockAuth() {
        UUID userId = UUID.randomUUID();
        SecurityUserDetails details = mock(SecurityUserDetails.class);
        when(details.getId()).thenReturn(userId);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(details);
        return auth;
    }

    @Test
    @DisplayName("getAuditRoom returns room")
    void getAuditRoom() {
        UUID id = UUID.randomUUID();
        ChatRoomResponse resp = ChatRoomResponse.builder().id(UUID.randomUUID()).build();
        when(chatService.getOrCreateAuditRoom(id)).thenReturn(resp);

        ResponseEntity<ChatRoomResponse> r = controller.getAuditRoom(id);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals(resp.getId(), r.getBody().getId());
    }

    @Test
    @DisplayName("getMessages returns page")
    void getMessages() {
        UUID id = UUID.randomUUID();
        when(chatService.getRoomMessages(eq(id), any())).thenReturn(new PageImpl<>(List.of()));

        ResponseEntity<Page<ChatMessageResponse>> r = controller.getMessages(id, Pageable.unpaged());
        assertEquals(HttpStatus.OK, r.getStatusCode());
    }

    @Test
    @DisplayName("sendMessageRest saves and broadcasts message")
    void sendMessageRest() {
        UUID roomId = UUID.randomUUID();
        Authentication auth = mockAuth();
        UUID userId = ((SecurityUserDetails) auth.getPrincipal()).getId();
        SendChatMessageRequest req = new SendChatMessageRequest();
        ChatMessageResponse resp = ChatMessageResponse.builder().id(UUID.randomUUID()).build();

        when(chatService.saveMessage(roomId, req, userId)).thenReturn(resp);

        ResponseEntity<ChatMessageResponse> r = controller.sendMessageRest(roomId, req, auth);

        assertEquals(HttpStatus.CREATED, r.getStatusCode());
        assertEquals(resp.getId(), r.getBody().getId());
        verify(messagingTemplate).convertAndSend("/topic/room." + roomId, resp);
    }

    @Test
    @DisplayName("sendMessageRest handles broadcast failure gracefully")
    void sendMessageRest_BroadcastFails() {
        UUID roomId = UUID.randomUUID();
        Authentication auth = mockAuth();
        UUID userId = ((SecurityUserDetails) auth.getPrincipal()).getId();
        SendChatMessageRequest req = new SendChatMessageRequest();
        ChatMessageResponse resp = ChatMessageResponse.builder().id(UUID.randomUUID()).build();

        when(chatService.saveMessage(roomId, req, userId)).thenReturn(resp);
        doThrow(new RuntimeException("STOMP error")).when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        ResponseEntity<ChatMessageResponse> r = controller.sendMessageRest(roomId, req, auth);

        assertEquals(HttpStatus.CREATED, r.getStatusCode()); // Still succeeds
    }

    @Test
    @DisplayName("sendMessageWs saves and broadcasts message")
    void sendMessageWs() {
        UUID roomId = UUID.randomUUID();
        Authentication auth = mockAuth();
        UUID userId = ((SecurityUserDetails) auth.getPrincipal()).getId();
        SendChatMessageRequest req = new SendChatMessageRequest();
        ChatMessageResponse resp = ChatMessageResponse.builder().id(UUID.randomUUID()).build();

        when(chatService.saveMessage(roomId, req, userId)).thenReturn(resp);

        controller.sendMessageWs(roomId, req, auth);

        verify(messagingTemplate).convertAndSend("/topic/room." + roomId, resp);
    }
}
