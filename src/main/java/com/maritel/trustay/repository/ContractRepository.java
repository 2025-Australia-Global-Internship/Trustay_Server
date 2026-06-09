package com.maritel.trustay.repository;

import com.maritel.trustay.constant.ContractStatus;
import com.maritel.trustay.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    @Query("""
            SELECT DISTINCT c FROM Contract c
            JOIN FETCH c.landlord l
            LEFT JOIN FETCH l.profile
            JOIN FETCH c.tenant t
            WHERE c.id = :id
            """)
    Optional<Contract> findByIdForPayment(@Param("id") Long id);

        @Query("""
            SELECT c FROM Contract c
            JOIN FETCH c.sharehouse s
            WHERE c.tenant.email = :tenantEmail
              AND c.status = :status
            ORDER BY c.regTime DESC
            """)
        List<Contract> findByTenantEmailAndStatusOrderByRegTimeDesc(@Param("tenantEmail") String tenantEmail,
                                    @Param("status") ContractStatus status);
}
