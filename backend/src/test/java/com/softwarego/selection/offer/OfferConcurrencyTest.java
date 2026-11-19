package com.softwarego.selection.offer;

import com.softwarego.selection.offer.dto.ConfirmOfferResponse;
import com.softwarego.selection.offer.service.OfferService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

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
 * 并发确认同一学生的两个拟录取：行锁 + final_admission(student_id) 唯一索引只能产生一个最终录取。
 * 不使用 @Transactional，线程需要独立连接；测试后清理独立学生的全部数据。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OfferConcurrencyTest extends OfferTestSupport {

    @Autowired
    private OfferService offerService;

    private StudentOffers data;

    @BeforeEach
    void setUp() throws Exception {
        data = createStudentWithOffers("2026999989");
    }

    @AfterEach
    void tearDown() {
        if (data != null) {
            cleanupStudentData(data.studentId());
        }
    }

    @Test
    void concurrentConfirmCreatesSingleFinalAdmission() throws Exception {
        Long firstOfferId = data.offerIds().get(0);
        Long secondOfferId = data.offerIds().get(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Long> offerIds = List.of(firstOfferId, secondOfferId);

        try {
            List<Future<ConfirmOfferResponse>> futures = offerIds.stream()
                    .map(offerId -> executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        return offerService.confirm(data.studentId(), offerId);
                    }))
                    .toList();

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            int success = 0;
            for (Future<ConfirmOfferResponse> future : futures) {
                try {
                    future.get(10, TimeUnit.SECONDS);
                    success += 1;
                } catch (ExecutionException ignored) {
                    // 失败方可能拿到已确认/确认失败；最终只要求一个最终录取。
                }
            }

            assertThat(success).isGreaterThanOrEqualTo(1);
            assertThat(finalAdmissionCount(data.studentId())).isEqualTo(1);

            Map<String, Object> first = offerRow(firstOfferId);
            Map<String, Object> second = offerRow(secondOfferId);
            long confirmedCount = List.of(first, second).stream()
                    .filter(row -> "CONFIRMED".equals(row.get("status")))
                    .count();
            long abandonedCount = List.of(first, second).stream()
                    .filter(row -> "ABANDONED".equals(row.get("status")))
                    .count();
            assertThat(confirmedCount).isEqualTo(1);
            assertThat(abandonedCount).isEqualTo(1);

            Long confirmedOfferId = "CONFIRMED".equals(first.get("status"))
                    ? firstOfferId : secondOfferId;
            Map<String, Object> finalRow = finalAdmissionRow(data.studentId());
            assertThat(((Number) finalRow.get("source_offer_id")).longValue())
                    .isEqualTo(confirmedOfferId);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}
