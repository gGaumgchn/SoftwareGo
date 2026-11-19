package com.softwarego.selection.volunteer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 志愿提交/重填：1–3 名、空白、重复、调剂、简历归属、轮次、名额、已录取、已处理。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VolunteerSubmitControllerTest extends VolunteerTestSupport {

    @Test
    void submitRound1WithThreeTeachers() throws Exception {
        setRoundOpen(1);
        StudentContext student = newStudent("2026999994");
        List<String> names = List.of(teacherName(2L), teacherName(3L), teacherName(4L));

        MvcResult result = mockMvc.perform(post("/api/v1/student/volunteers")
                        .session(student.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(names, true, false, student.resumeFileId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.applicationId").value(notNullValue()))
                .andExpect(jsonPath("$.data.roundNo").value(1))
                .andExpect(jsonPath("$.data.submittedAt").value(notNullValue()))
                .andExpect(jsonPath("$.data.intraLabAdjust").value(true))
                .andExpect(jsonPath("$.data.collegeAdjust").value(false))
                .andExpect(jsonPath("$.data.targets", hasSize(3)))
                .andExpect(jsonPath("$.data.targets[0].preferenceNo").value(1))
                .andExpect(jsonPath("$.data.targets[0].teacherName").value(teacherName(2L)))
                .andExpect(jsonPath("$.data.targets[1].teacherName").value(teacherName(3L)))
                .andExpect(jsonPath("$.data.targets[2].teacherName").value(teacherName(4L)))
                .andExpect(jsonPath("$.data.message").value("提交成功"))
                .andReturn();

        Long applicationId = json(result).path("data").path("applicationId").asLong();
        assertThat(applicationCount(student.studentId(), 1)).isEqualTo(1);
        assertThat(targetCount(applicationId)).isEqualTo(3);
        assertThat(targetTeacherIds(applicationId)).containsExactly(2L, 3L, 4L);

        mockMvc.perform(get("/api/v1/student/volunteers/current").session(student.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canSubmit").value(true))
                .andExpect(jsonPath("$.data.application.applicationId").value(applicationId.intValue()))
                .andExpect(jsonPath("$.data.application.resumeFileName")
                        .value("测试学生简历-2026999994.pdf"))
                .andExpect(jsonPath("$.data.application.targets", hasSize(3)));

        mockMvc.perform(get("/api/v1/student/volunteers").session(student.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(1)))
                .andExpect(jsonPath("$.data.list[0].roundNo").value(1));
    }

    @Test
    void submitRound1IgnoresBlankNames() throws Exception {
        setRoundOpen(1);
        StudentContext student = newStudent("2026999994");

        mockMvc.perform(post("/api/v1/student/volunteers")
                        .session(student.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(
                                List.of("   ", teacherName(2L)), true, true,
                                student.resumeFileId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targets", hasSize(1)))
                .andExpect(jsonPath("$.data.targets[0].teacherName").value(teacherName(2L)));

        mockMvc.perform(post("/api/v1/student/volunteers")
                        .session(student.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(
                                List.of("   "), true, true, student.resumeFileId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("提交失败，请重试"));
    }

    @Test
    void submitRound1RequiresNamesAndAdjust() throws Exception {
        setRoundOpen(1);
        StudentContext student = newStudent("2026999994");
        MockHttpSession session = student.session();
        Long resumeFileId = student.resumeFileId();
        String name2 = teacherName(2L);
        String name3 = teacherName(3L);
        String name4 = teacherName(4L);
        String name5 = teacherName(5L);

        // 0 名
        mockMvc.perform(post("/api/v1/student/volunteers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(List.of(), true, true, resumeFileId)))
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("提交失败，请重试"));

        // 4 名
        mockMvc.perform(post("/api/v1/student/volunteers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(
                                List.of(name2, name3, name4, name5), true, true, resumeFileId)))
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("提交失败，请重试"));

        // 重复
        mockMvc.perform(post("/api/v1/student/volunteers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(List.of(name2, name2), true, true, resumeFileId)))
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("导师姓名不能重复"));

        // 缺调剂意愿
        mockMvc.perform(post("/api/v1/student/volunteers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(List.of(name2), null, true, resumeFileId)))
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("提交失败，请重试"));

        // 缺简历
        mockMvc.perform(post("/api/v1/student/volunteers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(List.of(name2), true, true, null)))
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("提交失败，请重试"));

        assertThat(applicationCount(student.studentId(), 1)).isZero();
    }

    @Test
    void submitRound1RejectsResumeNotOwned() throws Exception {
        setRoundOpen(1);
        StudentContext student = newStudent("2026999994");
        StudentContext other = newStudent("2026999993");

        mockMvc.perform(post("/api/v1/student/volunteers")
                        .session(student.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(
                                List.of(teacherName(2L)), true, true, other.resumeFileId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301))
                .andExpect(jsonPath("$.message").value("无权访问该数据"));

        mockMvc.perform(post("/api/v1/student/volunteers")
                        .session(student.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(List.of(teacherName(2L)), true, true, 1L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void submitRejectsUnknownOrDuplicateTeacherName() throws Exception {
        setRoundOpen(1);
        StudentContext student = newStudent("2026999994");

        mockMvc.perform(post("/api/v1/student/volunteers")
                        .session(student.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(
                                List.of("不存在的导师"), true, true, student.resumeFileId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message")
                        .value("导师姓名不存在或存在重名，请联系负责人"));

        makeTeacherNameDuplicate(2L, 3L);

        mockMvc.perform(post("/api/v1/student/volunteers")
                        .session(student.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(
                                List.of(teacherName(2L)), true, true,
                                student.resumeFileId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message")
                        .value("导师姓名不存在或存在重名，请联系负责人"));
    }

    @Test
    void admittedStudentCannotSubmit() throws Exception {
        setRoundOpen(1);

        mockMvc.perform(post("/api/v1/student/volunteers")
                        .session(studentSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(List.of(teacherName(2L)), true, true, 5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("你已确认录取，无需参加补录"));
    }

    @Test
    void submitRound2NoQuota() throws Exception {
        setRoundOpen(2);
        setAllPlannedQuota(1);
        StudentContext student = newStudent("2026999994");

        mockMvc.perform(post("/api/v1/student/volunteers")
                        .session(student.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(
                                List.of(teacherName(2L)), true, true,
                                student.resumeFileId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("本轮暂无可填报名额"));
    }

    @Test
    void submitRound2SpecificTeacherNoQuota() throws Exception {
        setRoundOpen(2);
        setAllPlannedQuota(1);
        setPlannedQuota(2L, 3);
        StudentContext student = newStudent("2026999994");

        mockMvc.perform(post("/api/v1/student/volunteers")
                        .session(student.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(
                                List.of(teacherName(3L)), true, true,
                                student.resumeFileId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("所填导师已无剩余名额，请重新选择"));
    }

    @Test
    void submitRound2Success() throws Exception {
        setRoundOpen(2);
        setAllPlannedQuota(1);
        setPlannedQuota(2L, 3);
        StudentContext student = newStudent("2026999994");

        mockMvc.perform(post("/api/v1/student/volunteers")
                        .session(student.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(
                                List.of(teacherName(2L)), true, true,
                                student.resumeFileId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.roundNo").value(2))
                .andExpect(jsonPath("$.data.targets", hasSize(1)))
                .andExpect(jsonPath("$.data.targets[0].teacherId").value(2));

        assertThat(applicationCount(student.studentId(), 2)).isEqualTo(1);
        assertThat(applicationCount(student.studentId(), 1)).isZero();
    }

    @Test
    void resubmitSameRoundReplacesTargets() throws Exception {
        setRoundOpen(1);
        StudentContext student = newStudent("2026999994");
        MockHttpSession session = student.session();

        MvcResult first = mockMvc.perform(post("/api/v1/student/volunteers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(
                                List.of(teacherName(2L)), true, true,
                                student.resumeFileId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        Long applicationId = json(first).path("data").path("applicationId").asLong();

        mockMvc.perform(post("/api/v1/student/volunteers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(
                                List.of(teacherName(3L)), false, true,
                                student.resumeFileId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.applicationId").value(applicationId.intValue()))
                .andExpect(jsonPath("$.data.targets", hasSize(1)))
                .andExpect(jsonPath("$.data.targets[0].teacherId").value(3));

        assertThat(applicationCount(student.studentId(), 1)).isEqualTo(1);
        assertThat(targetTeacherIds(applicationId)).containsExactly(3L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT intra_lab_adjust FROM volunteer_application WHERE id = ?",
                Integer.class, applicationId)).isZero();

        mockMvc.perform(get("/api/v1/student/volunteers").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(1)))
                .andExpect(jsonPath("$.data.list[0].targets[0].teacherId").value(3));
    }

    @Test
    void processedAfterScoreRejectsResubmit() throws Exception {
        setRoundOpen(1);
        StudentContext student = newStudent("2026999994");
        MockHttpSession session = student.session();

        MvcResult first = mockMvc.perform(post("/api/v1/student/volunteers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(
                                List.of(teacherName(2L)), true, true,
                                student.resumeFileId())))
                .andExpect(status().isOk())
                .andReturn();
        Long applicationId = json(first).path("data").path("applicationId").asLong();

        setScore(applicationId, 2L);

        mockMvc.perform(get("/api/v1/student/volunteers/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canSubmit").value(false))
                .andExpect(jsonPath("$.data.blockedReason").value("PROCESSED"));

        mockMvc.perform(post("/api/v1/student/volunteers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(
                                List.of(teacherName(3L)), true, true,
                                student.resumeFileId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message")
                        .value("本轮志愿已进入处理阶段，不能再修改"));
    }

    @Test
    void processedAfterOfferRejectsResubmit() throws Exception {
        setRoundOpen(1);
        StudentContext student = newStudent("2026999994");
        MockHttpSession session = student.session();

        MvcResult first = mockMvc.perform(post("/api/v1/student/volunteers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(
                                List.of(teacherName(2L)), true, true,
                                student.resumeFileId())))
                .andExpect(status().isOk())
                .andReturn();
        Long applicationId = json(first).path("data").path("applicationId").asLong();

        insertPendingOffer(applicationId, 2L, student.studentId(), 1);

        mockMvc.perform(get("/api/v1/student/volunteers/current").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canSubmit").value(false))
                .andExpect(jsonPath("$.data.blockedReason").value("PROCESSED"));

        mockMvc.perform(post("/api/v1/student/volunteers")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(
                                List.of(teacherName(3L)), true, true,
                                student.resumeFileId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message")
                        .value("本轮志愿已进入处理阶段，不能再修改"));
    }

    @Test
    void submitRound1NotStarted() throws Exception {
        setAllRoundsFuture();
        assertSubmitMessage("志愿填报尚未开始");
    }

    @Test
    void submitRound2NotStarted() throws Exception {
        setNextRoundNotStarted(2);
        assertSubmitMessage("补录填报尚未开始");
    }

    @Test
    void submitRound3Ended() throws Exception {
        setAllRoundsPast();
        assertSubmitMessage("补录填报已截止");
    }

    @Test
    void submitStageUnconfigured() throws Exception {
        clearStageTimes();
        assertSubmitMessage("当前不在录取轮次");
    }

    private void assertSubmitMessage(String expectedMessage) throws Exception {
        StudentContext student = newStudent("2026999994");
        mockMvc.perform(post("/api/v1/student/volunteers")
                        .session(student.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(
                                List.of(teacherName(2L)), true, true,
                                student.resumeFileId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }
}
