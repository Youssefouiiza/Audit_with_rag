package com.audit.platform.modules.chat.service;

import com.audit.platform.config.AppProperties;
import com.audit.platform.modules.audit.domain.Audit;
import com.audit.platform.modules.audit.domain.AuditStatus;
import com.audit.platform.modules.audit.repository.AuditRepository;
import com.audit.platform.modules.chat.domain.*;
import com.audit.platform.modules.chat.dto.*;
import com.audit.platform.modules.chat.repository.ChatMessageRepository;
import com.audit.platform.modules.chat.repository.ChatRoomRepository;
import com.audit.platform.modules.document.service.MinioStorageService;
import com.audit.platform.modules.notification.service.NotificationService;
import com.audit.platform.modules.user.domain.User;
import com.audit.platform.modules.user.domain.UserRole;
import com.audit.platform.modules.user.repository.UserRepository;
import com.audit.platform.shared.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService Unit Tests")
class ChatServiceTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditRepository auditRepository;
    @Mock private MinioStorageService storageService;
    @Mock private NotificationService notificationService;
    @Mock private AppProperties appProperties;

    @InjectMocks
    private ChatService chatService;

    private User clientUser, auditorUser;
    private Audit audit;
    private UUID auditId, roomId;
    private ChatRoom chatRoom;

    @BeforeEach
    void setUp() {
        clientUser = User.builder().id(UUID.randomUUID()).email("c@t.com").fullName("Client").role(UserRole.CLIENT).build();
        auditorUser = User.builder().id(UUID.randomUUID()).email("a@t.com").fullName("Auditor").role(UserRole.AUDITOR).build();
        auditId = UUID.randomUUID();
        audit = Audit.builder().id(auditId).title("Test").client(clientUser).auditor(auditorUser).status(AuditStatus.IN_PROGRESS).build();
        roomId = UUID.randomUUID();
        chatRoom = ChatRoom.builder().id(roomId).roomType(RoomType.AUDIT).audit(audit).participantA(clientUser).participantB(auditorUser).createdAt(Instant.now()).build();
    }

    @Test
    @DisplayName("getOrCreateAuditRoom returns existing room")
    void getOrCreateAuditRoom_Existing() {
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));
        when(chatRoomRepository.findByAuditIdAndRoomType(auditId, RoomType.AUDIT)).thenReturn(Optional.of(chatRoom));
        ChatRoomResponse r = chatService.getOrCreateAuditRoom(auditId);
        assertNotNull(r);
        assertEquals(roomId, r.getId());
        verify(chatRoomRepository, never()).save(any());
    }

    @Test
    @DisplayName("getOrCreateAuditRoom creates new room")
    void getOrCreateAuditRoom_CreatesNew() {
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(audit));
        when(chatRoomRepository.findByAuditIdAndRoomType(auditId, RoomType.AUDIT)).thenReturn(Optional.empty());
        when(chatRoomRepository.save(any())).thenReturn(chatRoom);
        ChatRoomResponse r = chatService.getOrCreateAuditRoom(auditId);
        assertNotNull(r);
        verify(chatRoomRepository).save(any());
    }

    @Test
    @DisplayName("getOrCreateAuditRoom throws when audit not found")
    void getOrCreateAuditRoom_NotFound() {
        when(auditRepository.findById(auditId)).thenReturn(Optional.empty());
        assertThrows(ApiException.class, () -> chatService.getOrCreateAuditRoom(auditId));
    }

    @Test
    @DisplayName("saveMessage saves and notifies recipient")
    void saveMessage_Success() {
        SendChatMessageRequest req = new SendChatMessageRequest();
        req.setContent("Hi"); req.setMessageType(MessageType.TEXT);
        ChatMessage saved = ChatMessage.builder().id(UUID.randomUUID()).room(chatRoom).sender(clientUser).content("Hi").messageType(MessageType.TEXT).createdAt(Instant.now()).build();
        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(chatRoom));
        when(userRepository.findById(clientUser.getId())).thenReturn(Optional.of(clientUser));
        when(chatMessageRepository.save(any())).thenReturn(saved);
        ChatMessageResponse r = chatService.saveMessage(roomId, req, clientUser.getId());
        assertNotNull(r);
        assertEquals("Hi", r.getContent());
        verify(notificationService).push(eq(auditorUser), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("saveMessage throws when room not found")
    void saveMessage_RoomNotFound() {
        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.empty());
        assertThrows(ApiException.class, () -> chatService.saveMessage(roomId, new SendChatMessageRequest(), clientUser.getId()));
    }

    @Test
    @DisplayName("getRoomMessages returns paginated results")
    void getRoomMessages_Paginated() {
        ChatMessage msg = ChatMessage.builder().id(UUID.randomUUID()).room(chatRoom).sender(clientUser).content("msg").messageType(MessageType.TEXT).createdAt(Instant.now()).build();
        Pageable p = PageRequest.of(0, 20);
        when(chatMessageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, p)).thenReturn(new PageImpl<>(List.of(msg)));
        Page<ChatMessageResponse> result = chatService.getRoomMessages(roomId, p);
        assertEquals(1, result.getTotalElements());
    }
}
