package com.pdfmanager.controller;

import com.pdfmanager.dto.PdfDtos.*;
import com.pdfmanager.service.PdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Controller Layer — HTTP concerns ONLY.
 *
 * Responsibilities:
 *  - Map HTTP verbs + paths to service calls
 *  - Build HTTP response (headers, status codes, content-type)
 *  - NO business logic here
 *
 * Base path: /api/v1/pdfs
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/pdfs")
@RequiredArgsConstructor
public class PdfController {

    private final PdfService pdfService;

    // ------------------------------------------------------------------ //
    //  POST /api/v1/pdfs/up    load                                            //
    // ------------------------------------------------------------------ //

    /**
     * Upload a PDF file.
     *
     * curl -X POST http://localhost:8080/api/v1/pdfs/upload \
     *   -F "file=@sample.pdf" \
     *   -F "description=My first PDF"
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PdfUploadResponse>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "description", required = false) String description) {

        log.info("Upload request — file='{}', size={}", file.getOriginalFilename(), file.getSize());
        PdfUploadResponse response = pdfService.uploadPdf(file, description);
        return ResponseEntity
                .status(201)
                .body(ApiResponse.ok("PDF uploaded successfully.", response));
    }

    // ------------------------------------------------------------------ //
    //  GET /api/v1/pdfs                                                    //
    // ------------------------------------------------------------------ //

    /**
     * List all stored PDFs (metadata only — no binary data).
     *
     * curl http://localhost:8080/api/v1/pdfs
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PdfMetadataResponse>>> list() {
        List<PdfMetadataResponse> pdfs = pdfService.listAllPdfs();
        return ResponseEntity.ok(ApiResponse.ok("Found " + pdfs.size() + " PDF(s).", pdfs));
    }

    // ------------------------------------------------------------------ //
    //  GET /api/v1/pdfs/{id}                                              //
    // ------------------------------------------------------------------ //

    /**
     * Get metadata for a single PDF.
     *
     * curl http://localhost:8080/api/v1/pdfs/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PdfMetadataResponse>> getMetadata(@PathVariable Long id) {
        PdfMetadataResponse meta = pdfService.getPdfMetadata(id);
        return ResponseEntity.ok(ApiResponse.ok("PDF found.", meta));
    }

    // ------------------------------------------------------------------ //
    //  GET /api/v1/pdfs/{id}/download                                      //
    // ------------------------------------------------------------------ //

    /**
     * Download a PDF — browser will prompt Save-As dialog.
     *
     * curl -O http://localhost:8080/api/v1/pdfs/1/download
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        log.info("Download request — id={}", id);

        // Fetch metadata to get the filename
        PdfMetadataResponse meta = pdfService.getPdfMetadata(id);
        Resource resource = pdfService.fetchPdf(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(meta.getFileName())
                                .build()
                                .toString())
                .body(resource);
    }

    // ------------------------------------------------------------------ //
    //  GET /api/v1/pdfs/{id}/view                                         //
    // ------------------------------------------------------------------ //

    /**
     * View a PDF inline in the browser.
     *
     * curl http://localhost:8080/api/v1/pdfs/1/view
     */
    @GetMapping("/{id}/view")
    public ResponseEntity<Resource> view(@PathVariable Long id) {
        log.info("View request — id={}", id);

        PdfMetadataResponse meta = pdfService.getPdfMetadata(id);
        Resource resource = pdfService.fetchPdf(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(meta.getFileName())
                                .build()
                                .toString())
                .body(resource);
    }

    // ------------------------------------------------------------------ //
    //  GET /api/v1/pdfs/search?name=                                       //
    // ------------------------------------------------------------------ //

    /**
     * Search PDFs by filename (partial match).
     *
     * curl "http://localhost:8080/api/v1/pdfs/search?name=invoice"
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PdfMetadataResponse>>> search(
            @RequestParam("name") String name) {
        List<PdfMetadataResponse> results = pdfService.searchByFileName(name);
        return ResponseEntity.ok(ApiResponse.ok("Found " + results.size() + " result(s).", results));
    }

    // ------------------------------------------------------------------ //
    //  DELETE /api/v1/pdfs/{id}                                           //
    // ------------------------------------------------------------------ //

    /**
     * Delete a PDF.
     *
     * curl -X DELETE http://localhost:8080/api/v1/pdfs/1
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        log.info("Delete request — id={}", id);
        pdfService.deletePdf(id);
        return ResponseEntity.ok(ApiResponse.ok("PDF deleted successfully.", null));
    }
}
