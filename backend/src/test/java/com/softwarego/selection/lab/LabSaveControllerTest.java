package com.softwarego.selection.lab;

import com.softwarego.selectiontest.support.B3TestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PUT /teacher/lab：本人唯一更新、名额下限、简历归属、校验、非导师拒绝。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LabSaveControllerTest extends B3TestSupport {

    @Test
    void saveUpdatesOwnLabAndNormalizesTeacherName() throws Exception {
        MockHttpSession teacher = teacherSession();
        Map<String, Object> body = validLabBody();
        body.put("teacherName", "  陈   启航  ");

        mockMvc.perform(put("/api/v1/teacher/lab")
                        .session(teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.submitted").value(true))
                .andExpect(jsonPath("$.data.lab.teacherId").value(2))
                .andExpect(jsonPath("$.data.lab.teacherName").value("陈 启航"))
                .andExpect(jsonPath("$.data.occupiedQuota").value(2))
                .andExpect(jsonPath("$.data.remainingQuota").value(0))
                .andExpect(jsonPath("$.data.message").value("提交成功"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lab_info WHERE teacher_id = 2", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT teacher_name FROM lab_info WHERE teacher_id = 2", String.class))
                .isEqualTo("陈 启航");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT planned_quota FROM lab_info WHERE teacher_id = 2", Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lab_info", Integer.class)).isEqualTo(4);

        mockMvc.perform(get("/api/v1/labs/1").session(studentSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teacherName").value("陈 启航"));
    }

    @Test
    void plannedQuotaBelowOccupiedRejected() throws Exception {
        // 使用独立导师 + 两条最终录取，避免依赖种子 offer/final 的后续变化。
        MockHttpSession newTeacher = login("TEACHER", "2026999997");
        Long teacherId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE staff_no = ? AND role = 'TEACHER'",
                Long.class, "2026999997");

        jdbcTemplate.update(
                "INSERT INTO resume_file (owner_id, purpose, original_name, storage_path, content_type, file_size) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                teacherId, "TEACHER_RESUME", "quota-teacher.pdf",
                "2026/11/quota-teacher.pdf", "application/pdf", 10);
        Long resumeFileId = jdbcTemplate.queryForObject(
                "SELECT id FROM resume_file WHERE storage_path = ?",
                Long.class, "2026/11/quota-teacher.pdf");

        for (long studentId : new long[]{8L, 9L}) {
            jdbcTemplate.update(
                    "INSERT INTO final_admission "
                            + "(student_id, teacher_id, round_no, admission_type, source_offer_id, admitted_at) "
                            + "VALUES (?, ?, NULL, 'ADJUST_COLLEGE', NULL, NOW())",
                    studentId, teacherId);
        }

        Map<String, Object> body = validLabBody();
        body.put("resumeFileId", resumeFileId);
        body.put("teacherName", "配额 导师");
        body.put("plannedQuota", 1);

        mockMvc.perform(put("/api/v1/teacher/lab")
                        .session(newTeacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("计划招收人数不能小于当前已占用名额"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lab_info WHERE teacher_id = ?",
                Integer.class, teacherId)).isZero();
    }

    @Test
    void resumeOwnershipAndPurposeAreChecked() throws Exception {
        MockHttpSession teacher = teacherSession();

        assertForbiddenResume(teacher, 2L);
        assertForbiddenResume(teacher, 5L);
        assertForbiddenResume(teacher, 999999L);
    }

    @Test
    void validationRejectsInvalidFields() throws Exception {
        MockHttpSession teacher = teacherSession();

        Map<String, Object> body = validLabBody();
        body.put("labName", "   ");
        assertInvalid(teacher, body);

        body = validLabBody();
        body.put("location", "   ");
        assertInvalid(teacher, body);

        body = validLabBody();
        body.put("teacherName", "   ");
        assertInvalid(teacher, body);

        body = validLabBody();
        body.put("plannedQuota", 0);
        assertInvalid(teacher, body);

        body = validLabBody();
        body.remove("resumeFileId");
        assertInvalid(teacher, body);
    }

    @Test
    void nonTeacherCannotWrite() throws Exception {
        String body = objectMapper.writeValueAsString(validLabBody());

        mockMvc.perform(put("/api/v1/teacher/lab")
                        .session(studentSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301))
                .andExpect(jsonPath("$.message").value("无权访问该数据"));

        mockMvc.perform(put("/api/v1/teacher/lab")
                        .session(managerSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));

        mockMvc.perform(put("/api/v1/teacher/lab")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void newTeacherFirstSaveInsertsThenUpdatesSingleRow() throws Exception {
        MockHttpSession newTeacher = login("TEACHER", "2026999998");
        Long teacherId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE staff_no = ? AND role = 'TEACHER'",
                Long.class, "2026999998");

        jdbcTemplate.update(
                "INSERT INTO resume_file (owner_id, purpose, original_name, storage_path, content_type, file_size) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                teacherId, "TEACHER_RESUME", "新导师-简历.pdf",
                "2026/11/new-teacher-resume.pdf", "application/pdf", 10);
        Long resumeFileId = jdbcTemplate.queryForObject(
                "SELECT id FROM resume_file WHERE storage_path = ?",
                Long.class, "2026/11/new-teacher-resume.pdf");

        Map<String, Object> body = validLabBody();
        body.put("resumeFileId", resumeFileId);
        body.put("teacherName", "新 导师");
        body.put("labName", "新实训室");
        body.put("plannedQuota", 2);

        mockMvc.perform(put("/api/v1/teacher/lab")
                        .session(newTeacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submitted").value(true))
                .andExpect(jsonPath("$.data.lab.teacherId").value(teacherId.intValue()))
                .andExpect(jsonPath("$.data.occupiedQuota").value(0))
                .andExpect(jsonPath("$.data.remainingQuota").value(2))
                .andExpect(jsonPath("$.data.message").value("提交成功"));

        body.put("labName", "新实训室改名");
        body.put("plannedQuota", 3);
        mockMvc.perform(put("/api/v1/teacher/lab")
                        .session(newTeacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lab.labName").value("新实训室改名"))
                .andExpect(jsonPath("$.data.remainingQuota").value(3));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lab_info WHERE teacher_id = ?", Integer.class, teacherId))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT lab_name FROM lab_info WHERE teacher_id = ?", String.class, teacherId))
                .isEqualTo("新实训室改名");
    }

    private Map<String, Object> validLabBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("labName", "智能软件实训室");
        body.put("location", "软件楼 A-301");
        body.put("plannedQuota", 2);
        body.put("teacherName", "陈启航");
        body.put("researchDirection", "智能软件与自动化测试");
        body.put("contact", "chen.qihang@example.edu");
        body.put("resumeFileId", 1);
        return body;
    }

    private void assertForbiddenResume(MockHttpSession teacher, Long resumeFileId) throws Exception {
        Map<String, Object> body = validLabBody();
        body.put("resumeFileId", resumeFileId);

        mockMvc.perform(put("/api/v1/teacher/lab")
                        .session(teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301))
                .andExpect(jsonPath("$.message").value("无权访问该数据"));
    }

    private void assertInvalid(MockHttpSession teacher, Map<String, Object> body) throws Exception {
        mockMvc.perform(put("/api/v1/teacher/lab")
                        .session(teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("提交失败，请重试"));
    }
}
