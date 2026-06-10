package com.maritel.trustay.service;

import com.maritel.trustay.dto.req.CommentReq;
import com.maritel.trustay.dto.req.CommentUpdateReq;
import com.maritel.trustay.dto.res.CommentRes;
import com.maritel.trustay.entity.Comment;
import com.maritel.trustay.entity.Member;
import com.maritel.trustay.entity.Post;
import com.maritel.trustay.repository.CommentRepository;
import com.maritel.trustay.repository.MemberRepository;
import com.maritel.trustay.repository.PostRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public CommentRes create(String email, Long postId, CommentReq req) {
        Member author = findMember(email);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        Comment saved = commentRepository.save(Comment.builder()
                .post(post)
                .author(author)
                .content(req.getContent())
                .build());
        return CommentRes.from(saved);
    }

    public Page<CommentRes> list(Long postId, Pageable pageable) {
        if (!postRepository.existsById(postId)) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }
        return commentRepository.findByPostId(postId, pageable).map(CommentRes::from);
    }

    @Transactional
    public CommentRes update(String email, Long postId, Long commentId, CommentUpdateReq req) {
        Member me = findMember(email);
        Comment comment = findCommentInPost(postId, commentId);

        if (!comment.getAuthor().getId().equals(me.getId())) {
            throw new IllegalStateException("본인이 작성한 댓글만 수정할 수 있습니다.");
        }
        if (Boolean.TRUE.equals(comment.getIsDeleted())) {
            throw new IllegalStateException("삭제된 댓글은 수정할 수 없습니다.");
        }
        comment.updateContent(req.getContent());
        return CommentRes.from(comment);
    }

    @Transactional
    public void delete(String email, Long postId, Long commentId) {
        Member me = findMember(email);
        Comment comment = findCommentInPost(postId, commentId);
        if (!comment.getAuthor().getId().equals(me.getId())) {
            throw new IllegalStateException("본인이 작성한 댓글만 삭제할 수 있습니다.");
        }
        // soft delete (목록 표시용 placeholder 유지)
        comment.softDelete();
    }

    private Comment findCommentInPost(Long postId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("댓글을 찾을 수 없습니다."));
        if (!comment.getPost().getId().equals(postId)) {
            throw new IllegalArgumentException("게시글과 댓글이 일치하지 않습니다.");
        }
        return comment;
    }

    private Member findMember(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
    }
}
