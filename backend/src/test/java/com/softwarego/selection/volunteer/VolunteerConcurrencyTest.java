package com.softwarego.selection.volunteer;

import com.softwarego.selection.volunteer.dto.VolunteerSubmitRequest;
import com.softwarego.selection.volunteer.dto.VolunteerSubmitResponse;
import com.softwarego.selection.volunteer.service.VolunteerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 并发首次提交同一轮志愿：唯一索引/行锁兜底，只产生一条申请。
 * 不使用 @Transactional，线程需要各自独立连接；测试后显式清理用户/简历/申请并恢复阶段。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VolunteerConcurrencyTest extends VolunteerTestSupport {

    private static final String STAFF_NO = "2026999993";

    @Autowired
    private VolunteerService volunteerService;

    private StudentContext student;
    private List<StageTimeRow> originalStages;

    @BeforeEach
    void setUp() throws Exception {
        originalStages = loadStageTimes();
        setRoundOpen(1);
        student = newStudent(STAFF_NO);
    }

    @AfterEach
    void tearDown() {
        if (student != null) {
            cleanupStudent(student.studentId());
        }
        if (originalStages != null) {
            setStageTimes(originalStages);
        }
    }

    @Test
    void concurrentSubmitCreatesSingleApplication() throws Exception {
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        VolunteerSubmitRequest request = new VolunteerSubmitRequest(
                List.of(teacherName(2L)), true, true, student.resumeFileId());
        List<Future<VolunteerSubmitResponse>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < threadCount; index += 1) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return volunteerService.submit(student.studentId(), request);
                }));
            }

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            int success = 0;
            for (Future<VolunteerSubmitResponse> future : futures) {
                try {
                    future.get(10, TimeUnit.SECONDS);
                    success += 1;
                } catch (ExecutionException ignored) {
                    // 并发失败方可能拿到唯一索引冲突；只要求最终只有一条申请。
                }
            }

            assertThat(success).isGreaterThanOrEqualTo(1);
            assertThat(applicationCount(student.studentId(), 1)).isEqualTo(1);

            Long applicationId = applicationId(student.studentId(), 1);
            assertThat(targetCount(applicationId)).isEqualTo(1);
            assertThat(targetTeacherIds(applicationId)).containsExactly(2L);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}
