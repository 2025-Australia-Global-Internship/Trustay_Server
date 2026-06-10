package com.maritel.trustay.repository;

import com.maritel.trustay.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /** 한 사람이 한 매물에 이미 리뷰를 남겼는지 (1인 1회 제한 검증용) */
    boolean existsByAuthor_IdAndTargetHouse_Id(Long authorId, Long targetHouseId);

    Optional<Review> findByAuthor_IdAndTargetHouse_Id(Long authorId, Long targetHouseId);

    /** 매물별 리뷰 페이징 조회 (작성자 fetch join) */
    @Query(value = """
            SELECT r FROM Review r
            JOIN FETCH r.author a
            LEFT JOIN FETCH a.profile
            WHERE r.targetHouse.id = :houseId
            ORDER BY r.regTime DESC
            """,
            countQuery = "SELECT COUNT(r) FROM Review r WHERE r.targetHouse.id = :houseId")
    Page<Review> findByTargetHouseId(@Param("houseId") Long houseId, Pageable pageable);

    /** 내가 작성한 리뷰 목록 */
    @Query(value = """
            SELECT r FROM Review r
            JOIN FETCH r.author a
            LEFT JOIN FETCH r.targetHouse h
            WHERE r.author.id = :authorId
            ORDER BY r.regTime DESC
            """,
            countQuery = "SELECT COUNT(r) FROM Review r WHERE r.author.id = :authorId")
    Page<Review> findByAuthorId(@Param("authorId") Long authorId, Pageable pageable);

    /** 매물별 평균 평점 (소수점 둘째 자리 반환은 서비스에서 처리) */
    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.targetHouse.id = :houseId")
    Double findAverageRatingByHouseId(@Param("houseId") Long houseId);

    long countByTargetHouse_Id(Long houseId);

    /** 매물 리스트 화면에서 일괄 집계용 (houseId, avgRating, reviewCount) */
    @Query("""
            SELECT r.targetHouse.id AS houseId,
                   AVG(r.rating)    AS avgRating,
                   COUNT(r)         AS reviewCount
            FROM Review r
            WHERE r.targetHouse.id IN :houseIds
            GROUP BY r.targetHouse.id
            """)
    List<Object[]> aggregateByHouseIds(@Param("houseIds") List<Long> houseIds);
}
