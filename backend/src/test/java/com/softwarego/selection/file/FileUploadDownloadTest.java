package com.softwarego.selection.file;

import com.fasterxml.jackson.databind.JsonNode;
import com.softwarego.selection.config.UploadProperties;
import com.softwarego.selectiontest.support.B3TestSupport;
import com.softwarego.selectiontest.support.FailingResumeFileStorage;
import com.softwarego.selectiontest.support.FailingResumeFileStorageConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PDF 上传校验、磁盘/数据库一致性、鉴权下载、越权 403、路径穿越。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(FailingResumeFileStorageConfig.class)
@Transactional
class FileUploadDownloadTest extends B3TestSupport {

    private static final String PDF = "application/pdf";

    @Autowired
    private FailingResumeFileStorage failingResumeFileStorage;

    @Autowired
    private UploadProperties uploadProperties;

    @BeforeEach
    void setUp() {
        failingResumeFileStorage.reset();
    }

    @AfterEach
    void tearDown() {
        failingResumeFileStorage.reset();
    }

    @Test
    void studentResumeUploadAndDownloadPermissions() throws Exception {
        MockHttpSession student = studentSession();
        byte[] content = pdfBytes();
        String originalName = "学生 简历.pdf";

        MvcResult uploadResult = upload(student, new MockMultipartFile("file", originalName, PDF, content));
        JsonNode data = json(uploadResult).path("data");
        long fileId = data.path("fileId").asLong();
        assertThat(fileId).isPositive();
        assertThat(data.path("originalName").asText()).isEqualTo(originalName);
        assertThat(data.path("fileSize").asLong()).isEqualTo(content.length);
        assertThat(data.path("contentType").asText()).isEqualTo(PDF);
        assertThat(data.path("uploadedAt").asText()).isNotBlank();

        Map<String, Object> row = resumeRow(fileId);
        assertThat(((Number) row.get("owner_id")).longValue()).isEqualTo(6L);
        assertThat(row.get("purpose")).isEqualTo("STUDENT_RESUME");
        assertThat(row.get("original_name")).isEqualTo(originalName);
        String storagePath = (String) row.get("storage_path");
        assertThat(storagePath).matches("\\d{4}/\\d{2}/[0-9a-f-]{36}\\.pdf");
        assertThat(storagePath).doesNotContain("..");
        assertThat(Files.exists(storedPath(storagePath))).isTrue();

        String disposition = "attachment; filename*=UTF-8''" + encodedName(originalName);
        mockMvc.perform(get("/api/v1/files/{id}", fileId).session(student))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, disposition))
                .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, String.valueOf(content.length)))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(content));

        mockMvc.perform(get("/api/v1/files/{id}", fileId).session(teacherSession()))
                .andExpect(status().isOk())
                .andExpect(content().bytes(content));

        mockMvc.perform(get("/api/v1/files/{id}", fileId).session(otherTeacherSession()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));

        mockMvc.perform(get("/api/v1/files/{id}", fileId).session(managerSession()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));

        mockMvc.perform(get("/api/v1/files/{id}", fileId).session(student2Session()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));

        mockMvc.perform(get("/api/v1/files/{id}", fileId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void teacherResumeReadableByAllLoggedIn() throws Exception {
        byte[] content = pdfBytes();
        MvcResult uploadResult = upload(
                teacherSession(),
                new MockMultipartFile("file", "导师简历.pdf", PDF, content));
        long fileId = json(uploadResult).path("data").path("fileId").asLong();

        Map<String, Object> row = resumeRow(fileId);
        assertThat(((Number) row.get("owner_id")).longValue()).isEqualTo(2L);
        assertThat(row.get("purpose")).isEqualTo("TEACHER_RESUME");

        for (MockHttpSession session : List.of(
                studentSession(), managerSession(), otherTeacherSession())) {
            mockMvc.perform(get("/api/v1/files/{id}", fileId).session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().bytes(content));
        }
    }

    @Test
    void uploadRejectsNonPdfOversizeAndManager() throws Exception {
        MockHttpSession student = studentSession();
        byte[] content = pdfBytes();

        assertInvalidUpload(student, new MockMultipartFile(
                "file", "resume.txt", MediaType.TEXT_PLAIN_VALUE, content));
        assertInvalidUpload(student, new MockMultipartFile(
                "file", "resume.pdf", MediaType.TEXT_PLAIN_VALUE, content));
        assertInvalidUpload(student, new MockMultipartFile(
                "file", "resume.pdf", PDF, "not a pdf".getBytes(StandardCharsets.US_ASCII)));

        byte[] oversized = new byte[10 * 1024 * 1024 + 1];
        System.arraycopy("%PDF-".getBytes(StandardCharsets.US_ASCII), 0, oversized, 0, 5);
        mockMvc.perform(multipart("/api/v1/files/resumes")
                        .file(new MockMultipartFile("file", "big.pdf", PDF, oversized))
                        .session(student))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(50002))
                .andExpect(jsonPath("$.message").value("简历上传失败，请重试"));

        mockMvc.perform(multipart("/api/v1/files/resumes")
                        .file(new MockMultipartFile("file", "manager.pdf", PDF, content))
                        .session(managerSession()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
    }

    @Test
    void uploadFailureLeavesNoDatabaseRecord() throws Exception {
        failingResumeFileStorage.failNextStore();

        mockMvc.perform(multipart("/api/v1/files/resumes")
                        .file(new MockMultipartFile("file", "fail.pdf", PDF, pdfBytes()))
                        .session(studentSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(50002))
                .andExpect(jsonPath("$.message").value("简历上传失败，请重试"));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM resume_file WHERE owner_id = 6 AND original_name = 'fail.pdf'",
                Integer.class);
        assertThat(count).isZero();
    }

    @Test
    void uploadSanitizesPathTraversalName() throws Exception {
        MockHttpSession student = studentSession();
        byte[] content = pdfBytes();

        MvcResult result = upload(
                student,
                new MockMultipartFile("file", "../../evil.pdf", PDF, content));
        JsonNode data = json(result).path("data");
        long fileId = data.path("fileId").asLong();

        assertThat(data.path("originalName").asText()).isEqualTo("evil.pdf");
        String storagePath = (String) resumeRow(fileId).get("storage_path");
        assertThat(storagePath).doesNotContain("..");
        assertThat(Files.exists(storedPath(storagePath))).isTrue();

        mockMvc.perform(get("/api/v1/files/{id}", fileId).session(student))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''evil.pdf"));
    }

    @Test
    void downloadRejectsTraversalStoragePath() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO resume_file "
                        + "(owner_id, purpose, original_name, storage_path, content_type, file_size) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                6, "STUDENT_RESUME", "evil.pdf", "../../etc/passwd", PDF, 10);
        Long fileId = jdbcTemplate.queryForObject(
                "SELECT id FROM resume_file WHERE storage_path = ?",
                Long.class, "../../etc/passwd");

        mockMvc.perform(get("/api/v1/files/{id}", fileId).session(studentSession()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    @Test
    void downloadNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/files/999999").session(studentSession()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    private MvcResult upload(MockHttpSession session, MockMultipartFile file) throws Exception {
        return mockMvc.perform(multipart("/api/v1/files/resumes")
                        .file(file)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
    }

    private void assertInvalidUpload(MockHttpSession session, MockMultipartFile file) throws Exception {
        mockMvc.perform(multipart("/api/v1/files/resumes")
                        .file(file)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("请上传 PDF 格式的简历"));
    }

    private Map<String, Object> resumeRow(long fileId) {
        return jdbcTemplate.queryForMap(
                "SELECT owner_id, purpose, original_name, storage_path, content_type, file_size "
                        + "FROM resume_file WHERE id = ?",
                fileId);
    }

    private Path storedPath(String storagePath) {
        return Path.of(uploadProperties.getDir()).resolve(storagePath).normalize();
    }

    private String encodedName(String name) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private byte[] pdfBytes() {
        return "%PDF-1.4\n1 0 obj\n<<>>\nendobj\n%%EOF\n"
                .getBytes(StandardCharsets.US_ASCII);
    }
}
