package com.maritel.trustay.repository;

import com.maritel.trustay.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByPost_IdAndMember_Id(Long postId, Long memberId);

    boolean existsByPost_IdAndMember_Id(Long postId, Long memberId);

    void deleteByPost_Id(Long postId);
}
