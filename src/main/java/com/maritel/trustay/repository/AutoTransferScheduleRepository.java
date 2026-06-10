package com.maritel.trustay.repository;

import com.maritel.trustay.entity.AutoTransferSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AutoTransferScheduleRepository extends JpaRepository<AutoTransferSchedule, Long> {

    @Query("""
            SELECT s FROM AutoTransferSchedule s
            JOIN FETCH s.payer
            JOIN FETCH s.payee py
            LEFT JOIN FETCH py.profile
            LEFT JOIN FETCH s.contract
            WHERE s.payer.email = :email
            ORDER BY s.regTime DESC
            """)
    List<AutoTransferSchedule> findByPayerEmail(@Param("email") String email);

    @Query("""
            SELECT s FROM AutoTransferSchedule s
            JOIN FETCH s.payer
            JOIN FETCH s.payee py
            LEFT JOIN FETCH py.profile
            LEFT JOIN FETCH s.contract
            WHERE s.active = true AND s.nextRunAt <= :now
            """)
    List<AutoTransferSchedule> findDueSchedules(@Param("now") LocalDateTime now);
}
