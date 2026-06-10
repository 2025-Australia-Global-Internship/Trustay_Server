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
@Tag(name = "Payment API", description = "Toss test payments, rent preparation, split payments (dutch pay), and auto-transfers.")
public class PaymentController {

    private final PaymentService paymentService;
    private final AutoTransferService autoTransferService;

    @Operation(summary = "Get the Toss payment widget client key.")
    @GetMapping("/toss/client-config")
    public ResponseEntity<DataResponse<TossClientConfigRes>> tossClientConfig() {
        try {
            TossClientConfigRes res = paymentService.getTossClientConfig();
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, res));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(DataResponse.of(ResponseCode.NOT_VALID.getCode(), e.getMessage(), null));
        }
    }

    @Operation(summary = "Prepare a rent payment (issues an orderId and returns the host's account info).")
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

    @Operation(summary = "Create a split payment (distributes amount and orderId per participant).")
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

    @Operation(summary = "Confirm a Toss payment (test only when using test keys).")
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

    @Operation(summary = "List my pending payments.")
    @GetMapping("/me/pending")
    public ResponseEntity<DataResponse<List<PendingPaymentRes>>> myPending(Principal principal) {
        try {
            List<PendingPaymentRes> list = paymentService.listMyPendingPayments(principal.getName());
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, list));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get my payment history.")
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

    @Operation(summary = "Create an auto-transfer schedule.",
            description = "A pending payment is auto-created and a notification is sent on the configured day each month. The user still needs to approve the actual payment.")
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

    @Operation(summary = "List my auto-transfer schedules.")
    @GetMapping("/auto-transfer/me")
    public ResponseEntity<DataResponse<List<AutoTransferRes>>> listMyAutoTransfers(Principal principal) {
        try {
            List<AutoTransferRes> list = autoTransferService.listMine(principal.getName());
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, list));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Update an auto-transfer schedule.")
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

    @Operation(summary = "Cancel an auto-transfer schedule (sets active=false).")
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
