package com.softwarego.selection.common;

import com.softwarego.selectiontest.support.ExceptionTestController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ExceptionTestController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validationErrorReturns40001WithHttp200() throws Exception {
        mockMvc.perform(post("/test/errors/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"staffNo\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("学工号异常"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void businessConflictReturns40901WithHttp200() throws Exception {
        mockMvc.perform(get("/test/errors/conflict"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40901))
                .andExpect(jsonPath("$.message").value("名额已满"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void unauthorizedReturns40101WithHttp401() throws Exception {
        mockMvc.perform(get("/test/errors/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.message").value("未登录或会话过期"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void forbiddenReturns40301WithHttp403() throws Exception {
        mockMvc.perform(get("/test/errors/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301))
                .andExpect(jsonPath("$.message").value("无权访问该数据"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void notFoundReturns40401WithHttp404() throws Exception {
        mockMvc.perform(get("/test/errors/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401))
                .andExpect(jsonPath("$.message").value("资源不存在"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void unmappedPathReturns40401WithHttp404() throws Exception {
        mockMvc.perform(get("/test/errors/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401))
                .andExpect(jsonPath("$.message").value("资源不存在"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void fileErrorReturns50002WithHttp200() throws Exception {
        mockMvc.perform(get("/test/errors/file-error"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(50002))
                .andExpect(jsonPath("$.message").value("简历上传失败，请重试"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void unknownExceptionReturns50001WithHttp500AndNoInternalDetail() throws Exception {
        mockMvc.perform(get("/test/errors/unknown"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(50001))
                .andExpect(jsonPath("$.message").value("系统异常"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(content().string(not(containsString("boom"))));
    }
}
