package com.maritel.trustay.controller;

import com.maritel.trustay.client.TossPaymentsApiException;
import com.maritel.trustay.constant.PaymentType;
import com.maritel.trustay.dto.req.AutoTransferReq;
import com.maritel.trustay.dto.req.AutoTransferUpdateReq;
import com.maritel.trustay.dto.req.DutchPayCreateReq;
import com.maritel.trustay.dto.req.PaymentConfirmReq;
import com.maritel.trustay.dto.req.RentPaymentPrepareReq;
import com.maritel.trustay.dto.res.*;
import com.maritel.trustay.service.AutoTransferService;
import com.maritel.trustay.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/trustay/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment API", description = "토스 테스트 결제, 월세 준비, N빵(더치페이), 자동이체")
public class PaymentController {

    private final PaymentService paymentService;
    private final AutoTransferService autoTransferService;

    @Operation(summary = "토스 결제위젯용 클라이언트 키 조회")
    @GetMapping("/toss/client-config")
    public ResponseEntity<DataResponse<TossClientConfigRes>> tossClientConfig() {
        try {
            TossClientConfigRes res = paymentService.getTossClientConfig();
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, res));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(DataResponse.of(ResponseCode.NOT_VALID.getCode(), e.getMessage(), null));
        }
    }

    @Operation(summary = "월세 결제 준비 (orderId 발급, 집주인 계좌 안내)")
    @PostMapping("/rent/prepare")
    public ResponseEntity<DataResponse<PaymentPrepareRes>> prepareRent(
            Principal principal,
            @Valid @RequestBody RentPaymentPrepareReq req) {
        try {
            PaymentPrepareRes res = paymentService.prepareRentPayment(principal.getName(), req);
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, res));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "N빵 생성 (참여자별 금액·orderId 분배)")
    @PostMapping("/dutch")
    public ResponseEntity<DataResponse<DutchPayCreateRes>> createDutch(
            Principal principal,
            @Valid @RequestBody DutchPayCreateReq req) {
        try {
            DutchPayCreateRes res = paymentService.createDutchPay(principal.getName(), req);
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, res));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "토스 결제 승인 (테스트 키면 테스트만)")
    @PostMapping("/confirm")
    public ResponseEntity<DataResponse<PaymentConfirmRes>> confirm(
            Principal principal,
            @Valid @RequestBody PaymentConfirmReq req) {
        try {
            PaymentConfirmRes res = paymentService.confirmPayment(principal.getName(), req);
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, res));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (TossPaymentsApiException e) {
            log.warn("Toss confirm failed: {}", e.getResponseBody());
            return ResponseEntity.ok(DataResponse.of(ResponseCode.TOSS_PAYMENT_FAILED.getCode(),
                    ResponseCode.TOSS_PAYMENT_FAILED.getMessage() + " " + e.getResponseBody(), null));
        }
    }

    @Operation(summary = "내 미완료(PENDING) 결제 목록")
    @GetMapping("/me/pending")
    public ResponseEntity<DataResponse<List<PendingPaymentRes>>> myPending(Principal principal) {
        try {
            List<PendingPaymentRes> list = paymentService.listMyPendingPayments(principal.getName());
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, list));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "내 결제 이력 조회")
    @GetMapping("/me/history")
    public ResponseEntity<DataResponse<List<PaymentHistoryRes>>> myHistory(
            Principal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) PaymentType type) {
        try {
            List<PaymentHistoryRes> list = paymentService.getMyPaymentHistory(principal.getName(), from, to, type);
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, list));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    private static <T> ResponseEntity<DataResponse<T>> badRequest(String message) {
        return ResponseEntity.ok(DataResponse.of(ResponseCode.NOT_VALID.getCode(), message, null));
    }

    private static <T> ResponseEntity<DataResponse<T>> forbidden(String message) {
        return ResponseEntity.ok(DataResponse.of(ResponseCode.FORBIDDEN.getCode(), message, null));
    }

    private static <T> ResponseEntity<DataResponse<T>> error(ResponseCode code) {
        return ResponseEntity.ok(DataResponse.of(code.getCode(), code.getMessage(), null));
    }

    // =========================================================================
    // 자동이체 스케줄 관리
    // =========================================================================

    @Operation(summary = "자동이체 등록",
            description = "매월 dayOfMonth 일에 Payment(PENDING)이 자동 생성되고 알림이 발송됩니다. 실제 결제 승인은 사용자가 진행해야 합니다.")
    @PostMapping("/auto-transfer")
    public ResponseEntity<DataResponse<AutoTransferRes>> createAutoTransfer(
            Principal principal,
            @Valid @RequestBody AutoTransferReq req) {
        try {
            AutoTransferRes res = autoTransferService.create(principal.getName(), req);
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, res));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (IllegalStateException e) {
            return forbidden(e.getMessage());
        }
    }

    @Operation(summary = "내 자동이체 스케줄 목록")
    @GetMapping("/auto-transfer/me")
    public ResponseEntity<DataResponse<List<AutoTransferRes>>> listMyAutoTransfers(Principal principal) {
        try {
            List<AutoTransferRes> list = autoTransferService.listMine(principal.getName());
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, list));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "자동이체 스케줄 수정")
    @PutMapping("/auto-transfer/{id}")
    public ResponseEntity<DataResponse<AutoTransferRes>> updateAutoTransfer(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody AutoTransferUpdateReq req) {
        try {
            AutoTransferRes res = autoTransferService.update(principal.getName(), id, req);
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, res));
        } catch (EntityNotFoundException e) {
            return error(ResponseCode.NOT_FOUND_AUTO_TRANSFER);
        } catch (IllegalStateException e) {
            return forbidden(e.getMessage());
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "자동이체 스케줄 취소 (active=false)")
    @DeleteMapping("/auto-transfer/{id}")
    public ResponseEntity<DataResponse<Void>> cancelAutoTransfer(
            Principal principal,
            @PathVariable Long id) {
        try {
            autoTransferService.cancel(principal.getName(), id);
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS));
        } catch (EntityNotFoundException e) {
            return error(ResponseCode.NOT_FOUND_AUTO_TRANSFER);
        } catch (IllegalStateException e) {
            return forbidden(e.getMessage());
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }
}
