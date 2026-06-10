package com.maritel.trustay.dto.res;

import com.maritel.trustay.constant.PaymentType;
import com.maritel.trustay.entity.AutoTransferSchedule;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AutoTransferRes {
    private Long id;
    private Long payerId;
    private Long payeeId;
    private String payeeName;
    private Long contractId;
    private Long amount;
    private PaymentType type;
    private Integer dayOfMonth;
    private LocalDateTime nextRunAt;
    private LocalDateTime lastRunAt;
    private Boolean active;
    private String memo;
    private LocalDateTime regTime;

    public static AutoTransferRes from(AutoTransferSchedule s) {
        return AutoTransferRes.builder()
                .id(s.getId())
                .payerId(s.getPayer().getId())
                .payeeId(s.getPayee().getId())
                .payeeName(s.getPayee().getName())
                .contractId(s.getContract() != null ? s.getContract().getId() : null)
                .amount(s.getAmount())
                .type(s.getType())
                .dayOfMonth(s.getDayOfMonth())
                .nextRunAt(s.getNextRunAt())
                .lastRunAt(s.getLastRunAt())
                .active(s.getActive())
                .memo(s.getMemo())
                .regTime(s.getRegTime())
                .build();
    }
}
