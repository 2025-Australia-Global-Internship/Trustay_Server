package com.maritel.trustay.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.maritel.trustay.client.TossPaymentsApiException;
import com.maritel.trustay.client.TossPaymentsClient;
import com.maritel.trustay.config.TossPaymentsProperties;
import com.maritel.trustay.constant.NotificationType;
import com.maritel.trustay.constant.PaymentStatus;
import com.maritel.trustay.constant.PaymentType;
import com.maritel.trustay.dto.req.DutchPayCreateReq;
import com.maritel.trustay.dto.req.PaymentConfirmReq;
import com.maritel.trustay.dto.req.RentPaymentPrepareReq;
import com.maritel.trustay.dto.res.*;
import com.maritel.trustay.entity.Contract;
import com.maritel.trustay.entity.DutchPayGroup;
import com.maritel.trustay.entity.Member;
import com.maritel.trustay.entity.Payment;
import com.maritel.trustay.entity.Profile;
import com.maritel.trustay.repository.ContractRepository;
import com.maritel.trustay.repository.DutchPayGroupRepository;
import com.maritel.trustay.repository.MemberRepository;
import com.maritel.trustay.repository.PaymentRepository;
import com.maritel.trustay.util.AmountSplitUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    public static final String SETTLEMENT_GUIDE_RENT =
            "Toss only processes test payments. For actual rent, please transfer directly to the host's account shown.";
    public static final String SETTLEMENT_GUIDE_DUTCH =
            "Toss only processes test payments. For the actual split amount, please transfer directly to the payee's account shown.";

    private static final String ACCOUNT_NOT_SET = "(No account registered) Please add a settlement account to your profile.";

    private final MemberRepository memberRepository;
    private final ContractRepository contractRepository;
    private final DutchPayGroupRepository dutchPayGroupRepository;
    private final PaymentRepository paymentRepository;
    private final TossPaymentsClient tossPaymentsClient;
    private final TossPaymentsProperties tossPaymentsProperties;
    private final NotificationService notificationService;

    public TossClientConfigRes getTossClientConfig() {
        if (!StringUtils.hasText(tossPaymentsProperties.getClientKey())) {
            throw new IllegalStateException("toss.payments.client-key is not configured.");
        }
        return TossClientConfigRes.builder()
                .clientKey(tossPaymentsProperties.getClientKey())
                .build();
    }

    @Transactional
    public PaymentPrepareRes prepareRentPayment(String memberEmail, RentPaymentPrepareReq req) {
        Member tenant = memberRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        Contract contract = contractRepository.findByIdForPayment(req.getContractId())
                .orElseThrow(() -> new IllegalArgumentException("Contract not found."));
        if (!contract.getTenant().getId().equals(tenant.getId())) {
            throw new IllegalArgumentException("Only the tenant on this contract can prepare a rent payment.");
        }
        Profile landlordProfile = contract.getLandlord().getProfile();
        String targetAccount = resolveAccountDisplay(landlordProfile);

        String orderId = "TSY-R-" + UUID.randomUUID().toString().replace("-", "");
        Payment payment = Payment.builder()
                .member(tenant)
                .amount(req.getAmount())
                .targetAccount(targetAccount)
                .type(PaymentType.RENT)
                .contract(contract)
                .orderId(orderId)
                .dutchPayGroup(null)
                .build();
        paymentRepository.save(payment);

        return PaymentPrepareRes.builder()
                .paymentId(payment.getId())
                .orderId(orderId)
                .amount(req.getAmount())
                .paymentType(PaymentType.RENT)
                .targetAccount(targetAccount)
                .settlementGuide(SETTLEMENT_GUIDE_RENT)
                .build();
    }

    @Transactional
    public DutchPayCreateRes createDutchPay(String creatorEmail, DutchPayCreateReq req) {
        Member creator = memberRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));

        List<Long> memberIds = req.getMemberIds();
        if (memberIds.size() < 2) {
            throw new IllegalArgumentException("A split payment requires at least 2 participants.");
        }
        if (new HashSet<>(memberIds).size() != memberIds.size()) {
            throw new IllegalArgumentException("memberIds contains duplicates.");
        }
        if (!memberIds.contains(req.getPayeeMemberId())) {
            throw new IllegalArgumentException("payeeMemberId must be included in memberIds.");
        }

        List<Member> members = memberRepository.findAllById(new HashSet<>(memberIds));
        if (members.size() != memberIds.size()) {
            throw new IllegalArgumentException("One or more member IDs do not exist.");
        }
        Map<Long, Member> byId = members.stream().collect(Collectors.toMap(Member::getId, m -> m));

        Member payee = byId.get(req.getPayeeMemberId());
        String targetAccount = resolveAccountDisplay(payee.getProfile());

        Contract contract = null;
        if (req.getContractId() != null) {
            contract = contractRepository.findById(req.getContractId())
                    .orElseThrow(() -> new IllegalArgumentException("Contract not found."));
        }

        DutchPayGroup group = DutchPayGroup.builder()
                .createdBy(creator)
                .payee(payee)
                .contract(contract)
                .title(req.getTitle())
                .totalAmount(req.getTotalAmount())
                .build();
        dutchPayGroupRepository.save(group);

        long[] shares = AmountSplitUtils.splitEvenly(req.getTotalAmount(), memberIds.size());
        List<DutchPaySplitItemRes> splits = new ArrayList<>();

        for (int i = 0; i < memberIds.size(); i++) {
            Long mid = memberIds.get(i);
            Member member = byId.get(mid);
            String orderId = "TSY-D" + group.getId() + "-" + mid + "-" + UUID.randomUUID().toString().substring(0, 8);
            Payment payment = Payment.builder()
                    .member(member)
                    .amount(shares[i])
                    .targetAccount(targetAccount)
                    .type(PaymentType.DUTCH)
                    .contract(contract)
                    .orderId(orderId)
                    .dutchPayGroup(group)
                    .build();
            paymentRepository.save(payment);
            splits.add(DutchPaySplitItemRes.builder()
                    .paymentId(payment.getId())
                    .memberId(mid)
                    .amount(shares[i])
                    .orderId(orderId)
                    .targetAccount(targetAccount)
                    .build());
        }

        return DutchPayCreateRes.builder()
                .dutchPayGroupId(group.getId())
                .splits(splits)
                .settlementGuide(SETTLEMENT_GUIDE_DUTCH)
                .build();
    }

    @Transactional
    public PaymentConfirmRes confirmPayment(String memberEmail, PaymentConfirmReq req) {
        Member member = memberRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        Payment payment = paymentRepository.findByOrderId(req.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found."));
        if (!payment.getMember().getId().equals(member.getId())) {
            throw new IllegalArgumentException("You can only approve your own payments.");
        }
        if (!payment.getAmount().equals(req.getAmount())) {
            throw new IllegalArgumentException("Payment amount doesn't match.");
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalArgumentException("This payment has already been processed.");
        }

        try {
            JsonNode tossBody = tossPaymentsClient.confirmPayment(
                    req.getPaymentKey(), req.getOrderId(), req.getAmount());
            payment.confirmToss(req.getPaymentKey());
            String tossStatus = tossBody != null && tossBody.has("status")
                    ? tossBody.get("status").asText()
                    : null;

            publishPaymentConfirmedNotifications(payment);

            return PaymentConfirmRes.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .status(payment.getStatus())
                    .paymentType(payment.getType())
                    .tossPaymentStatus(tossStatus)
                    .build();
        } catch (TossPaymentsApiException e) {
            // 승인 실패 시 트랜잭션 롤백 → 결제 건은 PENDING 유지(재시도 가능)
            throw e;
        }
    }

    /**
     * 결제 완료 시 송금자와 수취인 모두에게 알림 발행.
     * - RENT: 송금자=세입자, 수취인=집주인(Contract.landlord)
     * - DUTCH: 송금자=납부자, 수취인=정산 담당자(DutchPayGroup.payee)
     * - 그 외 타입: 송금자에게만 결제 완료 알림
     */
    private void publishPaymentConfirmedNotifications(Payment payment) {
        Member payer = payment.getMember();
        String amountText = String.format("%,d KRW", payment.getAmount());

        notificationService.notify(
                payer,
                NotificationType.PAYMENT,
                "Payment complete",
                String.format("Your %s payment of %s has been approved.", payment.getType().name(), amountText),
                "/payments/" + payment.getId()
        );

        Member payee = null;
        if (payment.getType() == PaymentType.RENT && payment.getContract() != null) {
            payee = payment.getContract().getLandlord();
        } else if (payment.getType() == PaymentType.DUTCH && payment.getDutchPayGroup() != null) {
            payee = payment.getDutchPayGroup().getPayee();
        }
        if (payee != null && !payee.getId().equals(payer.getId())) {
            notificationService.notify(
                    payee,
                    NotificationType.PAYMENT,
                    "Payment received",
                    String.format("%s paid %s.", payer.getName(), amountText),
                    "/payments/" + payment.getId()
            );
        }
    }

    public List<PendingPaymentRes> listMyPendingPayments(String memberEmail) {
        Member member = memberRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        return paymentRepository.findByMember_IdAndStatusOrderByRegTimeDesc(member.getId(), PaymentStatus.PENDING)
                .stream()
                .map(p -> PendingPaymentRes.builder()
                        .paymentId(p.getId())
                        .orderId(p.getOrderId())
                        .amount(p.getAmount())
                        .paymentType(p.getType())
                        .targetAccount(p.getTargetAccount())
                        .dutchPayGroupId(p.getDutchPayGroup() != null ? p.getDutchPayGroup().getId() : null)
                        .build())
                .toList();
    }

    public List<PaymentHistoryRes> getMyPaymentHistory(String memberEmail, LocalDate from, LocalDate to, PaymentType type) {
        Member member = memberRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));

        LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDateTime = to != null ? to.atTime(LocalTime.MAX) : null;

        return paymentRepository.findHistory(member.getId(), fromDateTime, toDateTime, type)
                .stream()
                .map(PaymentHistoryRes::from)
                .toList();
    }

    private static String resolveAccountDisplay(Profile profile) {
        if (profile != null && StringUtils.hasText(profile.getAccountInfo())) {
            return profile.getAccountInfo().trim();
        }
        return ACCOUNT_NOT_SET;
    }
}
