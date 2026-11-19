package com.softwarego.selection.auth;

import com.softwarego.selection.user.domain.Role;
import com.softwarego.selection.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 并发首次登录：唯一索引兜底，捕获 DuplicateKeyException 后回查，只产生一条身份。
 * 不使用 @Transactional，因为并发线程需要各自独立连接。
 */
@SpringBootTest
@ActiveProfiles("test")
class AuthConcurrencyTest {

    private static final String TEST_STAFF_NO = "9999000001";

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanBefore() {
        deleteTestUser();
    }

    @AfterEach
    void cleanAfter() {
        deleteTestUser();
    }

    @Test
    void concurrentFirstLoginCreatesOnlyOneUser() throws Exception {
        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Long>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return userService.findOrCreate(TEST_STAFF_NO, Role.STUDENT).getId();
                }));
            }

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            Set<Long> userIds = new HashSet<>();
            for (Future<Long> future : futures) {
                userIds.add(future.get(10, TimeUnit.SECONDS));
            }

            assertThat(userIds).hasSize(1);
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_user WHERE staff_no = ? AND role = ?",
                    Integer.class, TEST_STAFF_NO, "STUDENT");
            assertThat(count).isEqualTo(1);
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private void deleteTestUser() {
        jdbcTemplate.update("DELETE FROM sys_user WHERE staff_no = ?", TEST_STAFF_NO);
    }
}
