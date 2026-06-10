package com.maritel.trustay.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maritel.trustay.constant.ApprovalStatus;
import com.maritel.trustay.constant.HouseType;
import com.maritel.trustay.constant.RoomType;
import com.maritel.trustay.dto.req.SharehouseReq;
import com.maritel.trustay.dto.req.SharehouseUpdateReq;
import com.maritel.trustay.dto.res.SharehouseRes;
import com.maritel.trustay.dto.res.SharehouseResultRes;
import com.maritel.trustay.dto.res.WishToggleRes;
import com.maritel.trustay.service.FileService;
import com.maritel.trustay.service.SharehouseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SharehouseController.class)
@AutoConfigureMockMvc(addFilters = false)
class SharehouseControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    SharehouseService sharehouseService;

    @MockBean
    FileService fileService;

    @MockBean
    com.maritel.trustay.config.CustomUserDetailsService customUserDetailsService;

    /** 유효한 SharehouseReq 헬퍼 (Validation 통과용 필수 필드 채움) */
    private SharehouseReq validSharehouseReq() {
        SharehouseReq req = new SharehouseReq();
        req.setTitle("테스트 쉐어하우스");
        req.setDescription("설명");
        req.setAddress("서울시 강남구");
        req.setHouseType(HouseType.APARTMENT);
        req.setRentPrice(500_000);
        req.setRoomCount(3);
        req.setBathroomCount(2);
        req.setCurrentResidents(2);
        req.setImageUrls(List.of("https://example.com/img.jpg"));
        req.setBillsIncluded(true);
        req.setRoomType(RoomType.PRIVATEROOM);
        req.setBondType(4);
        req.setMinimumStay(6);
        req.setGender("ALL");
        req.setAge("20-30");
        return req;
    }

    @Test
    @WithMockUser(username = "host@test.com")
    @DisplayName("POST /api/trustay/sharehouses/images - 이미지 업로드")
    void uploadSharehouseImages_success() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile(
                "images", "a.jpg", MediaType.IMAGE_JPEG_VALUE, "data1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile(
                "images", "b.jpg", MediaType.IMAGE_JPEG_VALUE, "data2".getBytes());

        when(fileService.uploadFile(any())).thenReturn("https://cdn/a.jpg", "https://cdn/b.jpg");

        mockMvc.perform(multipart("/api/trustay/sharehouses/images")
                        .file(file1)
                        .file(file2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser(username = "host@test.com")
    @DisplayName("POST /api/trustay/sharehouses - 매물 등록 성공")
    void registerSharehouse_success() throws Exception {
        SharehouseReq req = validSharehouseReq();

        SharehouseRes res = SharehouseRes.builder()
                .id(1L)
                .title(req.getTitle())
                .build();

        when(sharehouseService.registerSharehouse(eq("host@test.com"), any())).thenReturn(res);

        mockMvc.perform(post("/api/trustay/sharehouses")
                        .principal((Principal) () -> "host@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("테스트 쉐어하우스"));
    }

    @Test
    @DisplayName("GET /api/trustay/sharehouses - 매물 목록 조회")
    void getSharehouseList_success() throws Exception {
        when(sharehouseService.getSharehouseList(any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/trustay/sharehouses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("GET /api/trustay/sharehouses?keyword=강남 - 키워드 검색")
    void getSharehouseList_withKeyword() throws Exception {
        when(sharehouseService.getSharehouseList(any(), any())).thenReturn(new PageImpl<>(List.of(
                SharehouseRes.builder().id(1L).title("강남 매물").build()
        )));

        mockMvc.perform(get("/api/trustay/sharehouses").param("keyword", "강남"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("강남 매물"));
    }

    @Test
    @DisplayName("GET /api/trustay/sharehouses/1 - 매물 상세 조회 (조회수 증가)")
    void getSharehouseDetail_success() throws Exception {
        SharehouseResultRes res = SharehouseResultRes.builder()
                .id(1L)
                .title("테스트")
                .build();
        when(sharehouseService.getSharehouseDetail(1L)).thenReturn(res);

        mockMvc.perform(get("/api/trustay/sharehouses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("GET /api/trustay/sharehouses/my/1 - 내 매물 자세히 보기 (조회수 증가 X)")
    void getMySharehouseDetail_success() throws Exception {
        when(sharehouseService.getMySharehouseDetail(1L))
                .thenReturn(SharehouseResultRes.builder().id(1L).title("내 매물").build());

        mockMvc.perform(get("/api/trustay/sharehouses/my/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("내 매물"));
    }

    @Test
    @WithMockUser(username = "host@test.com")
    @DisplayName("GET /api/trustay/sharehouses/my - 내가 등록한 매물 목록")
    void getMySharehouses_success() throws Exception {
        when(sharehouseService.getMySharehouseList(eq("host@test.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/trustay/sharehouses/my")
                        .principal((Principal) () -> "host@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("GET /api/trustay/sharehouses/me/current - 내 현재 거주 매물")
    void getMyCurrentSharehouse_success() throws Exception {
        when(sharehouseService.getMyCurrentSharehouse("user@test.com"))
                .thenReturn(SharehouseRes.builder().id(7L).title("현재 거주").build());

        mockMvc.perform(get("/api/trustay/sharehouses/me/current")
                        .principal((Principal) () -> "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("POST /api/trustay/sharehouses/1/wish - 찜 토글")
    void toggleWish_success() throws Exception {
        when(sharehouseService.toggleWish(eq("user@test.com"), eq(1L)))
                .thenReturn(WishToggleRes.builder().sharehouseId(1L).wished(true).build());

        mockMvc.perform(post("/api/trustay/sharehouses/1/wish")
                        .principal((Principal) () -> "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sharehouseId").value(1))
                .andExpect(jsonPath("$.data.wished").value(true));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("GET /api/trustay/sharehouses/wishlist - 내 찜 목록")
    void getMyWishlist_success() throws Exception {
        when(sharehouseService.getMyWishlist(eq("user@test.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/trustay/sharehouses/wishlist")
                        .principal((Principal) () -> "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @WithMockUser(username = "host@test.com")
    @DisplayName("PUT /api/trustay/sharehouses/1 - 매물 수정")
    void updateSharehouse_success() throws Exception {
        SharehouseUpdateReq req = new SharehouseUpdateReq();
        req.setTitle("수정된 제목");
        req.setRentPrice(700_000);

        mockMvc.perform(put("/api/trustay/sharehouses/1")
                        .principal((Principal) () -> "host@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
        verify(sharehouseService).updateSharehouse(eq(1L), eq("host@test.com"), any());
    }

    @Test
    @WithMockUser(username = "host@test.com")
    @DisplayName("DELETE /api/trustay/sharehouses/1 - 매물 삭제")
    void deleteSharehouse_success() throws Exception {
        mockMvc.perform(delete("/api/trustay/sharehouses/1")
                        .principal((Principal) () -> "host@test.com"))
                .andExpect(status().isOk());
        verify(sharehouseService).deleteSharehouse(1L, "host@test.com");
    }

    @Test
    @WithMockUser(username = "admin@test.com")
    @DisplayName("PATCH /api/trustay/sharehouses/1/approval?status=ACTIVE - 매물 승인")
    void approveSharehouse_success() throws Exception {
        mockMvc.perform(patch("/api/trustay/sharehouses/1/approval")
                        .principal((Principal) () -> "admin@test.com")
                        .param("status", ApprovalStatus.ACTIVE.name()))
                .andExpect(status().isOk());
        verify(sharehouseService).approveSharehouse(eq(1L), eq(ApprovalStatus.ACTIVE), eq("admin@test.com"));
    }

    @Test
    @WithMockUser(username = "admin@test.com")
    @DisplayName("PATCH /api/trustay/sharehouses/1/approval?status=REJECTED - 매물 거절")
    void rejectSharehouse_success() throws Exception {
        mockMvc.perform(patch("/api/trustay/sharehouses/1/approval")
                        .principal((Principal) () -> "admin@test.com")
                        .param("status", ApprovalStatus.REJECTED.name()))
                .andExpect(status().isOk());
        verify(sharehouseService).approveSharehouse(eq(1L), eq(ApprovalStatus.REJECTED), eq("admin@test.com"));
    }
}
