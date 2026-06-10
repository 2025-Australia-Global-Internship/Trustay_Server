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
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private AutoTransferService autoTransferService;

    private PaymentController paymentController;
    private final Principal principal = () -> "user@example.com";

    @BeforeEach
    void setUp() {
        paymentController = new PaymentController(paymentService, autoTransferService);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Toss / 결제 준비 / 승인
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /toss/client-config - 토스 클라이언트 키 조회 성공")
    void tossClientConfig_returnsSuccess() {
        when(paymentService.getTossClientConfig())
                .thenReturn(TossClientConfigRes.builder().clientKey("test_ck").build());

        ResponseEntity<DataResponse<TossClientConfigRes>> response = paymentController.tossClientConfig();

        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());
        assertEquals("test_ck", response.getBody().getData().getClientKey());
    }

    @Test
    @DisplayName("GET /toss/client-config - 키 미설정 시 4000 코드")
    void tossClientConfig_whenKeyMissing_returnsNotValid() {
        when(paymentService.getTossClientConfig())
                .thenThrow(new IllegalStateException("토스 클라이언트 키가 설정되지 않았습니다."));

        ResponseEntity<DataResponse<TossClientConfigRes>> response = paymentController.tossClientConfig();

        assertNotNull(response.getBody());
        assertEquals(4000, response.getBody().getCode());
    }

    @Test
    @DisplayName("POST /rent/prepare - 월세 결제 준비 성공")
    void prepareRent_returnsSuccess() {
        RentPaymentPrepareReq req = new RentPaymentPrepareReq();
        req.setContractId(1L);
        req.setAmount(10_000L);

        when(paymentService.prepareRentPayment("user@example.com", req))
                .thenReturn(PaymentPrepareRes.builder()
                        .paymentId(1L)
                        .orderId("order-1")
                        .amount(10_000L)
                        .build());

        ResponseEntity<DataResponse<PaymentPrepareRes>> response =
                paymentController.prepareRent(principal, req);

        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());
        assertEquals("order-1", response.getBody().getData().getOrderId());
    }

    @Test
    @DisplayName("POST /rent/prepare - 잘못된 요청은 4000 코드")
    void prepareRent_invalidArg_returnsNotValid() {
        RentPaymentPrepareReq req = new RentPaymentPrepareReq();
        req.setContractId(999L);
        req.setAmount(100L);

        when(paymentService.prepareRentPayment("user@example.com", req))
                .thenThrow(new IllegalArgumentException("계약을 찾을 수 없습니다."));

        ResponseEntity<DataResponse<PaymentPrepareRes>> response =
                paymentController.prepareRent(principal, req);

        assertNotNull(response.getBody());
        assertEquals(4000, response.getBody().getCode());
    }

    @Test
    @DisplayName("POST /dutch - 더치페이 생성 성공")
    void createDutch_returnsSuccess() {
        DutchPayCreateReq req = new DutchPayCreateReq();
        req.setTotalAmount(3_000L);
        req.setMemberIds(List.of(1L, 2L, 3L));
        req.setPayeeMemberId(1L);

        when(paymentService.createDutchPay("user@example.com", req))
                .thenReturn(DutchPayCreateRes.builder().dutchPayGroupId(1L).build());

        ResponseEntity<DataResponse<DutchPayCreateRes>> response =
                paymentController.createDutch(principal, req);

        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());
        assertEquals(1L, response.getBody().getData().getDutchPayGroupId());
    }

    @Test
    @DisplayName("POST /confirm - 토스 승인 성공")
    void confirm_returnsSuccess() {
        PaymentConfirmReq req = new PaymentConfirmReq();
        req.setPaymentKey("pk");
        req.setOrderId("order-1");
        req.setAmount(1_000L);

        when(paymentService.confirmPayment("user@example.com", req))
                .thenReturn(PaymentConfirmRes.builder().paymentId(1L).orderId("order-1").build());

        ResponseEntity<DataResponse<PaymentConfirmRes>> response =
                paymentController.confirm(principal, req);

        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());
        assertEquals("order-1", response.getBody().getData().getOrderId());
    }

    @Test
    @DisplayName("POST /confirm - Toss API 예외 발생 시 5001 코드")
    void confirm_whenTossError_returnsTossFailedCode() {
        PaymentConfirmReq req = new PaymentConfirmReq();
        req.setPaymentKey("pk");
        req.setOrderId("order-1");
        req.setAmount(1_000L);

        when(paymentService.confirmPayment("user@example.com", req))
                .thenThrow(new TossPaymentsApiException(400, "bad request"));

        ResponseEntity<DataResponse<PaymentConfirmRes>> response =
                paymentController.confirm(principal, req);

        assertNotNull(response.getBody());
        assertEquals(5001, response.getBody().getCode());
    }

    // ─────────────────────────────────────────────────────────────────────
    // 내 결제 내역 / 미완료
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /me/pending - 미완료 결제 목록")
    void myPending_returnsSuccess() {
        when(paymentService.listMyPendingPayments("user@example.com"))
                .thenReturn(List.of(
                        PendingPaymentRes.builder().paymentId(1L).orderId("order-1").amount(1_000L).build(),
                        PendingPaymentRes.builder().paymentId(2L).orderId("order-2").amount(2_000L).build()
                ));

        ResponseEntity<DataResponse<List<PendingPaymentRes>>> response =
                paymentController.myPending(principal);

        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());
        assertEquals(2, response.getBody().getData().size());
    }

    @Test
    @DisplayName("GET /me/history - 내 결제 이력 (기간/타입 필터)")
    void myHistory_returnsSuccess() {
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);

        when(paymentService.getMyPaymentHistory(eq("user@example.com"), eq(from), eq(to), eq(PaymentType.RENT)))
                .thenReturn(List.of(
                        PaymentHistoryRes.builder().paymentId(1L).build()
                ));

        ResponseEntity<DataResponse<List<PaymentHistoryRes>>> response =
                paymentController.myHistory(principal, from, to, PaymentType.RENT);

        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());
        assertEquals(1, response.getBody().getData().size());
    }

    // ─────────────────────────────────────────────────────────────────────
    // 자동이체 스케줄
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auto-transfer - 자동이체 등록 성공")
    void createAutoTransfer_success() {
        AutoTransferReq req = new AutoTransferReq();
        req.setPayeeMemberId(10L);
        req.setAmount(500_000L);
        req.setType(PaymentType.RENT);
        req.setDayOfMonth(5);

        when(autoTransferService.create("user@example.com", req))
                .thenReturn(AutoTransferRes.builder().id(1L).amount(500_000L).build());

        ResponseEntity<DataResponse<AutoTransferRes>> response =
                paymentController.createAutoTransfer(principal, req);

        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());
        assertEquals(1L, response.getBody().getData().getId());
    }

    @Test
    @DisplayName("GET /auto-transfer/me - 내 자동이체 목록")
    void listMyAutoTransfers_success() {
        when(autoTransferService.listMine("user@example.com"))
                .thenReturn(List.of(AutoTransferRes.builder().id(1L).build()));

        ResponseEntity<DataResponse<List<AutoTransferRes>>> response =
                paymentController.listMyAutoTransfers(principal);

        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    @DisplayName("PUT /auto-transfer/{id} - 자동이체 수정 성공")
    void updateAutoTransfer_success() {
        AutoTransferUpdateReq req = new AutoTransferUpdateReq();
        req.setAmount(600_000L);

        when(autoTransferService.update("user@example.com", 1L, req))
                .thenReturn(AutoTransferRes.builder().id(1L).amount(600_000L).build());

        ResponseEntity<DataResponse<AutoTransferRes>> response =
                paymentController.updateAutoTransfer(principal, 1L, req);

        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());
        assertEquals(600_000L, response.getBody().getData().getAmount());
    }

    @Test
    @DisplayName("PUT /auto-transfer/{id} - 존재하지 않는 스케줄이면 NOT_FOUND_AUTO_TRANSFER(4048)")
    void updateAutoTransfer_notFound() {
        AutoTransferUpdateReq req = new AutoTransferUpdateReq();
        req.setAmount(100L);

        when(autoTransferService.update(eq("user@example.com"), eq(99L), any()))
                .thenThrow(new EntityNotFoundException("not found"));

        ResponseEntity<DataResponse<AutoTransferRes>> response =
                paymentController.updateAutoTransfer(principal, 99L, req);

        assertNotNull(response.getBody());
        assertEquals(ResponseCode.NOT_FOUND_AUTO_TRANSFER.getCode(), response.getBody().getCode());
    }

    @Test
    @DisplayName("PUT /auto-transfer/{id} - 본인 스케줄이 아니면 4030 forbidden")
    void updateAutoTransfer_forbidden() {
        AutoTransferUpdateReq req = new AutoTransferUpdateReq();
        when(autoTransferService.update(eq("user@example.com"), eq(1L), any()))
                .thenThrow(new IllegalStateException("본인의 자동이체 스케줄만 처리할 수 있습니다."));

        ResponseEntity<DataResponse<AutoTransferRes>> response =
                paymentController.updateAutoTransfer(principal, 1L, req);

        assertNotNull(response.getBody());
        assertEquals(4030, response.getBody().getCode());
    }

    @Test
    @DisplayName("DELETE /auto-transfer/{id} - 자동이체 취소 성공")
    void cancelAutoTransfer_success() {
        ResponseEntity<DataResponse<Void>> response =
                paymentController.cancelAutoTransfer(principal, 1L);

        assertNotNull(response.getBody());
        assertEquals(200, response.getBody().getCode());
        verify(autoTransferService).cancel("user@example.com", 1L);
    }

    @Test
    @DisplayName("DELETE /auto-transfer/{id} - 없는 스케줄이면 NOT_FOUND_AUTO_TRANSFER")
    void cancelAutoTransfer_notFound() {
        doThrow(new EntityNotFoundException("not found"))
                .when(autoTransferService).cancel("user@example.com", 99L);

        ResponseEntity<DataResponse<Void>> response =
                paymentController.cancelAutoTransfer(principal, 99L);

        assertNotNull(response.getBody());
        assertEquals(ResponseCode.NOT_FOUND_AUTO_TRANSFER.getCode(), response.getBody().getCode());
    }
}
