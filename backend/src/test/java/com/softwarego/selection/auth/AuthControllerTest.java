package com.softwarego.selection.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwarego.selectiontest.support.RoleProbeController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RoleProbeController.class)
@Transactional
class AuthControllerTest {

    private static final String AUTO_NO = "2026999901";
    private static final String SAME_NO = "2026999902";
    private static final String FIXATION_NO = "2026999903";
    private static final String STUDENT_NO = "2026999904";
    private static final String TEACHER_NO = "2026999905";
    private static final String MANAGER_NO = "2026999906";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void loginCreatesUserAndReturnsSession() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("STUDENT", AUTO_NO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.userId").value(notNullValue()))
                .andExpect(jsonPath("$.data.staffNo").value(AUTO_NO))
                .andExpect(jsonPath("$.data.name").value(nullValue()))
                .andExpect(jsonPath("$.data.role").value("STUDENT"))
                .andExpect(content().string(not(containsString("JSESSIONID"))))
                .andExpect(content().string(not(containsString("password"))))
                .andReturn();

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE staff_no = ? AND role = ?",
                Integer.class, AUTO_NO, "STUDENT");
        assertThat(count).isEqualTo(1);

        MockHttpSession session = sessionOf(result);
        assertThat(session).isNotNull();

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(notNullValue()))
                .andExpect(jsonPath("$.data.staffNo").value(AUTO_NO))
                .andExpect(jsonPath("$.data.name").value(nullValue()))
                .andExpect(jsonPath("$.data.role").value("STUDENT"));
    }

    @Test
    void sameStaffNoDifferentRolesCreatesTwoUsers() throws Exception {
        long studentId = userIdOf(login("STUDENT", SAME_NO));
        long teacherId = userIdOf(login("TEACHER", SAME_NO));

        assertThat(studentId).isNotEqualTo(teacherId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE staff_no = ?", Integer.class, SAME_NO))
                .isEqualTo(2);
    }

    @Test
    void existingUserIsReused() throws Exception {
        long first = userIdOf(login("STUDENT", AUTO_NO));
        long second = userIdOf(login("STUDENT", AUTO_NO));

        assertThat(second).isEqualTo(first);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE staff_no = ? AND role = ?",
                Integer.class, AUTO_NO, "STUDENT")).isEqualTo(1);
    }

    @Test
    void loginThenMeThenLogout() throws Exception {
        MockHttpSession session = sessionOf(login("STUDENT", STUDENT_NO));
        assertThat(session).isNotNull();

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.staffNo").value(STUDENT_NO))
                .andExpect(jsonPath("$.data.role").value("STUDENT"));

        mockMvc.perform(post("/api/v1/auth/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(nullValue()));

        assertThat(session.isInvalid()).isTrue();

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void sessionIsReplacedOnLogin() throws Exception {
        MockHttpSession oldSession = sessionOf(login("STUDENT", FIXATION_NO));
        assertThat(oldSession).isNotNull();

        MvcResult second = mockMvc.perform(post("/api/v1/auth/login")
                        .session(oldSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("STUDENT", FIXATION_NO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        MockHttpSession newSession = sessionOf(second);
        assertThat(newSession).isNotNull();
        assertThat(oldSession.isInvalid()).isTrue();
        assertThat(newSession).isNotSameAs(oldSession);

        mockMvc.perform(get("/api/v1/auth/me").session(newSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.staffNo").value(FIXATION_NO));
    }

    @Test
    void unauthenticatedRequestsReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.message").value("未登录或会话过期"))
                .andExpect(jsonPath("$.data").value(nullValue()));

        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));

        mockMvc.perform(get("/api/v1/student/probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void missingRoleReturns40001() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"staffNo\":\"2026001001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("请选择身份"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void invalidStaffNoReturns40001() throws Exception {
        assertInvalidStaffNo("{\"role\":\"STUDENT\",\"staffNo\":\"123\"}");
        assertInvalidStaffNo("{\"role\":\"STUDENT\",\"staffNo\":\"20260010AB\"}");
        assertInvalidStaffNo("{\"role\":\"STUDENT\",\"staffNo\":\"\"}");
        assertInvalidStaffNo("{\"role\":\"STUDENT\"}");
    }

    @Test
    void invalidRoleReturns40001() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\",\"staffNo\":\"2026001001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("请求参数格式错误"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void rolePathsEnforceRole() throws Exception {
        MockHttpSession studentSession = sessionOf(login("STUDENT", STUDENT_NO));
        mockMvc.perform(get("/api/v1/student/probe").session(studentSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/v1/teacher/probe").session(studentSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301))
                .andExpect(jsonPath("$.message").value("无权访问该数据"));
        mockMvc.perform(get("/api/v1/manager/probe").session(studentSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301))
                .andExpect(jsonPath("$.message").value("仅负责人可配置阶段"));

        MockHttpSession teacherSession = sessionOf(login("TEACHER", TEACHER_NO));
        mockMvc.perform(get("/api/v1/teacher/probe").session(teacherSession))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/student/probe").session(teacherSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));

        MockHttpSession managerSession = sessionOf(login("MANAGER", MANAGER_NO));
        mockMvc.perform(get("/api/v1/manager/probe").session(managerSession))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/student/probe").session(managerSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void healthIsPublicWithoutSession() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    private MvcResult login(String role, String staffNo) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(role, staffNo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
    }

    private String loginBody(String role, String staffNo) throws Exception {
        return objectMapper.writeValueAsString(Map.of("role", role, "staffNo", staffNo));
    }

    private MockHttpSession sessionOf(MvcResult result) {
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private long userIdOf(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("userId").asLong();
    }

    private void assertInvalidStaffNo(String body) throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("学工号异常"));
    }
}
