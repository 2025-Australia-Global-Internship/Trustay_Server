package com.maritel.trustay.repository;

import com.maritel.trustay.constant.PaymentStatus;
import com.maritel.trustay.constant.PaymentType;
import com.maritel.trustay.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(String orderId);

    List<Payment> findByMember_IdAndStatusOrderByRegTimeDesc(Long memberId, PaymentStatus status);

        @Query("""
                        SELECT p FROM Payment p
                        WHERE p.member.id = :memberId
                            AND (:fromDate IS NULL OR p.transactionDate >= :fromDate)
                            AND (:toDate IS NULL OR p.transactionDate <= :toDate)
                            AND (:paymentType IS NULL OR p.type = :paymentType)
                        ORDER BY p.transactionDate DESC
                        """)
        List<Payment> findHistory(@Param("memberId") Long memberId,
                                                            @Param("fromDate") LocalDateTime fromDate,
                                                            @Param("toDate") LocalDateTime toDate,
                                                            @Param("paymentType") PaymentType paymentType);
}
