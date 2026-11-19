package com.softwarego.selection.stage;

import com.softwarego.selection.stage.domain.StageCode;
import com.softwarego.selection.stage.dto.StageSaveRequest;
import com.softwarego.selection.stage.dto.StageSaveResponse;
import com.softwarego.selection.stage.service.StageConfigService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 并发重复保存同一 changeNo：7 行 stage 锁串行化，只保存一次、只发布一条公告。
 * 不使用 @Transactional；测试后恢复阶段时间并清理测试变更/公告。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StageConcurrencyTest extends StageTestSupport {

    @Autowired
    private StageConfigService stageConfigService;

    private List<StageTimeRow> originalStages;

    @BeforeEach
    void setUp() {
        originalStages = loadStageTimes();
        jdbcTemplate.update("UPDATE stage SET start_at = NULL, end_at = NULL");
        deleteTestStageChanges();
        deleteTestAnnouncements();
    }

    @AfterEach
    void tearDown() {
        setStageTimes(originalStages);
        deleteTestStageChanges();
        deleteTestAnnouncements();
    }

    @Test
    void concurrentDuplicateSavePublishesSingleAnnouncement() throws Exception {
        List<StageTimeRow> rows = sequentialRows(now().plusDays(30));
        List<StageSaveRequest.StageSaveItem> items = rows.stream()
                .map(row -> new StageSaveRequest.StageSaveItem(
                        row.stageCode().name(), row.startAt(), row.endAt()))
                .toList();
        String changeNo = TEST_CHANGE_PREFIX + UUID.randomUUID();
        String title = TEST_TITLE_PREFIX + " 并发公告";
        StageSaveRequest request = new StageSaveRequest(
                changeNo,
                items,
                true,
                new StageSaveRequest.AnnouncementDraftRequest(title, "并发重复保存测试公告内容"));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<StageSaveResponse>> futures = List.of(
                    executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        return stageConfigService.save(request, 1L);
                    }),
                    executor.submit(() -> {
                        ready.countDown();
                        start.await();
                        return stageConfigService.save(request, 1L);
                    }));

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            for (Future<StageSaveResponse> future : futures) {
                assertThat(future.get(10, TimeUnit.SECONDS).stageSaved()).isTrue();
            }

            assertThat(countStageChangesByChangeNo(changeNo)).isEqualTo(1);
            assertThat(countAnnouncementsByTitle(title)).isEqualTo(1);
            assertThat(stageStart(StageCode.BRIEFING)).isEqualTo(rows.get(0).startAt());
            assertThat(stageEnd(StageCode.ENROLLMENT)).isEqualTo(rows.get(6).endAt());
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}
