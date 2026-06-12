package com.maritel.trustay.service;

import com.maritel.trustay.constant.MessageType;
import com.maritel.trustay.constant.NotificationType;
import com.maritel.trustay.dto.req.ChatMessageReq;
import com.maritel.trustay.dto.res.ChatMessageRes;
import com.maritel.trustay.entity.ChatMessage;
import com.maritel.trustay.entity.ChatRoom;
import com.maritel.trustay.entity.Member;
import com.maritel.trustay.entity.PaperContractDocument;
import com.maritel.trustay.repository.ChatMessageRepository;
import com.maritel.trustay.repository.ChatRoomRepository;
import com.maritel.trustay.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 이 임포트인지 확인하세요!
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional // 기본적으로 쓰기 가능하게 설정
public class ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final FileService fileService;
    private final SimpMessagingTemplate messagingTemplate;

    // 메시지 저장 (보내기)
    public ChatMessageRes saveMessage(ChatMessageReq req) {
        ChatRoom room = chatRoomRepository.findById(req.getRoomId())
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found."));

        Member sender = memberRepository.findById(req.getSenderId())
                .orElseThrow(() -> new EntityNotFoundException("User not found."));

        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .message(req.getMessage())
                .messageType(req.getMessageType())
                .build();

        chatMessageRepository.save(chatMessage);

        // 채팅방의 상대방에게 알림 (자기 자신은 제외)
        Member counterparty = room.getSender().getId().equals(sender.getId())
                ? room.getReceiver() : room.getSender();
        String preview = req.getMessage() != null && req.getMessage().length() > 50
                ? req.getMessage().substring(0, 50) + "…"
                : req.getMessage();
        notificationService.notifyExcludingSender(
                counterparty,
                sender.getId(),
                NotificationType.CHAT,
                "New message from " + sender.getName(),
                preview,
                "/chat/room/" + room.getId()
        );

        return ChatMessageRes.of(chatMessage);
    }

    /**
     * 채팅방에 이미지 메시지 전송.
     * 1) 참여자 검증 → 2) FileService로 이미지 업로드 → 3) IMAGE 메시지 저장
     * → 4) 상대방에게 알림 → 5) 구독자에게 STOMP 브로드캐스트
     */
    public ChatMessageRes sendImageMessage(Long roomId, Long senderId, MultipartFile image)
            throws BadRequestException {
        if (image == null || image.isEmpty()) {
            throw new BadRequestException("Please attach an image file.");
        }

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found."));
        Member sender = memberRepository.findById(senderId)
                .orElseThrow(() -> new EntityNotFoundException("User not found."));

        if (!room.getSender().getId().equals(senderId)
                && !room.getReceiver().getId().equals(senderId)) {
            throw new IllegalArgumentException("You are not a participant in this chat room.");
        }

        String imageUrl;
        try {
            imageUrl = fileService.uploadFile(image);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Failed to build the image URL during upload.", e);
        }
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new BadRequestException("Unsupported image format. Allowed: jpg, jpeg, png, heic, heif.");
        }

        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .message(imageUrl)
                .messageType(MessageType.IMAGE)
                .build();
        chatMessageRepository.save(chatMessage);

        Member counterparty = room.getSender().getId().equals(sender.getId())
                ? room.getReceiver() : room.getSender();
        notificationService.notifyExcludingSender(
                counterparty,
                sender.getId(),
                NotificationType.CHAT,
                "New message from " + sender.getName(),
                "Sent a photo.",
                "/chat/room/" + room.getId()
        );

        ChatMessageRes res = ChatMessageRes.of(chatMessage);
        messagingTemplate.convertAndSend("/sub/chat/room/" + room.getId(), res);
        return res;
    }

    /**
     * 종이 계약 OCR/PDF 완료 후 채팅방에 CONTRACT 메시지 전송용
     */
    public ChatMessageRes saveContractScanMessage(Long roomId, Long senderId, String pdfUrl,
                                                  PaperContractDocument paperContractDocument) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found."));
        Member sender = memberRepository.findById(senderId)
                .orElseThrow(() -> new EntityNotFoundException("User not found."));

        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .message(pdfUrl)
                .messageType(MessageType.CONTRACT)
                .paperContractDocument(paperContractDocument)
                .build();

        chatMessageRepository.save(chatMessage);
        return ChatMessageRes.of(chatMessage);
    }

    // 대화 내역 조회 및 읽음 처리 (읽기)
    public List<ChatMessageRes> getChatHistory(Long roomId, Long readerId) {
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdOrderByRegTimeAsc(roomId);

        // 읽음 처리 로직: 내가 아닌 상대방이 보낸 안 읽은 메시지들을 모두 읽음 처리
        messages.stream()
                .filter(m -> !m.getSender().getId().equals(readerId) && !m.isRead())
                .forEach(ChatMessage::markAsRead); // Dirty Checking으로 자동 업데이트

        return messages.stream()
                .map(ChatMessageRes::of)
                .collect(Collectors.toList());
    }
}