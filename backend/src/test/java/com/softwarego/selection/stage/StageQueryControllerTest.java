package com.softwarego.selection.stage;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 阶段/公告只读接口测试。所有数据库改动在测试事务内回滚，不污染 dev 数据。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StageQueryControllerTest extends StageTestSupport {

    @Test
    void currentStageUnconfigured() throws Exception {
        jdbcTemplate.update("UPDATE stage SET start_at = NULL, end_at = NULL");
        MockHttpSession student = studentSession();

        mockMvc.perform(get("/api/v1/stages/current").session(student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.serverTime").value(notNullValue()))
                .andExpect(jsonPath("$.data.status").value("UNCONFIGURED"))
                .andExpect(jsonPath("$.data.configured").value(false))
                .andExpect(jsonPath("$.data.currentStage").value(nullValue()))
                .andExpect(jsonPath("$.data.nextStage").value(nullValue()))
                .andExpect(jsonPath("$.data.stages", hasSize(7)))
                .andExpect(jsonPath("$.data.stages[*].state", everyItem(is("UNCONFIGURED"))));
    }

    @Test
    void currentStageOngoing() throws Exception {
        setStageTimes(ongoingRows(now()));
        MockHttpSession student = studentSession();

        mockMvc.perform(get("/api/v1/stages/current").session(student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ONGOING"))
                .andExpect(jsonPath("$.data.configured").value(true))
                .andExpect(jsonPath("$.data.currentStage.stageCode").value("OPEN_DAY"))
                .andExpect(jsonPath("$.data.currentStage.stageName").value("实训室开放日"))
                .andExpect(jsonPath("$.data.nextStage").value(nullValue()))
                .andExpect(jsonPath("$.data.stages[0].state").value("FINISHED"))
                .andExpect(jsonPath("$.data.stages[1].state").value("ONGOING"))
                .andExpect(jsonPath("$.data.stages[2].state").value("NOT_STARTED"));
    }

    @Test
    void currentStageBetween() throws Exception {
        setStageTimes(betweenRows(now()));
        MockHttpSession student = studentSession();

        mockMvc.perform(get("/api/v1/stages/current").session(student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BETWEEN"))
                .andExpect(jsonPath("$.data.configured").value(true))
                .andExpect(jsonPath("$.data.currentStage").value(nullValue()))
                .andExpect(jsonPath("$.data.nextStage.stageCode").value("OPEN_DAY"))
                .andExpect(jsonPath("$.data.stages[0].state").value("FINISHED"))
                .andExpect(jsonPath("$.data.stages[1].state").value("NOT_STARTED"));
    }

    @Test
    void currentStageEnded() throws Exception {
        setStageTimes(endedRows(now()));
        MockHttpSession student = studentSession();

        mockMvc.perform(get("/api/v1/stages/current").session(student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ENDED"))
                .andExpect(jsonPath("$.data.configured").value(true))
                .andExpect(jsonPath("$.data.currentStage").value(nullValue()))
                .andExpect(jsonPath("$.data.nextStage").value(nullValue()))
                .andExpect(jsonPath("$.data.stages[*].state", everyItem(is("FINISHED"))));
    }

    @Test
    void managerStagesVisibleOnlyToManager() throws Exception {
        setStageTimes(ongoingRows(now()));

        mockMvc.perform(get("/api/v1/manager/stages").session(managerSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.serverTime").value(notNullValue()))
                .andExpect(jsonPath("$.data.configured").value(true))
                .andExpect(jsonPath("$.data.currentStageCode").value("OPEN_DAY"))
                .andExpect(jsonPath("$.data.stages", hasSize(7)))
                .andExpect(jsonPath("$.data.stages[0].stageCode").value("BRIEFING"))
                .andExpect(jsonPath("$.data.stages[0].stageName").value("集中宣讲"));

        mockMvc.perform(get("/api/v1/manager/stages").session(studentSession()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301))
                .andExpect(jsonPath("$.message").value("仅负责人可配置阶段"));

        mockMvc.perform(get("/api/v1/manager/stages").session(teacherSession()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));

        mockMvc.perform(get("/api/v1/manager/stages"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void managerWriteEndpointsRejectNonManager() throws Exception {
        MockHttpSession student = studentSession();
        MockHttpSession teacher = teacherSession();

        mockMvc.perform(put("/api/v1/manager/stages")
                        .session(student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301))
                .andExpect(jsonPath("$.message").value("仅负责人可配置阶段"));

        mockMvc.perform(post("/api/v1/manager/stages/announcement")
                        .session(student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changeNo\":\"x\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));

        mockMvc.perform(get("/api/v1/manager/stage-changes").session(student))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));

        mockMvc.perform(put("/api/v1/manager/stages")
                        .session(teacher)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));

        mockMvc.perform(put("/api/v1/manager/stages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void announcementsListPaginationAndDetail() throws Exception {
        jdbcTemplate.update("DELETE FROM announcement");
        LocalDateTime base = now();
        String longContent = "第一行公告内容 ".repeat(20) + "\n第二行";
        for (int index = 0; index < 12; index += 1) {
            String title = TEST_TITLE_PREFIX + " 公告 " + (index + 1);
            String content = index == 0 ? longContent : "公告内容 " + (index + 1);
            jdbcTemplate.update(
                    "INSERT INTO announcement (title, content, publisher_id, published_at) VALUES (?,?,?,?)",
                    title, content, 1, base.minusMinutes(index));
        }

        MockHttpSession student = studentSession();

        mockMvc.perform(get("/api/v1/announcements")
                        .session(student)
                        .param("page", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(12))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(5))
                .andExpect(jsonPath("$.data.list", hasSize(5)))
                .andExpect(jsonPath("$.data.list[0].title").value(TEST_TITLE_PREFIX + " 公告 1"))
                .andExpect(jsonPath("$.data.list[0].summary", endsWith("……")))
                .andExpect(jsonPath("$.data.list[0].publishedAt").value(notNullValue()));

        mockMvc.perform(get("/api/v1/announcements")
                        .session(student)
                        .param("page", "2")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(5)))
                .andExpect(jsonPath("$.data.list[0].title").value(TEST_TITLE_PREFIX + " 公告 6"));

        mockMvc.perform(get("/api/v1/announcements")
                        .session(student)
                        .param("page", "3")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list", hasSize(2)));

        long announcementId = announcementIdByTitle(TEST_TITLE_PREFIX + " 公告 1");
        mockMvc.perform(get("/api/v1/announcements/{id}", announcementId).session(student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(announcementId))
                .andExpect(jsonPath("$.data.title").value(TEST_TITLE_PREFIX + " 公告 1"))
                .andExpect(jsonPath("$.data.content").value(longContent))
                .andExpect(jsonPath("$.data.publisherId").value(1))
                .andExpect(jsonPath("$.data.publisherName").value("周明远"))
                .andExpect(jsonPath("$.data.publishedAt").value(notNullValue()));

        mockMvc.perform(get("/api/v1/announcements/{id}", 999999L).session(student))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));

        mockMvc.perform(get("/api/v1/announcements")
                        .session(student)
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("分页参数不合法"));

        mockMvc.perform(get("/api/v1/announcements")
                        .session(student)
                        .param("pageSize", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001));

        mockMvc.perform(get("/api/v1/announcements"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void announcementsEmptyList() throws Exception {
        jdbcTemplate.update("DELETE FROM announcement");
        MockHttpSession student = studentSession();

        mockMvc.perform(get("/api/v1/announcements").session(student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.list", hasSize(0)));
    }
}
