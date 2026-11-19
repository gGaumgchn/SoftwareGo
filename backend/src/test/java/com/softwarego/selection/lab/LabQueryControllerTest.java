package com.softwarego.selection.lab;

import com.fasterxml.jackson.databind.JsonNode;
import com.softwarego.selectiontest.support.B3TestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 实训室公开列表/详情、导师本人信息、名额占用只读测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LabQueryControllerTest extends B3TestSupport {

    @Test
    void labsListFiltersAndPagination() throws Exception {
        MockHttpSession student = studentSession();

        mockMvc.perform(get("/api/v1/labs")
                        .session(student)
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(4))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.list", hasSize(4)))
                .andExpect(jsonPath("$.data.list[*].labTotalQuota", everyItem(notNullValue())))
                .andExpect(jsonPath("$.data.researchDirections", hasSize(4)));

        mockMvc.perform(get("/api/v1/labs")
                        .session(student)
                        .param("keyword", "智能"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].labName").value("智能软件实训室"))
                .andExpect(jsonPath("$.data.researchDirections", hasSize(4)));

        mockMvc.perform(get("/api/v1/labs")
                        .session(student)
                        .param("researchDirection", "大数据平台与数据治理"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].teacherName").value("林若溪"));

        mockMvc.perform(get("/api/v1/labs")
                        .session(student)
                        .param("page", "1")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(2)))
                .andExpect(jsonPath("$.data.total").value(4));

        mockMvc.perform(get("/api/v1/labs")
                        .session(student)
                        .param("page", "2")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(2)))
                .andExpect(jsonPath("$.data.total").value(4));

        mockMvc.perform(get("/api/v1/labs")
                        .session(student)
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("分页参数不合法"));

        mockMvc.perform(get("/api/v1/labs")
                        .session(student)
                        .param("pageSize", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001));

        mockMvc.perform(get("/api/v1/labs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void labTotalQuotaAggregatesSameLabNameAndLocation() throws Exception {
        jdbcTemplate.update(
                "UPDATE lab_info SET lab_name = ?, location = ? WHERE teacher_id = ?",
                "智能软件实训室", "软件楼 A-301", 3);

        long expectedTotal = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(planned_quota), 0) FROM lab_info "
                        + "WHERE lab_name = ? AND location = ?",
                Long.class, "智能软件实训室", "软件楼 A-301");

        MvcResult listResult = mockMvc.perform(get("/api/v1/labs")
                        .session(studentSession())
                        .param("keyword", "智能软件实训室"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(2)))
                .andReturn();

        JsonNode list = json(listResult).path("data").path("list");
        for (JsonNode item : list) {
            assertThat(item.path("labTotalQuota").asLong()).isEqualTo(expectedTotal);
        }

        mockMvc.perform(get("/api/v1/labs/1").session(studentSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.labTotalQuota").value((int) expectedTotal));
        mockMvc.perform(get("/api/v1/labs/2").session(studentSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.labTotalQuota").value((int) expectedTotal));
    }

    @Test
    void labDetail() throws Exception {
        MockHttpSession student = studentSession();
        Map<String, Object> labRow = jdbcTemplate.queryForMap(
                "SELECT lab_name, location, teacher_id, teacher_name, planned_quota, resume_file_id "
                        + "FROM lab_info WHERE id = 1");
        String labName = (String) labRow.get("lab_name");
        String location = (String) labRow.get("location");
        int teacherId = ((Number) labRow.get("teacher_id")).intValue();
        String teacherName = (String) labRow.get("teacher_name");
        int plannedQuota = ((Number) labRow.get("planned_quota")).intValue();
        Long resumeFileId = ((Number) labRow.get("resume_file_id")).longValue();
        String resumeFileName = jdbcTemplate.queryForObject(
                "SELECT original_name FROM resume_file WHERE id = ?", String.class, resumeFileId);
        long labTotalQuota = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(planned_quota), 0) FROM lab_info "
                        + "WHERE lab_name = ? AND location = ?",
                Long.class, labName, location);

        mockMvc.perform(get("/api/v1/labs/1").session(student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.labName").value(labName))
                .andExpect(jsonPath("$.data.location").value(location))
                .andExpect(jsonPath("$.data.teacherId").value(teacherId))
                .andExpect(jsonPath("$.data.teacherName").value(teacherName))
                .andExpect(jsonPath("$.data.plannedQuota").value(plannedQuota))
                .andExpect(jsonPath("$.data.labTotalQuota").value((int) labTotalQuota))
                .andExpect(jsonPath("$.data.resumeFileId").value(resumeFileId.intValue()))
                .andExpect(jsonPath("$.data.resumeFileName").value(resumeFileName))
                .andExpect(jsonPath("$.data.updatedAt").value(notNullValue()));

        mockMvc.perform(get("/api/v1/labs/999999").session(student))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));

        mockMvc.perform(get("/api/v1/labs/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void teacherLabForSubmittedTeacher() throws Exception {
        Map<String, Object> labRow = jdbcTemplate.queryForMap(
                "SELECT teacher_id, lab_name, planned_quota, resume_file_id "
                        + "FROM lab_info WHERE teacher_id = 2");
        int teacherId = ((Number) labRow.get("teacher_id")).intValue();
        String labName = (String) labRow.get("lab_name");
        int plannedQuota = ((Number) labRow.get("planned_quota")).intValue();
        Long resumeFileId = ((Number) labRow.get("resume_file_id")).longValue();
        String resumeFileName = jdbcTemplate.queryForObject(
                "SELECT original_name FROM resume_file WHERE id = ?", String.class, resumeFileId);
        long occupied = jdbcTemplate.queryForObject(
                "SELECT (SELECT COUNT(*) FROM final_admission WHERE teacher_id = 2) "
                        + "+ (SELECT COUNT(*) FROM offer WHERE teacher_id = 2 "
                        + "AND status = 'PENDING' AND confirm_deadline > NOW())",
                Long.class);

        mockMvc.perform(get("/api/v1/teacher/lab").session(teacherSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submitted").value(true))
                .andExpect(jsonPath("$.data.lab.teacherId").value(teacherId))
                .andExpect(jsonPath("$.data.lab.labName").value(labName))
                .andExpect(jsonPath("$.data.lab.resumeFileId").value(resumeFileId.intValue()))
                .andExpect(jsonPath("$.data.lab.resumeFileName").value(resumeFileName))
                .andExpect(jsonPath("$.data.occupiedQuota").value((int) occupied))
                .andExpect(jsonPath("$.data.remainingQuota")
                        .value(plannedQuota - (int) occupied));

        mockMvc.perform(get("/api/v1/teacher/lab").session(studentSession()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301))
                .andExpect(jsonPath("$.message").value("无权访问该数据"));

        mockMvc.perform(get("/api/v1/teacher/lab"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void teacherLabUnsubmitted() throws Exception {
        MockHttpSession newTeacher = login("TEACHER", "2026999998");

        mockMvc.perform(get("/api/v1/teacher/lab").session(newTeacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submitted").value(false))
                .andExpect(jsonPath("$.data.lab").value(nullValue()))
                .andExpect(jsonPath("$.data.occupiedQuota").value(0))
                .andExpect(jsonPath("$.data.remainingQuota").value(nullValue()));
    }
}
