package com.maritel.trustay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "trustay.auto-transfer.enabled", havingValue = "true", matchIfMissing = true)
public class AutoTransferScheduler {

    private final AutoTransferService autoTransferService;

    @Value("${trustay.auto-transfer.cron:0 0 0 * * *}")
    private String cronExpr;

    /**
     * application.yml 의 {@code trustay.auto-transfer.cron} 에서 cron 표현식을 받는다.
     * - 기본값: 매일 0시 0분 0초
     * - {@code trustay.auto-transfer.enabled=false} 로 두면 빈이 등록되지 않아 스케줄러 자체가 꺼짐
     * - 한 건 실패가 다른 건 실행을 막지 않도록 service 내부에서 개별 try/catch 처리
     */
    @Scheduled(cron = "${trustay.auto-transfer.cron:0 0 0 * * *}")
    public void runDailyAutoTransfers() {
        try {
            int processed = autoTransferService.runDueSchedules();
            log.info("[AutoTransferScheduler] cron='{}' - 자동이체 실행 완료: {}건 처리", cronExpr, processed);
        } catch (Exception e) {
            log.error("[AutoTransferScheduler] 자동이체 실행 중 예기치 못한 오류", e);
        }
    }
}
