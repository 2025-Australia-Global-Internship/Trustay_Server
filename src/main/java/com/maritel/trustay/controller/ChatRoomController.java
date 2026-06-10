package com.maritel.trustay.controller;

import com.maritel.trustay.dto.req.ChatRoomCreateReq;
import com.maritel.trustay.dto.res.ChatMessageRes;
import com.maritel.trustay.dto.res.ChatRoomCreateRes;
import com.maritel.trustay.dto.res.ChatRoomListRes;
import com.maritel.trustay.dto.res.DataResponse;
import com.maritel.trustay.dto.res.ResponseCode;
import com.maritel.trustay.service.ChatMessageService;
import com.maritel.trustay.service.ChatRoomService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    // 채팅방 생성 (응답에 roomId 와 houseId 를 함께 내려줌)
    @Operation(summary = "Create a chat room.",
            description = "Creates or returns an existing chat room. Response contains both roomId and houseId so the client doesn't need to look up the house separately.")
    @PostMapping("/room")
    public DataResponse<ChatRoomCreateRes> createRoom(@RequestBody ChatRoomCreateReq req) {
        return DataResponse.of(chatRoomService.createOrGetRoom(req));
    }

    @Operation(summary = "Get chat message history.")
    @GetMapping("/room/{roomId}/messages/{memberId}")
    public DataResponse<List<ChatMessageRes>> getChatHistory(
            @PathVariable Long roomId,
            @PathVariable Long memberId) { // 읽는 사람의 ID를 추가로 받음
        return DataResponse.of(chatMessageService.getChatHistory(roomId, memberId));
    }

    // 나의 채팅방 목록 조회
    @Operation(summary = "List my chat rooms.")
    @GetMapping("/rooms/{memberId}")
    public DataResponse<List<ChatRoomListRes>> getRooms(@PathVariable Long memberId) {
        return DataResponse.of(chatRoomService.getMyChatRooms(memberId));
    }

    // 채팅방 나가기
    @Operation(summary = "Leave a chat room.")
    @PostMapping("/room/{roomId}/leave")
    public DataResponse<Void> leaveRoom(@PathVariable Long roomId, @RequestParam Long memberId) {
        chatRoomService.leaveRoom(roomId, memberId);
        return DataResponse.of(ResponseCode.SUCCESS);
    }
}