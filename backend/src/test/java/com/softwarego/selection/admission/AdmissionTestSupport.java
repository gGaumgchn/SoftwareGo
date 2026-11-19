package com.softwarego.selection.admission;

import com.softwarego.selectiontest.support.B3TestSupport;
import org.springframework.mock.web.MockHttpSession;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * B5 测试助手：控制轮次/名额，创建独立学生目标，避免依赖会被走查修改的种子状态。
 */
abstract class AdmissionTestSupport extends B3TestSupport {

    protected record StudentTarget(
            MockHttpSession session,
            Long studentId,
            Long applicationId,
            Long targetId
    ) {
    }

    protected void setRound2Open() {
        jdbcTemplate.update("UPDATE stage SET start_at = DATE_SUB(NOW(), INTERVAL 2 HOUR), "
                + "end_at = DATE_SUB(NOW(), INTERVAL 1 HOUR) WHERE stage_code = 'ROUND_1'");
        jdbcTemplate.update("UPDATE stage SET start_at = DATE_SUB(NOW(), INTERVAL 30 MINUTE), "
                + "end_at = DATE_ADD(NOW(), INTERVAL 1 HOUR) WHERE stage_code = 'ROUND_2'");
        jdbcTemplate.update("UPDATE stage SET start_at = DATE_ADD(NOW(), INTERVAL 2 HOUR), "
                + "end_at = DATE_ADD(NOW(), INTERVAL 3 HOUR) WHERE stage_code = 'ROUND_3'");
    }

    protected void setRound2Ended() {
        jdbcTemplate.update("UPDATE stage SET start_at = DATE_SUB(NOW(), INTERVAL 3 HOUR), "
                + "end_at = DATE_SUB(NOW(), INTERVAL 2 HOUR) WHERE stage_code = 'ROUND_1'");
        jdbcTemplate.update("UPDATE stage SET start_at = DATE_SUB(NOW(), INTERVAL 2 HOUR), "
                + "end_at = DATE_SUB(NOW(), INTERVAL 1 HOUR) WHERE stage_code = 'ROUND_2'");
        jdbcTemplate.update("UPDATE stage SET start_at = DATE_ADD(NOW(), INTERVAL 2 HOUR), "
                + "end_at = DATE_ADD(NOW(), INTERVAL 3 HOUR) WHERE stage_code = 'ROUND_3'");
    }

    protected void setAllRoundsEnded() {
        jdbcTemplate.update("UPDATE stage SET start_at = DATE_SUB(NOW(), INTERVAL 6 HOUR), "
                + "end_at = DATE_SUB(NOW(), INTERVAL 5 HOUR) WHERE stage_code = 'ROUND_1'");
        jdbcTemplate.update("UPDATE stage SET start_at = DATE_SUB(NOW(), INTERVAL 4 HOUR), "
                + "end_at = DATE_SUB(NOW(), INTERVAL 3 HOUR) WHERE stage_code = 'ROUND_2'");
        jdbcTemplate.update("UPDATE stage SET start_at = DATE_SUB(NOW(), INTERVAL 2 HOUR), "
                + "end_at = DATE_SUB(NOW(), INTERVAL 1 HOUR) WHERE stage_code = 'ROUND_3'");
    }

    protected void setPlannedQuota(Long teacherId, int plannedQuota) {
        jdbcTemplate.update(
                "UPDATE lab_info SET planned_quota = ? WHERE teacher_id = ?",
                plannedQuota, teacherId);
    }

    protected StudentTarget createStudentTarget(String staffNo,
                                                Long teacherId,
                                                BigDecimal score) throws Exception {
        MockHttpSession session = login("STUDENT", staffNo);
        Long studentId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE staff_no = ? AND role = 'STUDENT'",
                Long.class, staffNo);
        jdbcTemplate.update(
                "INSERT INTO volunteer_application "
                        + "(student_id, round_no, resume_file_id, intra_lab_adjust, college_adjust, submitted_at) "
                        + "VALUES (?, 2, 5, 1, 1, NOW())",
                studentId);
        Long applicationId = jdbcTemplate.queryForObject(
                "SELECT id FROM volunteer_application WHERE student_id = ? AND round_no = 2",
                Long.class, studentId);
        jdbcTemplate.update(
                "INSERT INTO application_target (application_id, teacher_id, preference_no, score) "
                        + "VALUES (?, ?, 1, ?)",
                applicationId, teacherId, score);
        Long targetId = jdbcTemplate.queryForObject(
                "SELECT id FROM application_target WHERE application_id = ? AND teacher_id = ?",
                Long.class, applicationId, teacherId);
        return new StudentTarget(session, studentId, applicationId, targetId);
    }

    protected void insertAdjustmentFinal(Long studentId, Long teacherId) {
        jdbcTemplate.update(
                "INSERT INTO final_admission "
                        + "(student_id, teacher_id, round_no, admission_type, source_offer_id, admitted_at) "
                        + "VALUES (?, ?, NULL, 'ADJUST_COLLEGE', NULL, NOW())",
                studentId, teacherId);
    }

    protected long countFinalAdmissions(Long teacherId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM final_admission WHERE teacher_id = ?",
                Long.class, teacherId);
        return count == null ? 0 : count;
    }

    protected long countPendingOffers(Long teacherId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM offer WHERE teacher_id = ? AND status = 'PENDING' "
                        + "AND confirm_deadline > ?",
                Long.class, teacherId, now());
        return count == null ? 0 : count;
    }

    protected long countOffersByTarget(Long targetId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM offer WHERE application_target_id = ?",
                Long.class, targetId);
        return count == null ? 0 : count;
    }

    protected Map<String, Object> offerByTarget(Long targetId) {
        return jdbcTemplate.queryForMap(
                "SELECT id, teacher_id, student_id, round_no, status, confirm_deadline "
                        + "FROM offer WHERE application_target_id = ?",
                targetId);
    }

    protected List<Map<String, Object>> snapshotStages() {
        return jdbcTemplate.queryForList(
                "SELECT stage_code, start_at, end_at FROM stage ORDER BY sort_no");
    }

    protected void restoreStages(List<Map<String, Object>> rows) {
        for (Map<String, Object> row : rows) {
            jdbcTemplate.update(
                    "UPDATE stage SET start_at = ?, end_at = ? WHERE stage_code = ?",
                    row.get("start_at"), row.get("end_at"), row.get("stage_code"));
        }
    }

    protected void cleanupStudentData(Long studentId) {
        jdbcTemplate.update("DELETE FROM offer WHERE student_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM final_admission WHERE student_id = ?", studentId);
        jdbcTemplate.update(
                "DELETE FROM application_target WHERE application_id IN "
                        + "(SELECT id FROM volunteer_application WHERE student_id = ?)",
                studentId);
        jdbcTemplate.update("DELETE FROM volunteer_application WHERE student_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", studentId);
    }

    protected LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Shanghai")).truncatedTo(ChronoUnit.SECONDS);
    }
}
