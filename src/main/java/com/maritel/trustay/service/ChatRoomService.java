package com.maritel.trustay.service;

import com.maritel.trustay.dto.req.ChatRoomCreateReq;
import com.maritel.trustay.dto.res.ChatRoomCreateRes;
import com.maritel.trustay.dto.res.ChatRoomListRes;
import com.maritel.trustay.entity.ChatMessage;
import com.maritel.trustay.entity.ChatRoom;
import com.maritel.trustay.entity.Member;
import com.maritel.trustay.entity.Sharehouse;
import com.maritel.trustay.repository.ChatMessageRepository;
import com.maritel.trustay.repository.ChatRoomRepository;
import com.maritel.trustay.repository.MemberRepository;
import com.maritel.trustay.repository.SharehouseRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;
    private final SharehouseRepository sharehouseRepository;

    // 1. 채팅방 생성 (이미 있으면 기존 방 반환). roomId 와 houseId 를 함께 반환.
    public ChatRoomCreateRes createOrGetRoom(ChatRoomCreateReq req) {
        Sharehouse house = sharehouseRepository.findById(req.getHouseId())
                .orElseThrow(() -> new EntityNotFoundException("Sharehouse not found."));

        Member sender = memberRepository.findById(req.getSenderId())
                .orElseThrow(() -> new EntityNotFoundException("User not found."));

        Member host = house.getHost(); // 매물 주인

        // 본인 매물에 본인이 채팅하는 것 방지 (선택 사항)
        if (sender.getId().equals(host.getId())) {
            throw new IllegalArgumentException("You can't start a chat on your own listing.");
        }

        // 이미 생성된 방이 있는지 확인
        Optional<ChatRoom> existingRoom = chatRoomRepository
                .findBySharehouse_IdAndSender_IdAndReceiver_Id(house.getId(), sender.getId(), host.getId());

        if (existingRoom.isPresent()) {
            return ChatRoomCreateRes.builder()
                    .roomId(existingRoom.get().getId())
                    .houseId(house.getId())
                    .build();
        }

        // 새 방 생성
        ChatRoom newRoom = ChatRoom.builder()
                .sharehouse(house)
                .sender(sender)
                .receiver(host)
                .build();

        ChatRoom saved = chatRoomRepository.save(newRoom);
        return ChatRoomCreateRes.builder()
                .roomId(saved.getId())
                .houseId(house.getId())
                .build();
    }

    // 2. 참여 중인 채팅방 목록 조회 (나가지 않은 방만, 마지막 메시지 포함)
    @Transactional(readOnly = true)
    public List<ChatRoomListRes> getMyChatRooms(Long memberId) {
        List<ChatRoom> rooms = chatRoomRepository.findActiveRoomsByMemberId(memberId);

        return rooms.stream().map(room -> {
            // 마지막 메시지 조회 (방 번호로 메시지 중 가장 최근 것 하나)
            // ChatMessageRepository에 별도 쿼리 작성이 필요할 수 있으나, 기본 List 조회 후 처리
            List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdOrderByRegTimeAsc(room.getId());
            ChatMessage lastMsg = messages.isEmpty() ? null : messages.get(messages.size() - 1);

            // 상대방 찾기
            Member other = room.getSender().getId().equals(memberId) ? room.getReceiver() : room.getSender();

            // 상대방 프로필 이미지 URL 추출 (Profile / Image가 null일 수 있어 안전하게 처리)
            String otherProfileImageUrl = null;
            if (other.getProfile() != null && other.getProfile().getProfileImage() != null) {
                otherProfileImageUrl = other.getProfile().getProfileImage().getImageUrl();
            }

            return ChatRoomListRes.builder()
                    .roomId(room.getId())
                    .houseId(room.getSharehouse().getId())
                    .houseTitle(room.getSharehouse().getTitle())
                    .otherMemberName(other.getName())
                    .lastMessage(lastMsg != null ? lastMsg.getMessage() : "No messages yet.")
                    .lastSenderName(lastMsg != null ? lastMsg.getSender().getName() : "")
                    .lastMessageTime(lastMsg != null ? lastMsg.getRegTime().toString() : "")
                    .profileImageUrl(otherProfileImageUrl)
                    .build();
        }).collect(Collectors.toList());
    }

    // 3. 채팅방 나가기
    public void leaveRoom(Long roomId, Long memberId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found."));
        if (room.getSender().getId().equals(memberId)) {
            room.leaveBySender();
        } else if (room.getReceiver().getId().equals(memberId)) {
            room.leaveByReceiver();
        } else {
            throw new IllegalArgumentException("You are not a participant in this chat room.");
        }
    }
}