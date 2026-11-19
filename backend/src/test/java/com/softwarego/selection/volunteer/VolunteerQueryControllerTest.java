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
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 志愿 current/history/available-teachers 只读、轮次状态、补录名额与权限测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class VolunteerQueryControllerTest extends VolunteerTestSupport {

    @Test
    void currentRoundOpenWithoutApplication() throws Exception {
        setRoundOpen(1);
        StudentContext student = newStudent("2026999994");

        mockMvc.perform(get("/api/v1/student/volunteers/current").session(student.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.serverTime").value(notNullValue()))
                .andExpect(jsonPath("$.data.roundNo").value(1))
                .andExpect(jsonPath("$.data.roundState").value("OPEN"))
                .andExpect(jsonPath("$.data.roundStart").value(notNullValue()))
                .andExpect(jsonPath("$.data.roundEnd").value(notNullValue()))
                .andExpect(jsonPath("$.data.admitted").value(false))
                .andExpect(jsonPath("$.data.canSubmit").value(true))
                .andExpect(jsonPath("$.data.blockedReason").value(nullValue()))
                .andExpect(jsonPath("$.data.availableTeacherCount").isNumber())
                .andExpect(jsonPath("$.data.application").value(nullValue()));
    }

    @Test
    void currentRoundNotStarted() throws Exception {
        setAllRoundsFuture();
        StudentContext student = newStudent("2026999994");

        mockMvc.perform(get("/api/v1/student/volunteers/current").session(student.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roundNo").value(1))
                .andExpect(jsonPath("$.data.roundState").value("NOT_STARTED"))
                .andExpect(jsonPath("$.data.canSubmit").value(false))
                .andExpect(jsonPath("$.data.blockedReason").value("NOT_OPEN"));
    }

    @Test
    void nextRoundNotStartedInGap() throws Exception {
        setNextRoundNotStarted(2);
        StudentContext student = newStudent("2026999994");

        mockMvc.perform(get("/api/v1/student/volunteers/current").session(student.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roundNo").value(2))
                .andExpect(jsonPath("$.data.roundState").value("NOT_STARTED"))
                .andExpect(jsonPath("$.data.canSubmit").value(false))
                .andExpect(jsonPath("$.data.blockedReason").value("NOT_OPEN"));
    }

    @Test
    void currentRoundEnded() throws Exception {
        setAllRoundsPast();
        StudentContext student = newStudent("2026999994");

        mockMvc.perform(get("/api/v1/student/volunteers/current").session(student.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roundNo").value(3))
                .andExpect(jsonPath("$.data.roundState").value("ENDED"))
                .andExpect(jsonPath("$.data.canSubmit").value(false))
                .andExpect(jsonPath("$.data.blockedReason").value("ENDED"));
    }

    @Test
    void currentRoundUnconfigured() throws Exception {
        clearStageTimes();
        StudentContext student = newStudent("2026999994");

        mockMvc.perform(get("/api/v1/student/volunteers/current").session(student.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roundNo").value(nullValue()))
                .andExpect(jsonPath("$.data.roundState").value("UNCONFIGURED"))
                .andExpect(jsonPath("$.data.canSubmit").value(false))
                .andExpect(jsonPath("$.data.blockedReason").value(nullValue()))
                .andExpect(jsonPath("$.data.availableTeacherCount").value(0))
                .andExpect(jsonPath("$.data.application").value(nullValue()));
    }

    @Test
    void admittedStudentIsBlocked() throws Exception {
        setRoundOpen(1);

        mockMvc.perform(get("/api/v1/student/volunteers/current").session(studentSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roundNo").value(1))
                .andExpect(jsonPath("$.data.admitted").value(true))
                .andExpect(jsonPath("$.data.canSubmit").value(false))
                .andExpect(jsonPath("$.data.blockedReason").value("ADMITTED"))
                .andExpect(jsonPath("$.data.application").value(notNullValue()))
                .andExpect(jsonPath("$.data.application.roundNo").value(1));
    }

    @Test
    void processedStudentIsBlocked() throws Exception {
        setRoundOpen(1);
        MockHttpSession processedStudent = login("STUDENT", "2026001003");

        mockMvc.perform(get("/api/v1/student/volunteers/current").session(processedStudent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roundNo").value(1))
                .andExpect(jsonPath("$.data.admitted").value(false))
                .andExpect(jsonPath("$.data.canSubmit").value(false))
                .andExpect(jsonPath("$.data.blockedReason").value("PROCESSED"))
                .andExpect(jsonPath("$.data.application").value(notNullValue()))
                .andExpect(jsonPath("$.data.application.targets[0].score").doesNotExist());
    }

    @Test
    void availableTeachersRound2() throws Exception {
        setRoundOpen(2);
        setAllPlannedQuota(1);
        setPlannedQuota(2L, 3);

        mockMvc.perform(get("/api/v1/student/available-teachers").session(studentSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.serverTime").value(notNullValue()))
                .andExpect(jsonPath("$.data.roundNo").value(2))
                .andExpect(jsonPath("$.data.list", hasSize(1)))
                .andExpect(jsonPath("$.data.list[0].teacherId").value(2))
                .andExpect(jsonPath("$.data.list[0].teacherName").value("陈启航"))
                .andExpect(jsonPath("$.data.list[0].plannedQuota").value(3))
                .andExpect(jsonPath("$.data.list[0].remainingQuota").value(1))
                .andExpect(jsonPath("$.data.list[0].labName").value(notNullValue()));
    }

    @Test
    void availableTeachersNotOpen() throws Exception {
        setAllRoundsFuture();

        mockMvc.perform(get("/api/v1/student/available-teachers").session(studentSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roundNo").value(nullValue()))
                .andExpect(jsonPath("$.data.list", hasSize(0)));
    }

    @Test
    void historyRetainsRoundsAndHidesScores() throws Exception {
        setRoundOpen(1);
        StudentContext student = newStudent("2026999994");
        String firstTeacher = teacherName(3L);

        mockMvc.perform(post("/api/v1/student/volunteers")
                        .session(student.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(
                                List.of(firstTeacher), true, false, student.resumeFileId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        setRoundOpen(2);
        setPlannedQuota(2L, 3);
        String secondTeacher = teacherName(2L);

        mockMvc.perform(post("/api/v1/student/volunteers")
                        .session(student.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody(
                                List.of(secondTeacher), true, true, student.resumeFileId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.roundNo").value(2));

        assertThat(applicationCount(student.studentId(), 1)).isEqualTo(1);
        assertThat(applicationCount(student.studentId(), 2)).isEqualTo(1);

        MvcResult historyResult = mockMvc.perform(get("/api/v1/student/volunteers")
                        .session(student.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(2)))
                .andExpect(jsonPath("$.data.list[0].roundNo").value(2))
                .andExpect(jsonPath("$.data.list[1].roundNo").value(1))
                .andExpect(jsonPath("$.data.list[0].targets[0].teacherName").value(secondTeacher))
                .andExpect(jsonPath("$.data.list[1].targets[0].teacherName").value(firstTeacher))
                .andExpect(jsonPath("$.data.list[0].targets[0].score").doesNotExist())
                .andExpect(jsonPath("$.data.list[0].targets[0].offerStatus").doesNotExist())
                .andReturn();

        assertThat(json(historyResult).path("data").path("list").size()).isEqualTo(2);

        mockMvc.perform(get("/api/v1/student/volunteers")
                        .session(student.session())
                        .param("roundNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(1)))
                .andExpect(jsonPath("$.data.list[0].roundNo").value(1));

        mockMvc.perform(get("/api/v1/student/volunteers")
                        .session(student.session())
                        .param("roundNo", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void nonStudentRejected() throws Exception {
        MockHttpSession teacher = teacherSession();

        mockMvc.perform(get("/api/v1/student/volunteers/current").session(teacher))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301))
                .andExpect(jsonPath("$.message").value("无权访问该数据"));

        mockMvc.perform(get("/api/v1/student/volunteers").session(teacher))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));

        mockMvc.perform(get("/api/v1/student/available-teachers").session(teacher))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));

        mockMvc.perform(post("/api/v1/student/volunteers")
                        .session(teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));

        mockMvc.perform(get("/api/v1/student/volunteers/current"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }
}
