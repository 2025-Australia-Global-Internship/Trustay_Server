package com.maritel.trustay.entity;

import com.maritel.trustay.constant.PaymentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

/**
 * 월세/공과금 등 반복 결제 자동이체 스케줄.
 * 매월 day_of_month 에 next_run_at 이 도래하면 스케줄러가 Payment(PENDING)을 자동 생성한다.
 */
@Entity
@Table(name = "TBL_AUTO_TRANSFER_SCHEDULE",
        indexes = {
                @Index(name = "idx_ats_payer", columnList = "payer_id"),
                @Index(name = "idx_ats_next_run", columnList = "active, next_run_at")
        })
@Check(constraints = "amount > 0 AND day_of_month BETWEEN 1 AND 31")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AutoTransferSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auto_transfer_schedule_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false)
    private Member payer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payee_id", nullable = false)
    private Member payee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @Column(nullable = false)
    private Long amount;

    /** 결제 종류 - 반복 결제는 RENT 또는 UTILITY 만 지원 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentType type;

    /** 매월 결제 일자 (1~31) */
    @Column(name = "day_of_month", nullable = false)
    private Integer dayOfMonth;

    @Column(name = "next_run_at", nullable = false)
    private LocalDateTime nextRunAt;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(nullable = false)
    @ColumnDefault("true")
    private Boolean active = true;

    @Column(length = 200)
    private String memo;

    @Builder
    public AutoTransferSchedule(Member payer, Member payee, Contract contract, Long amount,
                                PaymentType type, Integer dayOfMonth, LocalDateTime nextRunAt,
                                String memo) {
        this.payer = payer;
        this.payee = payee;
        this.contract = contract;
        this.amount = amount;
        this.type = type;
        this.dayOfMonth = dayOfMonth;
        this.nextRunAt = nextRunAt;
        this.active = true;
        this.memo = memo;
    }

    public void update(Long amount, Integer dayOfMonth, LocalDateTime nextRunAt, String memo, Boolean active) {
        if (amount != null) this.amount = amount;
        if (dayOfMonth != null) this.dayOfMonth = dayOfMonth;
        if (nextRunAt != null) this.nextRunAt = nextRunAt;
        if (memo != null) this.memo = memo;
        if (active != null) this.active = active;
    }

    public void cancel() {
        this.active = false;
    }

    public void markRun(LocalDateTime ranAt, LocalDateTime nextRunAt) {
        this.lastRunAt = ranAt;
        this.nextRunAt = nextRunAt;
    }
}
