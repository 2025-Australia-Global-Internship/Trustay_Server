package com.maritel.trustay.service;

import com.maritel.trustay.dto.req.PostReq;
import com.maritel.trustay.dto.req.PostUpdateReq;
import com.maritel.trustay.dto.res.PostLikeToggleRes;
import com.maritel.trustay.dto.res.PostRes;
import com.maritel.trustay.entity.*;
import com.maritel.trustay.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final CommunityRepository communityRepository;
    private final SharehouseCommunityRepository sharehouseCommunityRepository;
    private final SharehouseRepository sharehouseRepository;
    private final MemberRepository memberRepository;
    private final ImageRepository imageRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;

    /**
     * 게시글 작성
     */
    @Transactional
    public PostRes createPost(String userEmail, PostReq req) {
        Member member = memberRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));

        Community community = null;
        SharehouseCommunity sharehouseCommunity = null;

        // 일반 커뮤니티 게시글
        if (req.getCommunityId() != null) {
            community = communityRepository.findById(req.getCommunityId())
                    .orElseThrow(() -> new IllegalArgumentException("Community not found."));
        }
        // 쉐어하우스 커뮤니티 게시글
        else if (req.getSharehouseId() != null) {
            Sharehouse sharehouse = sharehouseRepository.findById(req.getSharehouseId())
                    .orElseThrow(() -> new IllegalArgumentException("Sharehouse not found."));

            // 쉐어하우스 커뮤니티가 없으면 생성
            sharehouseCommunity = sharehouseCommunityRepository.findBySharehouseId(req.getSharehouseId())
                    .orElseGet(() -> {
                        SharehouseCommunity newSharehouseCommunity = SharehouseCommunity.builder()
                                .sharehouse(sharehouse)
                                .build();
                        return sharehouseCommunityRepository.save(newSharehouseCommunity);
                    });

            // 쉐어하우스 게시글은 집주인만 작성 가능
            if (!sharehouse.getHost().getId().equals(member.getId())) {
                throw new IllegalStateException("Only the host can post in a sharehouse community.");
            }
        } else {
            throw new IllegalArgumentException("Either a community ID or a sharehouse ID is required.");
        }

        Post post = Post.builder()
                .community(community)
                .sharehouseCommunity(sharehouseCommunity)
                .author(member)
                .title(req.getTitle())
                .content(req.getContent())
                .isNotice(req.getIsNotice())
                .build();

        Post savedPost = postRepository.save(post);

        // 이미지 저장
        if (req.getImageUrls() != null && !req.getImageUrls().isEmpty()) {
            List<PostImage> postImages = new ArrayList<>();
            for (int i = 0; i < req.getImageUrls().size(); i++) {
                String url = req.getImageUrls().get(i);

                // 1. 통합 이미지 테이블에 저장
                Image image = imageRepository.save(Image.builder()
                        .imageUrl(url)
                        .build());

                // 2. 연결 테이블(PostImage)에 저장
                PostImage postImage = PostImage.builder()
                        .post(post)
                        .image(image) // URL 대신 Image 객체 참조
                        .displayOrder(i)
                        .build();
                postImages.add(postImage);
            }
            postImageRepository.saveAll(postImages);
        }

        List<String> imageUrls = postImageRepository.findByPostIdOrderByDisplayOrderAsc(savedPost.getId())
                .stream()
                .map(postImage -> postImage.getImage().getImageUrl())
                .collect(Collectors.toList());

        return PostRes.from(savedPost, imageUrls);
    }

    /**
     * 게시글 상세 조회
     */
    @Transactional
    public PostRes getPostDetail(Long postId, String viewerEmailOrNull) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found."));

        // 조회수 증가
        postRepository.increaseViewCount(postId);
        post.increaseViewCount(); // 엔티티도 업데이트

        List<String> imageUrls = postImageRepository.findByPostIdOrderByDisplayOrderAsc(postId)
                .stream()
                .map(postImage -> postImage.getImage().getImageUrl())
                .collect(Collectors.toList());

        long commentCount = commentRepository.countByPost_Id(postId);
        boolean likedByMe = isLikedByEmail(postId, viewerEmailOrNull);

        return PostRes.from(post, imageUrls, commentCount, likedByMe);
    }

    /** 호환용 오버로드 (Principal 없는 호출) */
    @Transactional
    public PostRes getPostDetail(Long postId) {
        return getPostDetail(postId, null);
    }

    private boolean isLikedByEmail(Long postId, String email) {
        if (email == null) return false;
        return memberRepository.findByEmail(email)
                .map(m -> postLikeRepository.existsByPost_IdAndMember_Id(postId, m.getId()))
                .orElse(false);
    }

    /**
     * 게시글 좋아요 토글 (있으면 삭제, 없으면 생성)
     */
    @Transactional
    public PostLikeToggleRes toggleLike(String email, Long postId) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found."));

        boolean liked;
        var existing = postLikeRepository.findByPost_IdAndMember_Id(postId, member.getId());
        if (existing.isPresent()) {
            postLikeRepository.delete(existing.get());
            post.decreaseLikeCount();
            liked = false;
        } else {
            postLikeRepository.save(PostLike.builder()
                    .post(post)
                    .member(member)
                    .build());
            post.increaseLikeCount();
            liked = true;
        }
        return PostLikeToggleRes.builder()
                .postId(postId)
                .liked(liked)
                .likeCount(post.getLikeCount())
                .build();
    }

    /**
     * 일반 커뮤니티 게시글 목록 조회
     */
    public Page<PostRes> getCommunityPosts(Long communityId, Pageable pageable) {
        return mapWithCommentCount(
                postRepository.findByCommunityIdOrderByNoticeAndRegTimeDesc(communityId, pageable));
    }

    /**
     * 쉐어하우스 커뮤니티 게시글 목록 조회
     */
    public Page<PostRes> getSharehouseCommunityPosts(Long sharehouseId, Pageable pageable) {
        sharehouseRepository.findById(sharehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Sharehouse not found."));

        SharehouseCommunity sharehouseCommunity = sharehouseCommunityRepository.findBySharehouseId(sharehouseId)
                .orElse(null);

        if (sharehouseCommunity == null) {
            return Page.empty(pageable);
        }

        return mapWithCommentCount(postRepository.findBySharehouseCommunityIdOrderByNoticeAndRegTimeDesc(
                sharehouseCommunity.getId(), pageable));
    }

    /**
     * 전체 게시글 피드 (Posts for you)
     */
    public Page<PostRes> getAllPosts(Pageable pageable) {
        return mapWithCommentCount(postRepository.findAllCommunityPosts(pageable));
    }

    /**
     * 내가 작성한 게시글 목록
     */
    public Page<PostRes> getMyPosts(String userEmail, Pageable pageable) {
        Member member = memberRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));

        return mapWithCommentCount(postRepository.findByAuthorId(member.getId(), pageable));
    }

    /** 게시글 페이지 → PostRes 페이지 변환 (commentCount 일괄 집계) */
    private Page<PostRes> mapWithCommentCount(Page<Post> posts) {
        List<Long> postIds = posts.map(Post::getId).toList();
        Map<Long, Long> countMap = aggregateCommentCounts(postIds);
        return posts.map(post -> {
            List<String> imageUrls = postImageRepository.findByPostIdOrderByDisplayOrderAsc(post.getId())
                    .stream()
                    .map(postImage -> postImage.getImage().getImageUrl())
                    .collect(Collectors.toList());
            long count = countMap.getOrDefault(post.getId(), 0L);
            return PostRes.from(post, imageUrls, count, false);
        });
    }

    private Map<Long, Long> aggregateCommentCounts(List<Long> postIds) {
        if (postIds == null || postIds.isEmpty()) return Map.of();
        Map<Long, Long> result = new HashMap<>();
        for (Object[] row : commentRepository.aggregateCountByPostIds(postIds)) {
            Long pid = ((Number) row[0]).longValue();
            Long cnt = ((Number) row[1]).longValue();
            result.put(pid, cnt);
        }
        return result;
    }

    /**
     * 게시글 수정
     */
    @Transactional
    public void updatePost(String userEmail, Long postId, PostUpdateReq req) {
        Member member = memberRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found."));

        // 권한 확인 로직 (기존과 동일)
        boolean hasPermission = false;
        if (post.getCommunity() != null) {
            hasPermission = post.getAuthor().getId().equals(member.getId());
        } else if (post.getSharehouseCommunity() != null) {
            Sharehouse sharehouse = post.getSharehouseCommunity().getSharehouse();
            hasPermission = sharehouse.getHost().getId().equals(member.getId());
        }

        if (!hasPermission) {
            throw new IllegalStateException("You don't have permission to edit this post.");
        }

        // 1. 게시글 본문 수정
        post.updatePost(req.getTitle(), req.getContent(), req.getIsNotice());

        // 2. 기존 이미지 연결 삭제
        postImageRepository.deleteByPostId(postId);

        // 3. 새 이미지 저장 (수정된 부분)
        if (req.getImageUrls() != null && !req.getImageUrls().isEmpty()) {
            List<PostImage> postImages = new ArrayList<>();

            for (int i = 0; i < req.getImageUrls().size(); i++) {
                String url = req.getImageUrls().get(i);

                // [핵심] 3-1. 공통 이미지 테이블(TBL_IMAGE)에 먼저 저장
                Image imageEntity = imageRepository.save(Image.builder()
                        .imageUrl(url)
                        .build());

                // [핵심] 3-2. 연결 테이블(PostImage)에 Image 객체 전달
                PostImage postImage = PostImage.builder()
                        .post(post)
                        .image(imageEntity) // .imageUrl(url) 대신 .image(imageEntity)
                        .displayOrder(i)
                        .build();

                postImages.add(postImage);
            }
            postImageRepository.saveAll(postImages);
        }
    }

    /**
     * 게시글 삭제
     */
    @Transactional
    public void deletePost(String userEmail, Long postId) {
        Member member = memberRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found."));

        // 권한 확인
        boolean hasPermission = false;

        // 일반 커뮤니티: 작성자만 삭제 가능
        if (post.getCommunity() != null) {
            hasPermission = post.getAuthor().getId().equals(member.getId());
        }
        // 쉐어하우스 커뮤니티: 집주인만 삭제 가능
        else if (post.getSharehouseCommunity() != null) {
            Sharehouse sharehouse = post.getSharehouseCommunity().getSharehouse();
            hasPermission = sharehouse.getHost().getId().equals(member.getId());
        }

        if (!hasPermission) {
            throw new IllegalStateException("You don't have permission to delete this post.");
        }

        // 이미지 / 댓글 / 좋아요 정리
        postImageRepository.deleteByPostId(postId);
        commentRepository.deleteByPost_Id(postId);
        postLikeRepository.deleteByPost_Id(postId);

        // 게시글 삭제
        postRepository.delete(post);
    }
}
