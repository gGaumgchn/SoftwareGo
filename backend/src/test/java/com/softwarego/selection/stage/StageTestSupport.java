package com.softwarego.selection.stage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.softwarego.selection.stage.domain.StageCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * B2 测试公共助手：登录种子账号、构造/恢复 7 个阶段时间、清理测试公告与变更记录。
 */
abstract class StageTestSupport {

    protected static final String MANAGER_NO = "9000000001";
    protected static final String STUDENT_NO = "2026001001";
    protected static final String TEACHER_NO = "1000000001";
    protected static final String TEST_CHANGE_PREFIX = "b2test-";
    protected static final String TEST_TITLE_PREFIX = "[B2TEST]";

    protected static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected record StageTimeRow(StageCode stageCode, LocalDateTime startAt, LocalDateTime endAt) {
    }

    protected MockHttpSession managerSession() throws Exception {
        return login("MANAGER", MANAGER_NO);
    }

    protected MockHttpSession studentSession() throws Exception {
        return login("STUDENT", STUDENT_NO);
    }

    protected MockHttpSession teacherSession() throws Exception {
        return login("TEACHER", TEACHER_NO);
    }

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

    protected LocalDateTime now() {
        return LocalDateTime.now(ZoneId.of("Asia/Shanghai")).truncatedTo(ChronoUnit.MINUTES);
    }

    protected List<StageTimeRow> sequentialRows(LocalDateTime base) {
        List<StageTimeRow> rows = new ArrayList<>();
        LocalDateTime cursor = base;
        for (StageCode code : StageCode.values()) {
            rows.add(new StageTimeRow(code, cursor, cursor.plusHours(1)));
            cursor = cursor.plusHours(1).plusMinutes(30);
        }
        return rows;
    }

    protected List<StageTimeRow> ongoingRows(LocalDateTime now) {
        return List.of(
                new StageTimeRow(StageCode.BRIEFING, now.minusHours(4), now.minusHours(3)),
                new StageTimeRow(StageCode.OPEN_DAY, now.minusHours(2), now.plusHours(2)),
                new StageTimeRow(StageCode.ROUND_1, now.plusHours(3), now.plusHours(4)),
                new StageTimeRow(StageCode.ROUND_2, now.plusHours(5), now.plusHours(6)),
                new StageTimeRow(StageCode.ROUND_3, now.plusHours(7), now.plusHours(8)),
                new StageTimeRow(StageCode.ADJUSTMENT, now.plusHours(9), now.plusHours(10)),
                new StageTimeRow(StageCode.ENROLLMENT, now.plusHours(11), now.plusHours(12)));
    }

    protected List<StageTimeRow> betweenRows(LocalDateTime now) {
        return List.of(
                new StageTimeRow(StageCode.BRIEFING, now.minusHours(4), now.minusHours(2)),
                new StageTimeRow(StageCode.OPEN_DAY, now.plusHours(2), now.plusHours(4)),
                new StageTimeRow(StageCode.ROUND_1, now.plusHours(5), now.plusHours(6)),
                new StageTimeRow(StageCode.ROUND_2, now.plusHours(7), now.plusHours(8)),
                new StageTimeRow(StageCode.ROUND_3, now.plusHours(9), now.plusHours(10)),
                new StageTimeRow(StageCode.ADJUSTMENT, now.plusHours(11), now.plusHours(12)),
                new StageTimeRow(StageCode.ENROLLMENT, now.plusHours(13), now.plusHours(14)));
    }

    protected List<StageTimeRow> endedRows(LocalDateTime now) {
        return List.of(
                new StageTimeRow(StageCode.BRIEFING, now.minusHours(14), now.minusHours(13)),
                new StageTimeRow(StageCode.OPEN_DAY, now.minusHours(12), now.minusHours(11)),
                new StageTimeRow(StageCode.ROUND_1, now.minusHours(10), now.minusHours(9)),
                new StageTimeRow(StageCode.ROUND_2, now.minusHours(8), now.minusHours(7)),
                new StageTimeRow(StageCode.ROUND_3, now.minusHours(6), now.minusHours(5)),
                new StageTimeRow(StageCode.ADJUSTMENT, now.minusHours(4), now.minusHours(3)),
                new StageTimeRow(StageCode.ENROLLMENT, now.minusHours(2), now.minusHours(1)));
    }

