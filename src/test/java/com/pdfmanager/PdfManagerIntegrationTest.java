package com.pdfmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test — starts the full Spring context + H2 in-memory DB.
 *
 * Tests the complete request lifecycle:
 *   Controller → Service → Repository → H2 → back up
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PdfManagerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Minimal valid PDF header bytes (PDF 1.4 magic).
     * A real integration test should load a proper PDF from test resources.
     */
    private static final byte[] MINIMAL_PDF = (
            "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\n" +
            "xref\n0 2\n0000000000 65535 f \ntrailer\n<< /Size 2 >>\nstartxref\n9\n%%EOF"
    ).getBytes();

    @Test
    @DisplayName("Upload PDF → returns 201 with metadata")
    void testUpload_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                MINIMAL_PDF
        );

        mockMvc.perform(multipart("/api/v1/pdfs/upload")
                        .file(file)
                        .param("description", "Test PDF"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileName").value("test.pdf"))
                .andExpect(jsonPath("$.data.id").isNumber());
    }

    @Test
    @DisplayName("Upload non-PDF file → returns 400")
    void testUpload_invalidFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "not-a-pdf.txt",
                "text/plain",
                "hello world".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/pdfs/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("List PDFs → returns array")
    void testList() throws Exception {
        mockMvc.perform(get("/api/v1/pdfs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("Get non-existent PDF → 404")
    void testGet_notFound() throws Exception {
        mockMvc.perform(get("/api/v1/pdfs/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Upload then download → returns PDF bytes")
    void testUploadThenDownload() throws Exception {
        // Upload
        MockMultipartFile file = new MockMultipartFile(
                "file", "download-test.pdf", "application/pdf", MINIMAL_PDF);

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/pdfs/upload").file(file))
                .andExpect(status().isCreated())
                .andReturn();

        // Parse the returned id
        String body = uploadResult.getResponse().getContentAsString();
        Long id = objectMapper.readTree(body).path("data").path("id").asLong();

        // Download
        mockMvc.perform(get("/api/v1/pdfs/{id}/download", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")));
    }

    @Test
    @DisplayName("Upload then delete → 200, then GET returns 404")
    void testUploadThenDelete() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "delete-test.pdf", "application/pdf", MINIMAL_PDF);

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/pdfs/upload").file(file))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = objectMapper.readTree(
                uploadResult.getResponse().getContentAsString()
        ).path("data").path("id").asLong();

        mockMvc.perform(delete("/api/v1/pdfs/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/pdfs/{id}", id))
                .andExpect(status().isNotFound());
    }
}
