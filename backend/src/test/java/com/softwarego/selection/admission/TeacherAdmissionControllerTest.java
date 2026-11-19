package com.softwarego.selection.admission;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * B5 导师申请/评分/同分顺序/名额概览/发放最小回归矩阵。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TeacherAdmissionControllerTest extends AdmissionTestSupport {

    private static final long TEACHER2 = 2L;
    private static final long TEACHER3 = 3L;

    @Test
    void applicationsListOwnRoundSortedAndEditableFlags() throws Exception {
        setRound2Open();
        setPlannedQuota(TEACHER3, 10);
        StudentTarget newTarget = createStudentTarget("2026999990", TEACHER3, new BigDecimal("95.00"));

        mockMvc.perform(get("/api/v1/teacher/applications")
                        .param("roundNo", "2")
                        .session(teacher3Session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roundNo").value(2))
                .andExpect(jsonPath("$.data.roundState").value("OPEN"))
                .andExpect(jsonPath("$.data.canScore").value(true))
                .andExpect(jsonPath("$.data.canGrant").value(true))
                .andExpect(jsonPath("$.data.list", hasSize(2)))
                .andExpect(jsonPath("$.data.list[0].targetId").value(newTarget.targetId().intValue()))
                .andExpect(jsonPath("$.data.list[0].score").value(95.0))
                .andExpect(jsonPath("$.data.list[0].scoreEditable").value(true))
                .andExpect(jsonPath("$.data.list[0].tieOrderEditable").value(true))
                .andExpect(jsonPath("$.data.list[0].offerId").value(nullValue()))
                .andExpect(jsonPath("$.data.list[1].score").value(79.0))
                .andExpect(jsonPath("$.data.list[*].targetId", not(hasItem(15))));
    }

    @Test
    void applicationsRound1PastIsReadOnly() throws Exception {
        setRound2Open();

        mockMvc.perform(get("/api/v1/teacher/applications")
                        .param("roundNo", "1")
                        .session(teacher3Session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roundState").value("ENDED"))
                .andExpect(jsonPath("$.data.canScore").value(false))
                .andExpect(jsonPath("$.data.canGrant").value(false));
    }

    @Test
    void applicationsWithoutCurrentRoundRejected() throws Exception {
        setRound2Ended();

        mockMvc.perform(get("/api/v1/teacher/applications").session(teacher3Session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("当前不在录取轮次"));
    }

    @Test
    void applicationsInvalidRoundNoRejected() throws Exception {
        mockMvc.perform(get("/api/v1/teacher/applications")
                        .param("roundNo", "4")
                        .session(teacher3Session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("roundNo 非法"));
    }

    @Test
    void applicationsLazilyExpireOwnPastPendingOffer() throws Exception {
        setRound2Open();
        setPlannedQuota(TEACHER3, 10);
        StudentTarget target = createStudentTarget("2026999990", TEACHER3, new BigDecimal("80.00"));
        insertPendingOffer(TEACHER3, target, now().minusHours(1));

        MvcResult result = mockMvc.perform(get("/api/v1/teacher/applications")
                        .param("roundNo", "2")
                        .session(teacher3Session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(2)))
                .andReturn();

        var row = json(result).path("data").path("list");
        for (var item : row) {
            if (item.path("targetId").asLong() == target.targetId()) {
                assertThat(item.path("offerStatus").asText()).isEqualTo("ABANDONED");
                assertThat(item.path("scoreEditable").asBoolean()).isFalse();
                assertThat(item.path("tieOrderEditable").asBoolean()).isFalse();
            }
        }
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM offer WHERE application_target_id = ?", String.class,
                target.targetId())).isEqualTo("ABANDONED");
    }

    @Test
    void scoreUpdatesAndClearsTieOrder() throws Exception {
        setRound2Open();
        StudentTarget target = createStudentTarget("2026999990", TEACHER3, new BigDecimal("80.00"));
        jdbcTemplate.update("UPDATE application_target SET tie_order = 1 WHERE id = ?", target.targetId());

        mockMvc.perform(put("/api/v1/teacher/applications/{id}/score", target.targetId())
                        .session(teacher3Session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("score", new BigDecimal("95.5")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targetId").value(target.targetId().intValue()))
                .andExpect(jsonPath("$.data.score").value(95.5))
                .andExpect(jsonPath("$.data.tieOrder").value(nullValue()))
                .andExpect(jsonPath("$.data.scoredAt").value(notNullValue()));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT score, tie_order, scored_at FROM application_target WHERE id = ?",
                target.targetId());
        assertThat(((BigDecimal) row.get("score")).compareTo(new BigDecimal("95.50"))).isZero();
        assertThat(row.get("tie_order")).isNull();
        assertThat(row.get("scored_at")).isNotNull();
    }

    @Test
    void scoreRejectsInvalidValue() throws Exception {
        setRound2Open();
        StudentTarget target = createStudentTarget("2026999990", TEACHER3, null);

        for (String body : new String[]{
                "{\"score\":-1}", "{\"score\":101}", "{\"score\":95.555}", "{}"}) {
            mockMvc.perform(put("/api/v1/teacher/applications/{id}/score", target.targetId())
                            .session(teacher3Session())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(40001))
                    .andExpect(jsonPath("$.message").value("请填写有效分数"));
        }
    }

    @Test
    void scoreRejectsEndedRoundAndOfferTarget() throws Exception {
        StudentTarget target = createStudentTarget("2026999990", TEACHER3, null);
        setRound2Ended();

        mockMvc.perform(put("/api/v1/teacher/applications/{id}/score", target.targetId())
                        .session(teacher3Session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":90}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("本轮已结束"));
    }

    @Test
    void scoreRejectsOfferTarget() throws Exception {
        setRound2Open();
        Long targetId = targetIdOf(TEACHER2, 15L);

        mockMvc.perform(put("/api/v1/teacher/applications/{id}/score", targetId)
                        .session(teacherSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":90}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("已发放拟录取，分数不可修改"));
    }

    @Test
    void scoreOrderOwnershipAndNotFound() throws Exception {
        Long targetId = targetIdOf(TEACHER2, 15L);

        mockMvc.perform(put("/api/v1/teacher/applications/{id}/score", targetId)
                        .session(teacher3Session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":90}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));

        mockMvc.perform(put("/api/v1/teacher/applications/{id}/order", targetId)
                        .session(teacher3Session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tieOrder\":1}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));

        mockMvc.perform(put("/api/v1/teacher/applications/{id}/score", 999999L)
                        .session(teacher3Session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":90}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void orderUpdatesAndRejectsDuplicates() throws Exception {
        setRound2Open();
        StudentTarget first = createStudentTarget("2026999990", TEACHER3, new BigDecimal("80.00"));
        StudentTarget second = createStudentTarget("2026999991", TEACHER3, new BigDecimal("80.00"));

        mockMvc.perform(put("/api/v1/teacher/applications/{id}/order", first.targetId())
                        .session(teacher3Session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tieOrder\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tieOrder").value(1))
                .andExpect(jsonPath("$.data.score").value(80.0));

        mockMvc.perform(put("/api/v1/teacher/applications/{id}/order", second.targetId())
                        .session(teacher3Session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tieOrder\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("同分顺序不能重复"));
    }

    @Test
    void orderRequiresScoreAndRejectsInvalidOrOffer() throws Exception {
        setRound2Open();
        StudentTarget noScore = createStudentTarget("2026999990", TEACHER3, null);

        mockMvc.perform(put("/api/v1/teacher/applications/{id}/order", noScore.targetId())
                        .session(teacher3Session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tieOrder\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("请先录入面试分数"));

        mockMvc.perform(put("/api/v1/teacher/applications/{id}/order", targetIdOf(TEACHER2, 15L))
                        .session(teacherSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tieOrder\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("已发放拟录取，顺序不可修改"));
    }

    @Test
    void summaryCountsQuota() throws Exception {
        setRound2Open();
        long admitted = countFinalAdmissions(TEACHER2);
        long pending = countPendingOffers(TEACHER2);

        mockMvc.perform(get("/api/v1/teacher/admissions").session(teacherSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roundNo").value(2))
                .andExpect(jsonPath("$.data.roundState").value("OPEN"))
                .andExpect(jsonPath("$.data.labSubmitted").value(true))
                .andExpect(jsonPath("$.data.plannedQuota").value(3))
                .andExpect(jsonPath("$.data.admittedCount").value((int) admitted))
                .andExpect(jsonPath("$.data.pendingCount").value((int) pending))
                .andExpect(jsonPath("$.data.remainingQuota").value((int) (3 - admitted - pending)))
                .andExpect(jsonPath("$.data.canGrant").value(true));
    }

    @Test
    void summaryNoLab() throws Exception {
        setRound2Open();
        MockHttpSession newTeacher = login("TEACHER", "2026999995");

        mockMvc.perform(get("/api/v1/teacher/admissions").session(newTeacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.labSubmitted").value(false))
                .andExpect(jsonPath("$.data.plannedQuota").value(nullValue()))
                .andExpect(jsonPath("$.data.remainingQuota").value(nullValue()))
                .andExpect(jsonPath("$.data.canGrant").value(false));
    }

    @Test
    void grantSuccessAndQuotaDrops() throws Exception {
        setRound2Open();
        setPlannedQuota(TEACHER3, 10);
        long admitted = countFinalAdmissions(TEACHER3);
        long pending = countPendingOffers(TEACHER3);
        StudentTarget target = createStudentTarget("2026999990", TEACHER3, new BigDecimal("95.00"));

        mockMvc.perform(post("/api/v1/teacher/admissions/grant")
                        .session(teacher3Session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cutoffScore\":90}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roundNo").value(2))
                .andExpect(jsonPath("$.data.grantedCount").value(1))
                .andExpect(jsonPath("$.data.remainingQuota").value((int) (10 - admitted - pending - 1)))
                .andExpect(jsonPath("$.data.offers[0].studentId").value(target.studentId().intValue()))
                .andExpect(jsonPath("$.data.offers[0].status").value("PENDING"));

        Map<String, Object> offer = offerByTarget(target.targetId());
        assertThat(offer.get("status")).isEqualTo("PENDING");
        LocalDateTime roundEnd = jdbcTemplate.queryForObject(
                "SELECT end_at FROM stage WHERE stage_code = 'ROUND_2'",
                LocalDateTime.class);
        assertThat(offer.get("confirm_deadline")).isEqualTo(roundEnd);

        mockMvc.perform(get("/api/v1/teacher/admissions").session(teacher3Session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendingCount").value((int) (pending + 1)))
                .andExpect(jsonPath("$.data.remainingQuota").value((int) (10 - admitted - pending - 1)));
    }

    @Test
    void grantRespectsRemainingQuota() throws Exception {
        setRound2Open();
        setPlannedQuota(TEACHER3, (int) (countFinalAdmissions(TEACHER3) + countPendingOffers(TEACHER3)) + 1);
        StudentTarget first = createStudentTarget("2026999990", TEACHER3, new BigDecimal("95.00"));
        StudentTarget second = createStudentTarget("2026999991", TEACHER3, new BigDecimal("94.00"));

        mockMvc.perform(post("/api/v1/teacher/admissions/grant")
                        .session(teacher3Session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cutoffScore\":90}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.grantedCount").value(1))
                .andExpect(jsonPath("$.data.remainingQuota").value(0));

        assertThat(countOffersByTarget(first.targetId()) + countOffersByTarget(second.targetId()))
                .isEqualTo(1);
    }

    @Test
    void grantRejectsNoCandidatesNoLabCutoffAndEndedRound() throws Exception {
        setRound2Open();
        setPlannedQuota(TEACHER3, 10);

        mockMvc.perform(post("/api/v1/teacher/admissions/grant")
                        .session(teacher3Session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cutoffScore\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("暂无可发放的达标学生"));

        MockHttpSession noLab = login("TEACHER", "2026999995");
        mockMvc.perform(post("/api/v1/teacher/admissions/grant")
                        .session(noLab)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cutoffScore\":90}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("请先提交实训室信息"));

        mockMvc.perform(post("/api/v1/teacher/admissions/grant")
                        .session(teacher3Session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cutoffScore\":100.001}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("请填写有效分数"));
    }

    @Test
    void grantAfterAllRoundsEndedRejected() throws Exception {
        setAllRoundsEnded();

        mockMvc.perform(post("/api/v1/teacher/admissions/grant")
                        .session(teacher3Session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cutoffScore\":90}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("本轮拟录取发放已截止"));
    }

    @Test
    void grantTieUnresolvedBlocksWholeBatch() throws Exception {
        setRound2Open();
        setPlannedQuota(TEACHER3, 10);
        StudentTarget first = createStudentTarget("2026999990", TEACHER3, new BigDecimal("95.00"));
        StudentTarget second = createStudentTarget("2026999991", TEACHER3, new BigDecimal("95.00"));

        mockMvc.perform(post("/api/v1/teacher/admissions/grant")
                        .session(teacher3Session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cutoffScore\":90}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("请先确定同分学生顺序"));

        assertThat(countOffersByTarget(first.targetId())).isZero();
        assertThat(countOffersByTarget(second.targetId())).isZero();
    }

    @Test
    void grantAfterTieOrderGrantsBoth() throws Exception {
        setRound2Open();
        setPlannedQuota(TEACHER3, 10);
        StudentTarget first = createStudentTarget("2026999990", TEACHER3, new BigDecimal("95.00"));
        StudentTarget second = createStudentTarget("2026999991", TEACHER3, new BigDecimal("95.00"));
        setTieOrder(first.targetId(), 1);
        setTieOrder(second.targetId(), 2);

        mockMvc.perform(post("/api/v1/teacher/admissions/grant")
                        .session(teacher3Session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cutoffScore\":90}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.grantedCount").value(2))
                .andExpect(jsonPath("$.data.offers[0].studentId").value(first.studentId().intValue()))
                .andExpect(jsonPath("$.data.offers[1].studentId").value(second.studentId().intValue()));
    }

    @Test
    void grantExcludesStudentWithFinalAdmission() throws Exception {
        setRound2Open();
        setPlannedQuota(TEACHER3, 10);
        StudentTarget target = createStudentTarget("2026999990", TEACHER3, new BigDecimal("95.00"));
        insertAdjustmentFinal(target.studentId(), TEACHER2);

        mockMvc.perform(get("/api/v1/teacher/applications")
                        .param("roundNo", "2")
                        .session(teacher3Session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].targetId").value(target.targetId().intValue()))
                .andExpect(jsonPath("$.data.list[0].studentAdmitted").value(true));

        mockMvc.perform(post("/api/v1/teacher/admissions/grant")
                        .session(teacher3Session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cutoffScore\":90}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("暂无可发放的达标学生"));
    }

    @Test
    void grantExcludesTargetWithExistingOffer() throws Exception {
        setRound2Open();

        mockMvc.perform(post("/api/v1/teacher/admissions/grant")
                        .session(teacherSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cutoffScore\":90}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("暂无可发放的达标学生"));
    }

    private MockHttpSession teacher3Session() throws Exception {
        return login("TEACHER", "1000000002");
    }

    private Long targetIdOf(long teacherId, long seedTargetId) {
        Long actual = jdbcTemplate.queryForObject(
                "SELECT id FROM application_target WHERE id = ? AND teacher_id = ?",
                Long.class, seedTargetId, teacherId);
        return actual;
    }

    private void insertPendingOffer(long teacherId, StudentTarget target, LocalDateTime deadline) {
        jdbcTemplate.update(
                "INSERT INTO offer (teacher_id, student_id, application_target_id, round_no, "
                        + "status, confirm_deadline, granted_at) "
                        + "VALUES (?, ?, ?, 2, 'PENDING', ?, ?)",
                teacherId, target.studentId(), target.targetId(), deadline, deadline);
    }

    private void setTieOrder(Long targetId, int tieOrder) throws Exception {
        mockMvc.perform(put("/api/v1/teacher/applications/{id}/order", targetId)
                        .session(teacher3Session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tieOrder\":" + tieOrder + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private static org.hamcrest.Matcher<Object> notNullValue() {
        return org.hamcrest.Matchers.notNullValue(Object.class);
    }
}
