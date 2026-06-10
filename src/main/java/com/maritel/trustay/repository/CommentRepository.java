package com.maritel.trustay.repository;

import com.maritel.trustay.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query(value = """
            SELECT c FROM Comment c
            JOIN FETCH c.author a
            LEFT JOIN FETCH a.profile
            WHERE c.post.id = :postId
            ORDER BY c.regTime ASC
            """,
            countQuery = "SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId")
    Page<Comment> findByPostId(@Param("postId") Long postId, Pageable pageable);

    long countByPost_Id(Long postId);

    /** 게시글 목록의 댓글 수 일괄 집계 */
    @Query("""
            SELECT c.post.id AS postId, COUNT(c) AS commentCount
            FROM Comment c
            WHERE c.post.id IN :postIds
            GROUP BY c.post.id
            """)
    List<Object[]> aggregateCountByPostIds(@Param("postIds") List<Long> postIds);

    void deleteByPost_Id(Long postId);
}
