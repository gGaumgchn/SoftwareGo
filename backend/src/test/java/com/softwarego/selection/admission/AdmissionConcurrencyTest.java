package com.softwarego.selection.admission;

import com.softwarego.selection.admission.dto.GrantOfferRequest;
import com.softwarego.selection.admission.dto.GrantOfferResponse;
import com.softwarego.selection.admission.service.AdmissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 并发发放：先锁 lab_info 名额行，再在锁内重算占用，不能超发。
 * 不使用 @Transactional；测试后清理独立学生并恢复阶段/名额。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdmissionConcurrencyTest extends AdmissionTestSupport {

    private static final long TEACHER3 = 3L;

    @Autowired
    private AdmissionService admissionService;

    private List<Map<String, Object>> originalStages;
    private Integer originalPlannedQuota;
    private long occupiedBefore;
    private StudentTarget target;

    @BeforeEach
    void setUp() throws Exception {
        originalStages = snapshotStages();
        originalPlannedQuota = jdbcTemplate.queryForObject(
                "SELECT planned_quota FROM lab_info WHERE teacher_id = ?",
                Integer.class, TEACHER3);
        setRound2Open();
        occupiedBefore = countFinalAdmissions(TEACHER3) + countPendingOffers(TEACHER3);
        setPlannedQuota(TEACHER3, (int) occupiedBefore + 1);
        target = createStudentTarget("2026999980", TEACHER3, new BigDecimal("95.00"));
    }

    @AfterEach
    void tearDown() {
        if (target != null) {
            cleanupStudentData(target.studentId());
        }
        if (originalPlannedQuota != null) {
            setPlannedQuota(TEACHER3, originalPlannedQuota);
        }
        if (originalStages != null) {
            restoreStages(originalStages);
        }
    }

    @Test
    void concurrentGrantDoesNotOversubscribe() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<GrantOfferResponse>> futures = List.of(
                    executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        return admissionService.grant(TEACHER3, new GrantOfferRequest(new BigDecimal("90")));
                    }),
                    executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        return admissionService.grant(TEACHER3, new GrantOfferRequest(new BigDecimal("90")));
                    }));

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            int success = 0;
            for (Future<GrantOfferResponse> future : futures) {
                try {
                    GrantOfferResponse response = future.get(10, TimeUnit.SECONDS);
                    success += 1;
                    assertThat(response.grantedCount()).isEqualTo(1);
                } catch (ExecutionException ignored) {
                    // 失败方应拿到名额已满/无可发放，最终只要求一个 offer。
                }
            }

            assertThat(success).isGreaterThanOrEqualTo(1);
            assertThat(countOffersByTarget(target.targetId())).isEqualTo(1);
            assertThat(countFinalAdmissions(TEACHER3) + countPendingOffers(TEACHER3))
                    .isEqualTo(occupiedBefore + 1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}
