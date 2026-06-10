package com.maritel.trustay.dto.res;

import lombok.Builder;
import lombok.Getter;

/**
 * 채팅방 생성/조회(idempotent) API 응답.
 * 프론트가 어떤 매물에 대한 방인지 즉시 알 수 있도록 roomId 와 함께 houseId 도 같이 내려준다.
 */
@Getter
@Builder
public class ChatRoomCreateRes {
    private Long roomId;
    private Long houseId;
}
