package com.softwarego.selection.todo;

import com.softwarego.selection.common.BusinessException;
import com.softwarego.selection.offer.domain.AdmissionType;
import com.softwarego.selection.offer.domain.FinalAdmission;
import com.softwarego.selection.offer.service.OfferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 线下调剂导入的校验/幂等，以及调剂结果只读展示。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdjustmentImportTest extends B7TestSupport {

    @Autowired
    private OfferService offerService;

    @Test
    void importCreatesReadOnlyAdjustment() throws Exception {
        MockHttpSession session = login("STUDENT", "2026999994");
        Long studentId = studentId("2026999994");
        setPlannedQuota(3L, 10);

        FinalAdmission imported = offerService.importAdjustment(
                studentId, 3L, AdmissionType.ADJUST_INTRA_LAB);

        assertThat(imported.getId()).isNotNull();
        assertThat(imported.getRoundNo()).isNull();
        assertThat(imported.getSourceOfferId()).isNull();
        assertThat(imported.getAdmissionType()).isEqualTo(AdmissionType.ADJUST_INTRA_LAB);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT admission_type, round_no, source_offer_id FROM final_admission "
                        + "WHERE student_id = ?",
                studentId);
        assertThat(row.get("admission_type")).isEqualTo("ADJUST_INTRA_LAB");
        assertThat(row.get("round_no")).isNull();
        assertThat(row.get("source_offer_id")).isNull();

        mockMvc.perform(get("/api/v1/student/admission").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.admitted").value(true))
                .andExpect(jsonPath("$.data.admission.admissionType").value("ADJUST_INTRA_LAB"))
                .andExpect(jsonPath("$.data.admission.roundNo").value(nullValue()));

        // 调剂仅展示结果：没有可确认的拟录取记录。
        mockMvc.perform(get("/api/v1/student/offers").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(0)));

        mockMvc.perform(get("/api/v1/todos").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(0)));
    }

    @Test
    void duplicateImportRejectedByExistingAdmission() throws Exception {
        login("STUDENT", "2026999994");
        Long studentId = studentId("2026999994");
        setPlannedQuota(3L, 10);

        offerService.importAdjustment(studentId, 3L, AdmissionType.ADJUST_COLLEGE);

        assertThatThrownBy(() -> offerService.importAdjustment(
                studentId, 3L, AdmissionType.ADJUST_COLLEGE))
                .isInstanceOf(BusinessException.class)
                .hasMessage("学生已有最终录取");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM final_admission WHERE student_id = ?",
                Integer.class, studentId);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void existingAdmissionRejected() {
        assertThatThrownBy(() -> offerService.importAdjustment(
                6L, 3L, AdmissionType.ADJUST_COLLEGE))
                .isInstanceOf(BusinessException.class)
                .hasMessage("学生已有最终录取");
    }

    @Test
    void pendingOfferBlocksImport() throws Exception {
        login("STUDENT", "2026001006");
        Long studentId = studentId("2026001006");
        setOfferDeadlineInMinutes(6L, 1440L);
        setPlannedQuota(3L, 10);

        assertThatThrownBy(() -> offerService.importAdjustment(
                studentId, 3L, AdmissionType.ADJUST_COLLEGE))
                .isInstanceOf(BusinessException.class)
                .hasMessage("学生存在未处理拟录取");
    }

    @Test
    void expiredPendingOfferIsTimedOutBeforeImport() throws Exception {
        login("STUDENT", "2026001006");
        Long studentId = studentId("2026001006");
        setOfferDeadlineInMinutes(6L, -60L);
        setPlannedQuota(3L, 10);

        offerService.importAdjustment(studentId, 3L, AdmissionType.ADJUST_COLLEGE);

        Map<String, Object> offer = jdbcTemplate.queryForMap(
                "SELECT status, abandon_reason FROM offer WHERE id = 6");
        assertThat(offer.get("status")).isEqualTo("ABANDONED");
        assertThat(offer.get("abandon_reason")).isEqualTo("TIMEOUT");

        Integer finalCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM final_admission WHERE student_id = ?",
                Integer.class, studentId);
        assertThat(finalCount).isEqualTo(1);
    }

    @Test
    void quotaInsufficientRejected() throws Exception {
        login("STUDENT", "2026999994");
        Long studentId = studentId("2026999994");

        // 导师 5：计划 2，已有最终录取 1 + 未截止 PENDING offer 1。
        assertThatThrownBy(() -> offerService.importAdjustment(
                studentId, 5L, AdmissionType.ADJUST_COLLEGE))
                .isInstanceOf(BusinessException.class)
                .hasMessage("导师名额不足");
    }

    @Test
    void invalidAdjustmentTypeRejected() throws Exception {
        login("STUDENT", "2026999994");
        Long studentId = studentId("2026999994");

        assertThatThrownBy(() -> offerService.importAdjustment(
                studentId, 3L, AdmissionType.VOLUNTEER))
                .isInstanceOf(BusinessException.class)
                .hasMessage("调剂类型不合法");
    }

    private Long studentId(String staffNo) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE staff_no = ? AND role = 'STUDENT'",
                Long.class, staffNo);
    }
}
