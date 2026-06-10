package com.maritel.trustay.dto.res;

import com.maritel.trustay.constant.MessageType;
import com.maritel.trustay.entity.ChatMessage;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ChatMessageRes {
    private Long messageId;
    /** 메시지가 속한 채팅방 ID (웹소켓 재연결 시 라우팅용) */
    private Long roomId;
    /** 메시지가 속한 채팅방의 매물 ID (어떤 하우스에 대한 채팅인지 식별용) */
    private Long houseId;
    private Long senderId;
    private String senderName; // 화면에 표시할 이름
    private String message;
    private MessageType messageType;
    private LocalDateTime regTime; // BaseEntity의 생성 시간
    /** CONTRACT 메시지가 스캔 문서와 연결된 경우 */
    private Long paperContractDocumentId;

    public static ChatMessageRes of(ChatMessage entity) {
        Long docId = entity.getPaperContractDocument() != null
                ? entity.getPaperContractDocument().getId()
                : null;

        Long roomId = entity.getChatRoom() != null ? entity.getChatRoom().getId() : null;
        Long houseId = (entity.getChatRoom() != null && entity.getChatRoom().getSharehouse() != null)
                ? entity.getChatRoom().getSharehouse().getId()
                : null;

        return ChatMessageRes.builder()
                .messageId(entity.getId())
                .roomId(roomId)
                .houseId(houseId)
                .senderId(entity.getSender().getId())
                .senderName(entity.getSender().getName())
                .message(entity.getMessage())
                .messageType(entity.getMessageType())
                .regTime(entity.getRegTime()) // BaseEntity 필드 사용
                .paperContractDocumentId(docId)
                .build();
    }
}
