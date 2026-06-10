package com.maritel.trustay.repository;

import com.maritel.trustay.entity.SharehouseRecentSearch;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SharehouseRecentSearchRepository extends JpaRepository<SharehouseRecentSearch, Long> {

    Optional<SharehouseRecentSearch> findByMember_IdAndKeyword(Long memberId, String keyword);

    List<SharehouseRecentSearch> findByMember_IdOrderBySearchedAtDesc(Long memberId, Pageable pageable);

    Optional<SharehouseRecentSearch> findByIdAndMember_Id(Long id, Long memberId);

    @Modifying
    @Query("DELETE FROM SharehouseRecentSearch s WHERE s.member.id = :memberId")
    int deleteAllByMemberId(@Param("memberId") Long memberId);
}
