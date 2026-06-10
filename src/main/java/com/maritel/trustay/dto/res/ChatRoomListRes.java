package com.maritel.trustay.dto.res;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatRoomListRes {
    private Long roomId;
    private Long houseId;
    private String houseTitle;     // 매물 제목
    private String otherMemberName; // 상대방 이름 (내가 보낸사람이면 받는사람 이름, 반대면 보낸사람 이름)
    private String lastMessage;     // 마지막 메시지 내용
    private String lastSenderName;  // 마지막 메시지를 보낸 사람
    private String lastMessageTime; // 마지막 메시지 전송 시간
    private String profileImageUrl;

    // TODO: 안 읽은 메시지 개수(unreadCount) 필드 추가 필요
    //   - 필요 작업: ChatMessage에 isRead boolean 컬럼 추가 OR
    //               ChatRoomParticipant(또는 새 테이블)에 lastReadAt 컬럼 추가
    //   - ChatMessageService에서 채팅방 입장 시 lastReadAt 갱신, broadcast 시 카운트 증가
    //   - ChatRoomService.getMyChatRooms()에서 (전체 메시지 수 - 내가 읽은 메시지 수) 계산해 채워주기
    //   - 프론트(house_comm_page.dart)에서 하드코딩된 unreadCount = 0 자리에 표시
    //   private Long unreadCount;

}