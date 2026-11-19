package com.softwarego.selection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 只读冒烟：验证 application-test.yml 的数据源可用，且 schema.sql 的 7 行阶段已初始化。
 * 不写入任何业务数据，不会影响 dev 演示数据。
 */
@SpringBootTest
@ActiveProfiles("test")
class DatabaseSmokeTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void databaseIsReachableAndStageHasSevenRows() {
        Integer stageCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stage", Integer.class);
        assertThat(stageCount).isEqualTo(7);
    }
}