    protected List<StageTimeRow> briefingEndedRows(LocalDateTime now) {
        return List.of(
                new StageTimeRow(StageCode.BRIEFING, now.minusHours(4), now.minusHours(2)),
                new StageTimeRow(StageCode.OPEN_DAY, now.minusHours(1), now.plusHours(1)),
                new StageTimeRow(StageCode.ROUND_1, now.plusHours(2), now.plusHours(3)),
                new StageTimeRow(StageCode.ROUND_2, now.plusHours(4), now.plusHours(5)),
                new StageTimeRow(StageCode.ROUND_3, now.plusHours(6), now.plusHours(7)),
                new StageTimeRow(StageCode.ADJUSTMENT, now.plusHours(8), now.plusHours(9)),
                new StageTimeRow(StageCode.ENROLLMENT, now.plusHours(10), now.plusHours(11)));
    }

    protected void setStageTimes(List<StageTimeRow> rows) {
        for (StageTimeRow row : rows) {
            jdbcTemplate.update(
                    "UPDATE stage SET start_at = ?, end_at = ? WHERE stage_code = ?",
                    row.startAt(), row.endAt(), row.stageCode().name());
        }
    }

    protected List<StageTimeRow> loadStageTimes() {
        return jdbcTemplate.query(
                "SELECT stage_code, start_at, end_at FROM stage ORDER BY sort_no",
                (rs, rowNum) -> new StageTimeRow(
                        StageCode.valueOf(rs.getString("stage_code")),
                        toLocalDateTime(rs.getTimestamp("start_at")),
                        toLocalDateTime(rs.getTimestamp("end_at"))));
    }

    protected LocalDateTime stageStart(StageCode stageCode) {
        Timestamp timestamp = jdbcTemplate.queryForObject(
                "SELECT start_at FROM stage WHERE stage_code = ?", Timestamp.class, stageCode.name());
        return toLocalDateTime(timestamp);
    }

    protected LocalDateTime stageEnd(StageCode stageCode) {
        Timestamp timestamp = jdbcTemplate.queryForObject(
                "SELECT end_at FROM stage WHERE stage_code = ?", Timestamp.class, stageCode.name());
        return toLocalDateTime(timestamp);
    }

    protected String format(LocalDateTime value) {
        return DATE_TIME_FORMATTER.format(value);
    }

    protected List<Map<String, Object>> stageMaps(List<StageTimeRow> rows) {
        List<Map<String, Object>> stages = new ArrayList<>();
        for (StageTimeRow row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("stageCode", row.stageCode().name());
            item.put("startAt", format(row.startAt()));
            item.put("endAt", format(row.endAt()));
            stages.add(item);
        }
        return stages;
    }

    protected String saveBody(String changeNo,
                              List<StageTimeRow> rows,
                              Boolean publishAnnouncement,
                              String title,
                              String content) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        if (changeNo != null) {
            body.put("changeNo", changeNo);
        }
        body.put("stages", stageMaps(rows));
        if (publishAnnouncement != null) {
            body.put("publishAnnouncement", publishAnnouncement);
        }
        if (Boolean.TRUE.equals(publishAnnouncement)) {
            Map<String, String> announcement = new LinkedHashMap<>();
            if (title != null) {
                announcement.put("title", title);
            }
            if (content != null) {
                announcement.put("content", content);
            }
            body.put("announcement", announcement);
        }
        return objectMapper.writeValueAsString(body);
    }

    protected void deleteTestStageChanges() {
        jdbcTemplate.update("DELETE FROM stage_change WHERE change_no LIKE ?", TEST_CHANGE_PREFIX + "%");
    }

    protected void deleteTestAnnouncements() {
        jdbcTemplate.update("DELETE FROM announcement WHERE title LIKE ?", TEST_TITLE_PREFIX + "%");
    }

    protected int countAnnouncementsByTitle(String title) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM announcement WHERE title = ?", Integer.class, title);
        return count == null ? 0 : count;
    }

    protected long announcementIdByTitle(String title) {
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM announcement WHERE title = ? ORDER BY id DESC LIMIT 1",
                Long.class, title);
        return id == null ? -1L : id;
    }

    protected String stageChangeStatus(String changeNo) {
        return jdbcTemplate.queryForObject(
                "SELECT announcement_status FROM stage_change WHERE change_no = ?",
                String.class, changeNo);
    }

    protected int countStageChangesByChangeNo(String changeNo) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM stage_change WHERE change_no = ?",
                Integer.class, changeNo);
        return count == null ? 0 : count;
    }

    protected String newChangeNo() {
        return TEST_CHANGE_PREFIX + java.util.UUID.randomUUID();
    }

    protected long maxAnnouncementId() {
        Long id = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(id), 0) FROM announcement", Long.class);
        return id == null ? 0L : id;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
