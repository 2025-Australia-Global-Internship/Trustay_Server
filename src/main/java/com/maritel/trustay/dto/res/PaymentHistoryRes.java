package com.maritel.trustay.dto.res;

import com.maritel.trustay.constant.PaymentStatus;
import com.maritel.trustay.constant.PaymentType;
import com.maritel.trustay.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class PaymentHistoryRes {
    private Long paymentId;
    private String orderId;
    private Long amount;
    private PaymentType paymentType;
    private PaymentStatus status;
    private String targetAccount;
    private LocalDateTime transactionDate;
    private Boolean autoTransfer;
    private Long contractId;
    private Long dutchPayGroupId;

    public static PaymentHistoryRes from(Payment payment) {
        return PaymentHistoryRes.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .paymentType(payment.getType())
                .status(payment.getStatus())
                .targetAccount(payment.getTargetAccount())
                .transactionDate(payment.getTransactionDate())
                .autoTransfer(payment.isAutoTransfer())
                .contractId(payment.getContract() != null ? payment.getContract().getId() : null)
                .dutchPayGroupId(payment.getDutchPayGroup() != null ? payment.getDutchPayGroup().getId() : null)
                .build();
    }
}