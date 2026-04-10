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

@Slf4j
@RestController
@RequestMapping("/api/v1/pdfs")
@RequiredArgsConstructor
public class PdfController {

    private final PdfService pdfService;

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


    @GetMapping
    public ResponseEntity<ApiResponse<List<PdfMetadataResponse>>> list() {
        List<PdfMetadataResponse> pdfs = pdfService.listAllPdfs();
        return ResponseEntity.ok(ApiResponse.ok("Found " + pdfs.size() + " PDF(s).", pdfs));
    }

 
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PdfMetadataResponse>> getMetadata(@PathVariable Long id) {
        PdfMetadataResponse meta = pdfService.getPdfMetadata(id);
        return ResponseEntity.ok(ApiResponse.ok("PDF found.", meta));
    }

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

    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<PdfMetadataResponse>>> search(
            @RequestParam("name") String name) {
        List<PdfMetadataResponse> results = pdfService.searchByFileName(name);
        return ResponseEntity.ok(ApiResponse.ok("Found " + results.size() + " result(s).", results));
    }

 
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        log.info("Delete request — id={}", id);
        pdfService.deletePdf(id);
        return ResponseEntity.ok(ApiResponse.ok("PDF deleted successfully.", null));
    }
}
