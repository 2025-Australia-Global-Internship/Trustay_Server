package com.maritel.trustay.controller;

import com.maritel.trustay.dto.req.CommunityReq;
import com.maritel.trustay.dto.res.*;
import com.maritel.trustay.service.CommunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/trustay/communities")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Community API", description = "Manage communities.")
public class CommunityController {

    private final CommunityService communityService;

    @Operation(summary = "Create a community.", description = "The user creates a new community.")
    @PostMapping
    public ResponseEntity<DataResponse<CommunityRes>> createCommunity(
            Principal principal,
            @Valid @RequestBody CommunityReq req) {

        String userEmail = principal.getName();
        CommunityRes response = communityService.createCommunity(userEmail, req);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }

    @Operation(summary = "List communities.", description = "Returns the list of communities. Supports keyword search.")
    @GetMapping
    public ResponseEntity<DataResponse<PageResponse<CommunityRes>>> getCommunityList(
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "regTime", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<CommunityRes> resultPage = communityService.getCommunityList(keyword, pageable);
        PageResponse<CommunityRes> response = new PageResponse<>(resultPage);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }

    @Operation(summary = "List trending communities.", description = "Returns trending communities sorted by member count.")
    @GetMapping("/trending")
    public ResponseEntity<DataResponse<PageResponse<CommunityRes>>> getTrendingCommunities(
            @PageableDefault(size = 10) Pageable pageable) {

        Page<CommunityRes> resultPage = communityService.getTrendingCommunities(pageable);
        PageResponse<CommunityRes> response = new PageResponse<>(resultPage);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }

    @Operation(summary = "List communities I created.", description = "Returns communities created by the current user.")
    @GetMapping("/created")
    public ResponseEntity<DataResponse<List<CommunityRes>>> getMyCommunities(Principal principal) {
        String userEmail = principal.getName();
        List<CommunityRes> response = communityService.getMyCommunities(userEmail);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }

    @Operation(summary = "List communities I've joined.", description = "Returns communities the current user has joined.")
    @GetMapping("/joined")
    public ResponseEntity<DataResponse<List<CommunityRes>>> getJoinedCommunities(Principal principal) {
        String userEmail = principal.getName();
        List<CommunityRes> response = communityService.getJoinedCommunities(userEmail);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }

    @Operation(summary = "Get community details.", description = "Returns the details of a community.")
    @GetMapping("/{communityId}")
    public ResponseEntity<DataResponse<CommunityRes>> getCommunityDetail(@PathVariable Long communityId) {
        CommunityRes response = communityService.getCommunityDetail(communityId);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }

    @Operation(summary = "Join a community.", description = "Joins the specified community.")
    @PostMapping("/{communityId}/join")
    public ResponseEntity<DataResponse<Void>> joinCommunity(
            Principal principal,
            @PathVariable Long communityId) {

        String userEmail = principal.getName();
        communityService.joinCommunity(userEmail, communityId);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS));
    }

    @Operation(summary = "Leave a community.", description = "Leaves the specified community.")
    @PostMapping("/{communityId}/leave")
    public ResponseEntity<DataResponse<Void>> leaveCommunity(
            Principal principal,
            @PathVariable Long communityId) {

        String userEmail = principal.getName();
        communityService.leaveCommunity(userEmail, communityId);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS));
    }

    @Operation(summary = "Update a community.", description = "The community owner updates the community's information.")
    @PutMapping("/{communityId}")
    public ResponseEntity<DataResponse<Void>> updateCommunity(
            Principal principal,
            @PathVariable Long communityId,
            @Valid @RequestBody CommunityReq req) {

        String userEmail = principal.getName();
        communityService.updateCommunity(userEmail, communityId, req);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS));
    }

    @Operation(summary = "Delete a community.", description = "The community owner deletes the community.")
    @DeleteMapping("/{communityId}")
    public ResponseEntity<DataResponse<Void>> deleteCommunity(
            Principal principal,
            @PathVariable Long communityId) {

        String userEmail = principal.getName();
        communityService.deleteCommunity(userEmail, communityId);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS));
    }
}
