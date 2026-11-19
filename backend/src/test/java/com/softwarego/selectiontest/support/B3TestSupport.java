package com.softwarego.selectiontest.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * B3 测试公共助手：使用种子账号登录，提供 JSON 解析。
 */
public abstract class B3TestSupport {

    protected static final String MANAGER_NO = "9000000001";
    protected static final String STUDENT_NO = "2026001001";       // 王雨桐，student id 6
    protected static final String STUDENT2_NO = "2026001002";      // 李昊然，student id 7
    protected static final String TEACHER_NO = "1000000001";       // 陈启航，teacher id 2
    protected static final String OTHER_TEACHER_NO = "1000000003"; // 赵子墨，teacher id 4

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected MockHttpSession login(String role, String staffNo) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("role", role, "staffNo", staffNo))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    protected MockHttpSession managerSession() throws Exception {
        return login("MANAGER", MANAGER_NO);
    }

    protected MockHttpSession studentSession() throws Exception {
        return login("STUDENT", STUDENT_NO);
    }

    protected MockHttpSession student2Session() throws Exception {
        return login("STUDENT", STUDENT2_NO);
    }

    protected MockHttpSession teacherSession() throws Exception {
        return login("TEACHER", TEACHER_NO);
    }

    protected MockHttpSession otherTeacherSession() throws Exception {
        return login("TEACHER", OTHER_TEACHER_NO);
    }

    protected JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
