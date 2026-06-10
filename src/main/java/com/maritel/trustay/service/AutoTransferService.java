package com.maritel.trustay.service;

import com.maritel.trustay.constant.NotificationType;
import com.maritel.trustay.constant.PaymentType;
import com.maritel.trustay.dto.req.AutoTransferReq;
import com.maritel.trustay.dto.req.AutoTransferUpdateReq;
import com.maritel.trustay.dto.res.AutoTransferRes;
import com.maritel.trustay.entity.AutoTransferSchedule;
import com.maritel.trustay.entity.Contract;
import com.maritel.trustay.entity.Member;
import com.maritel.trustay.entity.Payment;
import com.maritel.trustay.entity.Profile;
import com.maritel.trustay.repository.AutoTransferScheduleRepository;
import com.maritel.trustay.repository.ContractRepository;
import com.maritel.trustay.repository.MemberRepository;
import com.maritel.trustay.repository.PaymentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * 월세/공과금 자동이체 스케줄 도메인.
 * 매일 0시 (AutoTransferScheduler 참고) 도래한 스케줄을 실행해
 * Payment(PENDING) 을 자동 생성하고 송금자/수취인에게 알림을 발행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AutoTransferService {

    private static final String ACCOUNT_NOT_SET = "(계좌 미등록) 프로필에 정산 계좌를 입력해 주세요.";

    private final AutoTransferScheduleRepository scheduleRepository;
    private final MemberRepository memberRepository;
    private final ContractRepository contractRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;

    // ────────────────────────────────────────────────────────────────────────
    // CRUD
    // ────────────────────────────────────────────────────────────────────────

    @Transactional
    public AutoTransferRes create(String payerEmail, AutoTransferReq req) {
        if (req.getType() != PaymentType.RENT && req.getType() != PaymentType.UTILITY) {
            throw new IllegalArgumentException("자동이체는 RENT 또는 UTILITY 타입만 지원합니다.");
        }
        Member payer = findMember(payerEmail);
        Member payee = memberRepository.findById(req.getPayeeMemberId())
                .orElseThrow(() -> new IllegalArgumentException("수취인 회원을 찾을 수 없습니다."));
        if (payee.getId().equals(payer.getId())) {
            throw new IllegalArgumentException("자기 자신에게 자동이체를 설정할 수 없습니다.");
        }
        Contract contract = null;
        if (req.getContractId() != null) {
            contract = contractRepository.findById(req.getContractId())
                    .orElseThrow(() -> new IllegalArgumentException("계약을 찾을 수 없습니다."));
            if (!contract.getTenant().getId().equals(payer.getId())) {
                throw new IllegalStateException("본인이 세입자인 계약만 자동이체에 연결할 수 있습니다.");
            }
        }

        LocalDateTime nextRunAt = computeNextRunAt(req.getDayOfMonth(), LocalDateTime.now());
        AutoTransferSchedule schedule = AutoTransferSchedule.builder()
                .payer(payer)
                .payee(payee)
                .contract(contract)
                .amount(req.getAmount())
                .type(req.getType())
                .dayOfMonth(req.getDayOfMonth())
                .nextRunAt(nextRunAt)
                .memo(req.getMemo())
                .build();
        return AutoTransferRes.from(scheduleRepository.save(schedule));
    }

    public List<AutoTransferRes> listMine(String payerEmail) {
        return scheduleRepository.findByPayerEmail(payerEmail).stream()
                .map(AutoTransferRes::from)
                .toList();
    }

    @Transactional
    public AutoTransferRes update(String payerEmail, Long scheduleId, AutoTransferUpdateReq req) {
        Member me = findMember(payerEmail);
        AutoTransferSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("자동이체 스케줄을 찾을 수 없습니다."));
        ensureOwner(schedule, me);

        LocalDateTime nextRunAt = null;
        if (req.getDayOfMonth() != null) {
            nextRunAt = computeNextRunAt(req.getDayOfMonth(), LocalDateTime.now());
        }
        schedule.update(req.getAmount(), req.getDayOfMonth(), nextRunAt, req.getMemo(), req.getActive());
        return AutoTransferRes.from(schedule);
    }

    @Transactional
    public void cancel(String payerEmail, Long scheduleId) {
        Member me = findMember(payerEmail);
        AutoTransferSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new EntityNotFoundException("자동이체 스케줄을 찾을 수 없습니다."));
        ensureOwner(schedule, me);
        schedule.cancel();
    }

    // ────────────────────────────────────────────────────────────────────────
    // 스케줄 실행 (Scheduler가 호출)
    // ────────────────────────────────────────────────────────────────────────

    @Transactional
    public int runDueSchedules() {
        LocalDateTime now = LocalDateTime.now();
        List<AutoTransferSchedule> due = scheduleRepository.findDueSchedules(now);
        int success = 0;
        for (AutoTransferSchedule s : due) {
            try {
                executeOne(s, now);
                success++;
            } catch (Exception e) {
                log.error("[AutoTransfer] schedule={} 실행 실패: {}", s.getId(), e.getMessage(), e);
            }
        }
        if (!due.isEmpty()) {
            log.info("[AutoTransfer] 도래 스케줄 {}건 중 {}건 처리 성공", due.size(), success);
        }
        return success;
    }

    private void executeOne(AutoTransferSchedule s, LocalDateTime now) {
        String targetAccount = resolveAccountDisplay(s.getPayee().getProfile());
        String orderId = "TSY-A-" + UUID.randomUUID().toString().replace("-", "");
        Payment payment = Payment.builder()
                .member(s.getPayer())
                .amount(s.getAmount())
                .targetAccount(targetAccount)
                .type(s.getType())
                .contract(s.getContract())
                .orderId(orderId)
                .dutchPayGroup(null)
                .isAutoTransfer(true)
                .build();
        paymentRepository.save(payment);

        LocalDateTime next = computeNextRunAt(s.getDayOfMonth(), now.plusDays(1));
        s.markRun(now, next);

        notificationService.notify(
                s.getPayer(),
                NotificationType.PAYMENT,
                "자동이체 결제 대기",
                String.format("%s 자동이체 %,d원이 준비되었습니다. 결제 승인을 진행해 주세요.",
                        s.getType().name(), s.getAmount()),
                "/payments/" + payment.getId()
        );
        notificationService.notify(
                s.getPayee(),
                NotificationType.PAYMENT,
                "자동이체 결제 대기 알림",
                String.format("%s님의 %s 자동이체 %,d원이 결제 대기 상태입니다.",
                        s.getPayer().getName(), s.getType().name(), s.getAmount()),
                "/payments/" + payment.getId()
        );
    }

    // ────────────────────────────────────────────────────────────────────────
    // 유틸
    // ────────────────────────────────────────────────────────────────────────

    /**
     * 다음 실행 시각 계산.
     * - {@code from} 기준으로 같은 달의 {@code dayOfMonth} 가 아직 안 지났으면 그 날짜
     * - 이미 지났으면 다음 달 {@code dayOfMonth}
     * - 해당 달에 그 일자가 없으면(예: 2월 30일) 그 달 말일로 조정
     * 모두 자정(00:00) 기준.
     */
    static LocalDateTime computeNextRunAt(int dayOfMonth, LocalDateTime from) {
        YearMonth ym = YearMonth.from(from.toLocalDate());
        int day = Math.min(dayOfMonth, ym.lengthOfMonth());
        LocalDate candidate = ym.atDay(day);
        if (!candidate.isAfter(from.toLocalDate())) {
            ym = ym.plusMonths(1);
            day = Math.min(dayOfMonth, ym.lengthOfMonth());
            candidate = ym.atDay(day);
        }
        return candidate.atStartOfDay();
    }

    private static String resolveAccountDisplay(Profile profile) {
        if (profile != null && StringUtils.hasText(profile.getAccountInfo())) {
            return profile.getAccountInfo().trim();
        }
        return ACCOUNT_NOT_SET;
    }

    private Member findMember(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
    }

    private static void ensureOwner(AutoTransferSchedule schedule, Member me) {
        if (!schedule.getPayer().getId().equals(me.getId())) {
            throw new IllegalStateException("본인의 자동이체 스케줄만 처리할 수 있습니다.");
        }
    }
}
