package com.maritel.trustay.repository;

import com.maritel.trustay.entity.SharehouseRecentView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SharehouseRecentViewRepository extends JpaRepository<SharehouseRecentView, Long> {

    Optional<SharehouseRecentView> findByMember_IdAndSharehouse_Id(Long memberId, Long sharehouseId);

    List<SharehouseRecentView> findByMember_IdOrderByViewedAtDesc(Long memberId, Pageable pageable);
}
