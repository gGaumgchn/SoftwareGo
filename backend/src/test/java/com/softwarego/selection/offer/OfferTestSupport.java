package com.softwarego.selection.offer;

import com.softwarego.selectiontest.support.B3TestSupport;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * B6 测试助手：为独立测试学生构造申请/目标/拟录取，避免依赖会被走查修改的种子学生。
 */
abstract class OfferTestSupport extends B3TestSupport {

    protected record StudentOffers(
            MockHttpSession session,
            Long studentId,
            Long applicationId,
            List<Long> targetIds,
            List<Long> offerIds
    ) {
    }

    protected StudentOffers createStudentWithOffers(String staffNo) throws Exception {
        MockHttpSession session = login("STUDENT", staffNo);
        Long studentId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE staff_no = ? AND role = 'STUDENT'",
                Long.class, staffNo);

        jdbcTemplate.update(
                "INSERT INTO volunteer_application "
                        + "(student_id, round_no, resume_file_id, intra_lab_adjust, college_adjust, submitted_at) "
                        + "VALUES (?, 1, 5, 1, 1, NOW())",
                studentId);
        Long applicationId = jdbcTemplate.queryForObject(
                "SELECT id FROM volunteer_application WHERE student_id = ? AND round_no = 1",
                Long.class, studentId);

        jdbcTemplate.update(
                "INSERT INTO application_target (application_id, teacher_id, preference_no) VALUES (?, 2, 1)",
                applicationId);
        jdbcTemplate.update(
                "INSERT INTO application_target (application_id, teacher_id, preference_no) VALUES (?, 3, 2)",
                applicationId);
        List<Long> targetIds = jdbcTemplate.queryForList(
                "SELECT id FROM application_target WHERE application_id = ? ORDER BY preference_no",
                Long.class, applicationId);

        jdbcTemplate.update(
                "INSERT INTO offer "
                        + "(teacher_id, student_id, application_target_id, round_no, status, "
                        + "confirm_deadline, granted_at) "
                        + "VALUES (2, ?, ?, 1, 'PENDING', DATE_ADD(NOW(), INTERVAL 1 DAY), NOW())",
                studentId, targetIds.get(0));
        jdbcTemplate.update(
                "INSERT INTO offer "
                        + "(teacher_id, student_id, application_target_id, round_no, status, "
                        + "confirm_deadline, granted_at) "
                        + "VALUES (3, ?, ?, 1, 'PENDING', DATE_ADD(NOW(), INTERVAL 1 DAY), NOW())",
                studentId, targetIds.get(1));
        List<Long> offerIds = jdbcTemplate.queryForList(
                "SELECT id FROM offer WHERE student_id = ? ORDER BY id",
                Long.class, studentId);

        return new StudentOffers(session, studentId, applicationId, targetIds, offerIds);
    }

    protected void setOfferDeadline(Long offerId, LocalDateTime deadline) {
        jdbcTemplate.update(
                "UPDATE offer SET confirm_deadline = ? WHERE id = ?",
                deadline, offerId);
    }

    protected void markOfferConfirmed(Long offerId) {
        jdbcTemplate.update(
                "UPDATE offer SET status = 'CONFIRMED', confirmed_at = NOW(), "
                        + "abandoned_at = NULL, abandon_reason = NULL WHERE id = ?",
                offerId);
    }

    protected void markOfferAbandoned(Long offerId) {
        jdbcTemplate.update(
                "UPDATE offer SET status = 'ABANDONED', abandoned_at = NOW(), "
                        + "confirmed_at = NULL, abandon_reason = 'TIMEOUT' WHERE id = ?",
                offerId);
    }

    protected void insertAdjustmentFinal(Long studentId, Long teacherId) {
        jdbcTemplate.update(
                "INSERT INTO final_admission "
                        + "(student_id, teacher_id, round_no, admission_type, source_offer_id, admitted_at) "
                        + "VALUES (?, ?, NULL, 'ADJUST_COLLEGE', NULL, NOW())",
                studentId, teacherId);
    }

    protected Map<String, Object> offerRow(Long offerId) {
        return jdbcTemplate.queryForMap(
                "SELECT id, teacher_id, student_id, status, confirm_deadline, "
                        + "confirmed_at, abandoned_at, abandon_reason "
                        + "FROM offer WHERE id = ?",
                offerId);
    }

    protected Map<String, Object> finalAdmissionRow(Long studentId) {
        return jdbcTemplate.queryForMap(
                "SELECT id, student_id, teacher_id, round_no, admission_type, source_offer_id, admitted_at "
                        + "FROM final_admission WHERE student_id = ?",
                studentId);
    }

    protected int finalAdmissionCount(Long studentId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM final_admission WHERE student_id = ?",
                Integer.class, studentId);
        return count == null ? 0 : count;
    }

    protected void cleanupStudentData(Long studentId) {
        jdbcTemplate.update("DELETE FROM final_admission WHERE student_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM offer WHERE student_id = ?", studentId);
        jdbcTemplate.update(
                "DELETE FROM application_target WHERE application_id IN "
                        + "(SELECT id FROM volunteer_application WHERE student_id = ?)",
                studentId);
        jdbcTemplate.update("DELETE FROM volunteer_application WHERE student_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", studentId);
    }

    protected LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Shanghai")).truncatedTo(ChronoUnit.MINUTES);
    }
}
