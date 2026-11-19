package com.softwarego.selection.todo;

import com.softwarego.selectiontest.support.B3TestSupport;
import org.springframework.mock.web.MockHttpSession;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * B7 测试助手：显式控制阶段/名额，并为导师待办构造本轮申请目标。
 */
abstract class B7TestSupport extends B3TestSupport {

    protected record StudentTarget(MockHttpSession session, Long studentId, Long targetId) {
    }

    protected void setRound1Open() {
        jdbcTemplate.update("UPDATE stage SET start_at = DATE_SUB(NOW(), INTERVAL 30 MINUTE), "
                + "end_at = DATE_ADD(NOW(), INTERVAL 1 HOUR) WHERE stage_code = 'ROUND_1'");
        jdbcTemplate.update("UPDATE stage SET start_at = DATE_ADD(NOW(), INTERVAL 2 HOUR), "
                + "end_at = DATE_ADD(NOW(), INTERVAL 3 HOUR) WHERE stage_code = 'ROUND_2'");
        jdbcTemplate.update("UPDATE stage SET start_at = DATE_ADD(NOW(), INTERVAL 4 HOUR), "
                + "end_at = DATE_ADD(NOW(), INTERVAL 5 HOUR) WHERE stage_code = 'ROUND_3'");
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

    protected void setPlannedQuota(Long teacherId, int plannedQuota) {
        jdbcTemplate.update(
                "UPDATE lab_info SET planned_quota = ? WHERE teacher_id = ?",
                plannedQuota, teacherId);
    }

    protected void setAllPlannedQuota(int plannedQuota) {
        jdbcTemplate.update("UPDATE lab_info SET planned_quota = ?", plannedQuota);
    }

    protected void setStageUnconfigured(String stageCode) {
        jdbcTemplate.update(
                "UPDATE stage SET start_at = NULL, end_at = NULL WHERE stage_code = ?",
                stageCode);
    }

    protected void setOfferDeadlineInMinutes(Long offerId, long minutes) {
        LocalDateTime deadline = LocalDateTime.now(ZoneId.of("Asia/Shanghai"))
                .truncatedTo(ChronoUnit.SECONDS)
                .plusMinutes(minutes);
        jdbcTemplate.update("UPDATE offer SET confirm_deadline = ? WHERE id = ?", deadline, offerId);
    }

    protected StudentTarget createStudentTarget(String staffNo, int roundNo,
                                                Long teacherId, BigDecimal score) throws Exception {
        MockHttpSession session = login("STUDENT", staffNo);
        Long studentId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE staff_no = ? AND role = 'STUDENT'",
                Long.class, staffNo);

        jdbcTemplate.update(
                "INSERT INTO volunteer_application "
                        + "(student_id, round_no, resume_file_id, intra_lab_adjust, college_adjust, submitted_at) "
                        + "VALUES (?, ?, 5, 1, 1, NOW())",
                studentId, roundNo);
        Long applicationId = jdbcTemplate.queryForObject(
                "SELECT id FROM volunteer_application WHERE student_id = ? AND round_no = ?",
                Long.class, studentId, roundNo);
        jdbcTemplate.update(
                "INSERT INTO application_target (application_id, teacher_id, preference_no, score) "
                        + "VALUES (?, ?, 1, ?)",
                applicationId, teacherId, score);
        Long targetId = jdbcTemplate.queryForObject(
                "SELECT id FROM application_target WHERE application_id = ? AND teacher_id = ?",
                Long.class, applicationId, teacherId);
        return new StudentTarget(session, studentId, targetId);
    }

    protected int countTodos(com.fasterxml.jackson.databind.JsonNode body) {
        return body.path("data").path("list").size();
    }
}
