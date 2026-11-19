package com.softwarego.selection.todo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 三角色 × 阶段 × 用户状态 × 截止时间 × actionPath 的待办矩阵最小测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TodoControllerTest extends B7TestSupport {

    @Test
    void studentRound1SubmitTodo() throws Exception {
        setRound1Open();
        var session = login("STUDENT", "2026999990");

        mockMvc.perform(get("/api/v1/todos").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.serverTime").value(notNullValue()))
                .andExpect(jsonPath("$.data.list", hasSize(1)))
                .andExpect(jsonPath("$.data.list[0].type").value("VOLUNTEER_SUBMIT"))
                .andExpect(jsonPath("$.data.list[0].stageCode").value("ROUND_1"))
                .andExpect(jsonPath("$.data.list[0].stageName").value("第一轮录取"))
                .andExpect(jsonPath("$.data.list[0].deadline").value(notNullValue()))
                .andExpect(jsonPath("$.data.list[0].status").value("TODO"))
                .andExpect(jsonPath("$.data.list[0].actionPath").value("/student/volunteer"))
                .andExpect(jsonPath("$.data.list[0].actionLabel").value("去填报"));
    }

    @Test
    void studentRound2MakeupTodo() throws Exception {
        setRound2Open();
        var session = login("STUDENT", "2026999990");

        mockMvc.perform(get("/api/v1/todos").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(1)))
                .andExpect(jsonPath("$.data.list[0].type").value("VOLUNTEER_MAKEUP"))
                .andExpect(jsonPath("$.data.list[0].stageCode").value("ROUND_2"))
                .andExpect(jsonPath("$.data.list[0].deadline").value(notNullValue()))
                .andExpect(jsonPath("$.data.list[0].actionPath").value("/student/volunteer"));
    }

    @Test
    void makeupRequiresAvailableQuota() throws Exception {
        setRound2Open();
        setAllPlannedQuota(1);
        var session = login("STUDENT", "2026999990");

        mockMvc.perform(get("/api/v1/todos").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(0)));
    }

    @Test
    void studentOfferConfirmTodo() throws Exception {
        setRound2Open();
        setOfferDeadlineInMinutes(8L, 1440L);
        var session = login("STUDENT", "2026001004"); // 学生 9，已有第二轮申请

        mockMvc.perform(get("/api/v1/todos").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(1)))
                .andExpect(jsonPath("$.data.list[0].type").value("OFFER_CONFIRM"))
                .andExpect(jsonPath("$.data.list[0].stageCode").value("ROUND_2"))
                .andExpect(jsonPath("$.data.list[0].deadline").value(notNullValue()))
                .andExpect(jsonPath("$.data.list[0].actionPath").value("/student/offers"))
                .andExpect(jsonPath("$.data.list[0].actionLabel").value("去确认"));
    }

    @Test
    void studentTodosSortedByDeadlineThenType() throws Exception {
        setRound2Open();
        setOfferDeadlineInMinutes(6L, 30L);
        var session = login("STUDENT", "2026001006"); // 学生 11，有第一轮 PENDING offer

        mockMvc.perform(get("/api/v1/todos").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(2)))
                .andExpect(jsonPath("$.data.list[0].type").value("OFFER_CONFIRM"))
                .andExpect(jsonPath("$.data.list[1].type").value("VOLUNTEER_MAKEUP"));
    }

    @Test
    void admittedStudentHasNoVolunteerOrConfirmTodo() throws Exception {
        setRound2Open();
        mockMvc.perform(get("/api/v1/todos").session(studentSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(0)));
    }

    @Test
    void studentWithCurrentApplicationHasNoTodo() throws Exception {
        setRound2Open();
        mockMvc.perform(get("/api/v1/todos").session(login("STUDENT", "2026001009")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(0)));
    }

    @Test
    void offerConfirmTodoSurvivesClosedRound() throws Exception {
        setRound2Ended();
        setOfferDeadlineInMinutes(8L, 1440L);

        mockMvc.perform(get("/api/v1/todos").session(login("STUDENT", "2026001004")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(1)))
                .andExpect(jsonPath("$.data.list[0].type").value("OFFER_CONFIRM"));
    }

    @Test
    void teacherScoreEntryTodo() throws Exception {
        setRound2Open();
        createStudentTarget("2026999991", 2, 2L, null);

        mockMvc.perform(get("/api/v1/todos").session(teacherSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(1)))
                .andExpect(jsonPath("$.data.list[0].type").value("SCORE_ENTRY"))
                .andExpect(jsonPath("$.data.list[0].stageCode").value("ROUND_2"))
                .andExpect(jsonPath("$.data.list[0].deadline").value(notNullValue()))
                .andExpect(jsonPath("$.data.list[0].actionPath").value("/teacher/admissions"));
    }

    @Test
    void closedRoundHidesTeacherRoundTodos() throws Exception {
        setRound2Ended();
        createStudentTarget("2026999991", 2, 2L, null);

        mockMvc.perform(get("/api/v1/todos").session(teacherSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(0)));
    }

    @Test
    void teacherGrantOfferTodo() throws Exception {
        setRound2Open();
        createStudentTarget("2026999992", 2, 3L, new BigDecimal("95.00"));
        setPlannedQuota(3L, 10);

        mockMvc.perform(get("/api/v1/todos").session(login("TEACHER", "1000000002")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(1)))
                .andExpect(jsonPath("$.data.list[0].type").value("GRANT_OFFER"))
                .andExpect(jsonPath("$.data.list[0].stageCode").value("ROUND_2"))
                .andExpect(jsonPath("$.data.list[0].deadline").value(notNullValue()))
                .andExpect(jsonPath("$.data.list[0].actionPath").value("/teacher/admissions"));
    }

    @Test
    void grantTodoRequiresRemainingQuota() throws Exception {
        setRound2Open();
        createStudentTarget("2026999992", 2, 3L, new BigDecimal("95.00"));
        setPlannedQuota(3L, 1);

        mockMvc.perform(get("/api/v1/todos").session(login("TEACHER", "1000000002")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(0)));
    }

    @Test
    void teacherWithoutLabHasLabInfoTodoWithoutDeadline() throws Exception {
        var session = login("TEACHER", "2026999993");

        mockMvc.perform(get("/api/v1/todos").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(1)))
                .andExpect(jsonPath("$.data.list[0].type").value("LAB_INFO"))
                .andExpect(jsonPath("$.data.list[0].stageCode").value(nullValue()))
                .andExpect(jsonPath("$.data.list[0].deadline").value(nullValue()))
                .andExpect(jsonPath("$.data.list[0].actionPath").value("/teacher/lab"))
                .andExpect(jsonPath("$.data.list[0].actionLabel").value("去填写"));
    }

    @Test
    void managerConfiguredHasNoTodo() throws Exception {
        mockMvc.perform(get("/api/v1/todos").session(managerSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(0)));
    }

    @Test
    void managerUnconfiguredStageHasConfigTodo() throws Exception {
        setStageUnconfigured("ENROLLMENT");

        mockMvc.perform(get("/api/v1/todos").session(managerSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(1)))
                .andExpect(jsonPath("$.data.list[0].type").value("STAGE_CONFIG"))
                .andExpect(jsonPath("$.data.list[0].deadline").value(nullValue()))
                .andExpect(jsonPath("$.data.list[0].actionPath").value("/manager/stages"))
                .andExpect(jsonPath("$.data.list[0].actionLabel").value("去配置"));
    }

    @Test
    void todosRequireLogin() throws Exception {
        mockMvc.perform(get("/api/v1/todos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }
}
