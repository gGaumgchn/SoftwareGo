package com.softwarego.selection.volunteer;

import com.softwarego.selection.stage.domain.StageCode;
import com.softwarego.selectiontest.support.B3TestSupport;
import org.springframework.mock.web.MockHttpSession;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * B4 测试助手：控制 stage 轮次、创建测试学生简历、查询申请/目标、模拟评分/发放。
 */
abstract class VolunteerTestSupport extends B3TestSupport {

    protected static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    protected record StageTimeRow(StageCode stageCode, LocalDateTime startAt, LocalDateTime endAt) {
    }

    protected record StudentContext(MockHttpSession session, Long studentId, Long resumeFileId) {
    }

    protected LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Shanghai")).truncatedTo(ChronoUnit.MINUTES);
    }

    protected StudentContext newStudent(String staffNo) throws Exception {
        MockHttpSession session = login("STUDENT", staffNo);
        Long studentId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE staff_no = ? AND role = 'STUDENT'",
                Long.class, staffNo);
        String storagePath = "2026/11/test-resume-" + staffNo + ".pdf";
        jdbcTemplate.update(
                "INSERT INTO resume_file "
                        + "(owner_id, purpose, original_name, storage_path, content_type, file_size) "
                        + "VALUES (?, 'STUDENT_RESUME', ?, ?, 'application/pdf', 1166)",
                studentId, "测试学生简历-" + staffNo + ".pdf", storagePath);
        Long resumeFileId = jdbcTemplate.queryForObject(
                "SELECT id FROM resume_file WHERE storage_path = ?", Long.class, storagePath);
        return new StudentContext(session, studentId, resumeFileId);
    }

    protected void setRoundOpen(int roundNo) {
        LocalDateTime current = now();
        List<StageTimeRow> rows = switch (roundNo) {
            case 1 -> List.of(
                    row(StageCode.BRIEFING, current.minusHours(10), current.minusHours(9)),
                    row(StageCode.OPEN_DAY, current.minusHours(8), current.minusHours(7)),
                    row(StageCode.ROUND_1, current.minusHours(1), current.plusHours(1)),
                    row(StageCode.ROUND_2, current.plusHours(3), current.plusHours(4)),
                    row(StageCode.ROUND_3, current.plusHours(5), current.plusHours(6)),
                    row(StageCode.ADJUSTMENT, current.plusHours(7), current.plusHours(8)),
                    row(StageCode.ENROLLMENT, current.plusHours(9), current.plusHours(10)));
            case 2 -> List.of(
                    row(StageCode.BRIEFING, current.minusHours(10), current.minusHours(9)),
                    row(StageCode.OPEN_DAY, current.minusHours(8), current.minusHours(7)),
                    row(StageCode.ROUND_1, current.minusHours(6), current.minusHours(5)),
                    row(StageCode.ROUND_2, current.minusHours(1), current.plusHours(1)),
                    row(StageCode.ROUND_3, current.plusHours(3), current.plusHours(4)),
                    row(StageCode.ADJUSTMENT, current.plusHours(5), current.plusHours(6)),
                    row(StageCode.ENROLLMENT, current.plusHours(7), current.plusHours(8)));
            case 3 -> List.of(
                    row(StageCode.BRIEFING, current.minusHours(12), current.minusHours(11)),
                    row(StageCode.OPEN_DAY, current.minusHours(10), current.minusHours(9)),
                    row(StageCode.ROUND_1, current.minusHours(8), current.minusHours(7)),
                    row(StageCode.ROUND_2, current.minusHours(6), current.minusHours(5)),
                    row(StageCode.ROUND_3, current.minusHours(1), current.plusHours(1)),
                    row(StageCode.ADJUSTMENT, current.plusHours(3), current.plusHours(4)),
                    row(StageCode.ENROLLMENT, current.plusHours(5), current.plusHours(6)));
            default -> throw new IllegalArgumentException("roundNo must be 1-3");
        };
        setStageTimes(rows);
    }

    protected void setAllRoundsFuture() {
        LocalDateTime current = now();
        setStageTimes(List.of(
                row(StageCode.BRIEFING, current.plusHours(1), current.plusHours(2)),
                row(StageCode.OPEN_DAY, current.plusHours(3), current.plusHours(4)),
                row(StageCode.ROUND_1, current.plusHours(5), current.plusHours(6)),
                row(StageCode.ROUND_2, current.plusHours(7), current.plusHours(8)),
                row(StageCode.ROUND_3, current.plusHours(9), current.plusHours(10)),
                row(StageCode.ADJUSTMENT, current.plusHours(11), current.plusHours(12)),
                row(StageCode.ENROLLMENT, current.plusHours(13), current.plusHours(14))));
    }

    protected void setAllRoundsPast() {
        LocalDateTime current = now();
        setStageTimes(List.of(
                row(StageCode.BRIEFING, current.minusHours(14), current.minusHours(13)),
                row(StageCode.OPEN_DAY, current.minusHours(12), current.minusHours(11)),
                row(StageCode.ROUND_1, current.minusHours(10), current.minusHours(9)),
                row(StageCode.ROUND_2, current.minusHours(8), current.minusHours(7)),
                row(StageCode.ROUND_3, current.minusHours(6), current.minusHours(5)),
                row(StageCode.ADJUSTMENT, current.minusHours(4), current.minusHours(3)),
                row(StageCode.ENROLLMENT, current.minusHours(2), current.minusHours(1))));
    }

    protected void setNextRoundNotStarted(int nextRound) {
        LocalDateTime current = now();
        List<StageTimeRow> rows = nextRound == 2
                ? List.of(
                        row(StageCode.BRIEFING, current.minusHours(10), current.minusHours(9)),
                        row(StageCode.OPEN_DAY, current.minusHours(8), current.minusHours(7)),
                        row(StageCode.ROUND_1, current.minusHours(6), current.minusHours(5)),
                        row(StageCode.ROUND_2, current.plusHours(1), current.plusHours(2)),
                        row(StageCode.ROUND_3, current.plusHours(3), current.plusHours(4)),
                        row(StageCode.ADJUSTMENT, current.plusHours(5), current.plusHours(6)),
                        row(StageCode.ENROLLMENT, current.plusHours(7), current.plusHours(8)))
                : List.of(
                        row(StageCode.BRIEFING, current.minusHours(12), current.minusHours(11)),
                        row(StageCode.OPEN_DAY, current.minusHours(10), current.minusHours(9)),
                        row(StageCode.ROUND_1, current.minusHours(8), current.minusHours(7)),
                        row(StageCode.ROUND_2, current.minusHours(6), current.minusHours(5)),
                        row(StageCode.ROUND_3, current.plusHours(1), current.plusHours(2)),
                        row(StageCode.ADJUSTMENT, current.plusHours(3), current.plusHours(4)),
                        row(StageCode.ENROLLMENT, current.plusHours(5), current.plusHours(6)));
        setStageTimes(rows);
    }

    protected void clearStageTimes() {
        jdbcTemplate.update("UPDATE stage SET start_at = NULL, end_at = NULL");
    }

    protected List<StageTimeRow> loadStageTimes() {
        return jdbcTemplate.query(
                "SELECT stage_code, start_at, end_at FROM stage ORDER BY sort_no",
                (rs, rowNum) -> new StageTimeRow(
                        StageCode.valueOf(rs.getString("stage_code")),
                        toLocalDateTime(rs.getTimestamp("start_at")),
                        toLocalDateTime(rs.getTimestamp("end_at"))));
    }

    protected void setStageTimes(List<StageTimeRow> rows) {
        for (StageTimeRow row : rows) {
            jdbcTemplate.update(
                    "UPDATE stage SET start_at = ?, end_at = ? WHERE stage_code = ?",
                    row.startAt(), row.endAt(), row.stageCode().name());
        }
    }

    protected void setPlannedQuota(Long teacherId, int plannedQuota) {
        jdbcTemplate.update(
                "UPDATE lab_info SET planned_quota = ? WHERE teacher_id = ?",
                plannedQuota, teacherId);
    }

    protected void setAllPlannedQuota(int plannedQuota) {
        jdbcTemplate.update("UPDATE lab_info SET planned_quota = ?", plannedQuota);
    }

    protected String teacherName(Long teacherId) {
        return jdbcTemplate.queryForObject(
                "SELECT teacher_name FROM lab_info WHERE teacher_id = ?", String.class, teacherId);
    }

    protected void makeTeacherNameDuplicate(Long sourceTeacherId, Long targetTeacherId) {
        String sourceName = teacherName(sourceTeacherId);
        jdbcTemplate.update(
                "UPDATE lab_info SET teacher_name = ? WHERE teacher_id = ?",
                sourceName, targetTeacherId);
    }

    protected String submitBody(List<String> teacherNames,
                                Boolean intraLabAdjust,
                                Boolean collegeAdjust,
                                Long resumeFileId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        if (teacherNames != null) {
            body.put("teacherNames", teacherNames);
        }
        if (intraLabAdjust != null) {
            body.put("intraLabAdjust", intraLabAdjust);
        }
        if (collegeAdjust != null) {
            body.put("collegeAdjust", collegeAdjust);
        }
        if (resumeFileId != null) {
            body.put("resumeFileId", resumeFileId);
        }
        return objectMapper.writeValueAsString(body);
    }

    protected int applicationCount(Long studentId, int roundNo) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM volunteer_application WHERE student_id = ? AND round_no = ?",
                Integer.class, studentId, roundNo);
        return count == null ? 0 : count;
    }

    protected Long applicationId(Long studentId, int roundNo) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM volunteer_application WHERE student_id = ? AND round_no = ?",
                Long.class, studentId, roundNo);
    }

    protected int targetCount(Long applicationId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM application_target WHERE application_id = ?",
                Integer.class, applicationId);
        return count == null ? 0 : count;
    }

    protected List<Long> targetTeacherIds(Long applicationId) {
        return jdbcTemplate.queryForList(
                "SELECT teacher_id FROM application_target WHERE application_id = ? "
                        + "ORDER BY preference_no",
                Long.class, applicationId);
    }

    protected void setScore(Long applicationId, Long teacherId) {
        jdbcTemplate.update(
                "UPDATE application_target SET score = ?, scored_at = NOW() "
                        + "WHERE application_id = ? AND teacher_id = ?",
                new BigDecimal("80.00"), applicationId, teacherId);
    }

    protected void insertPendingOffer(Long applicationId, Long teacherId,
                                      Long studentId, int roundNo) {
        Long targetId = jdbcTemplate.queryForObject(
                "SELECT id FROM application_target WHERE application_id = ? AND teacher_id = ?",
                Long.class, applicationId, teacherId);
        jdbcTemplate.update(
                "INSERT INTO offer "
                        + "(teacher_id, student_id, application_target_id, round_no, status, "
                        + "confirm_deadline, granted_at) "
                        + "VALUES (?, ?, ?, ?, 'PENDING', DATE_ADD(NOW(), INTERVAL 1 DAY), NOW())",
                teacherId, studentId, targetId, roundNo);
    }

    protected void cleanupStudent(Long studentId) {
        jdbcTemplate.update("DELETE FROM offer WHERE student_id = ?", studentId);
        jdbcTemplate.update(
                "DELETE FROM application_target "
                        + "WHERE application_id IN "
                        + "(SELECT id FROM volunteer_application WHERE student_id = ?)",
                studentId);
        jdbcTemplate.update("DELETE FROM volunteer_application WHERE student_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM resume_file WHERE owner_id = ?", studentId);
        jdbcTemplate.update("DELETE FROM sys_user WHERE id = ?", studentId);
    }

    private StageTimeRow row(StageCode stageCode, LocalDateTime startAt, LocalDateTime endAt) {
        return new StageTimeRow(stageCode, startAt, endAt);
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
