package com.softwarego.selection;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SelectionApplicationTests {

    @Test
    void contextLoads() {
        // 验证公共基础设施、MyBatis、Jackson 配置能够加载。
    }
}
