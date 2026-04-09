package com.pdfmanager.service;

import com.pdfmanager.config.StorageProperties;
import com.pdfmanager.config.StorageStrategy;
import com.pdfmanager.dto.PdfDtos.*;
import com.pdfmanager.exception.PdfExceptions;
import com.pdfmanager.model.PdfDocument;
import com.pdfmanager.repository.PdfDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service Layer — all business logic lives here.
 * Responsibilities:
 *  - Validate that the uploaded file is actually a PDF
 *  - Delegate storage to DB or filesystem depending on config
 *  - Map entities → DTOs (never expose raw entities from controllers)
 *  - Update audit fields (lastAccessedAt) on download
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfService {

    private final PdfDocumentRepository repository;
    private final StorageProperties storageProperties;
    private final Tika tika = new Tika();

    // ------------------------------------------------------------------ //
    //  Upload                                                               //
    // ------------------------------------------------------------------ //

    /**
     * Validate, persist and return metadata about the uploaded PDF.
     *
     * @param file        the multipart upload
     * @param description optional human-readable description
     * @return upload response DTO with download/view URLs
     */
    @Transactional
    public PdfUploadResponse uploadPdf(MultipartFile file, String description) {

        validateFile(file);

        PdfDocument document;

        if (storageProperties.getStrategy() == StorageStrategy.DB) {
            document = storeInDatabase(file, description);
        } else {
            document = storeOnFilesystem(file, description);
        }

        PdfDocument saved = repository.save(document);
        log.info("PDF saved — id={}, name={}, strategy={}",
                saved.getId(), saved.getFileName(), storageProperties.getStrategy());

        return toUploadResponse(saved);
    }

    // ------------------------------------------------------------------ //
    //  Download / View                                                      //
    // ------------------------------------------------------------------ //

    /**
     * Fetch the binary content of a PDF as a Spring {@link Resource}.
     * Also updates the lastAccessedAt timestamp.
     */
    @Transactional
    public Resource fetchPdf(Long id) {
        PdfDocument document = repository.findById(id)
                .orElseThrow(() -> new PdfExceptions.PdfNotFoundException(id));

        // Update access timestamp
        repository.updateLastAccessedAt(id, LocalDateTime.now());

        if (storageProperties.getStrategy() == StorageStrategy.DB) {
            if (document.getFileData() == null) {
                throw new PdfExceptions.StorageException(
                        "File data missing in DB for id=" + id);
            }
            return new ByteArrayResource(document.getFileData());
        } else {
            Path filePath = Paths.get(document.getFilePath());
            if (!Files.exists(filePath)) {
                throw new PdfExceptions.StorageException(
                        "File not found on disk: " + filePath);
            }
            return new FileSystemResource(filePath);
        }
    }

    // ------------------------------------------------------------------ //
    //  Query                                                                //
    // ------------------------------------------------------------------ //

    /** List all PDFs — metadata only, no binary data. */
    @Transactional(readOnly = true)
    public List<PdfMetadataResponse> listAllPdfs() {
        return repository.findAllMetadata()
                .stream()
                .map(this::toMetadataResponse)
                .collect(Collectors.toList());
    }

    /** Get a single PDF's metadata by id. */
    @Transactional(readOnly = true)
    public PdfMetadataResponse getPdfMetadata(Long id) {
        PdfDocument doc = repository.findMetadataById(id)
                .orElseThrow(() -> new PdfExceptions.PdfNotFoundException(id));
        return toMetadataResponse(doc);
    }

    /** Search by filename (partial, case-insensitive). */
    @Transactional(readOnly = true)
    public List<PdfMetadataResponse> searchByFileName(String name) {
        return repository.findByFileNameContainingIgnoreCase(name)
                .stream()
                .map(this::toMetadataResponse)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ //
    //  Delete                                                               //
    // ------------------------------------------------------------------ //

    @Transactional
    public void deletePdf(Long id) {
        PdfDocument doc = repository.findById(id)
                .orElseThrow(() -> new PdfExceptions.PdfNotFoundException(id));

        // If filesystem, remove the file from disk
        if (storageProperties.getStrategy() == StorageStrategy.FILESYSTEM
                && doc.getFilePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(doc.getFilePath()));
            } catch (IOException e) {
                log.warn("Could not delete file from disk: {}", doc.getFilePath(), e);
            }
        }

        repository.delete(doc);
        log.info("PDF deleted — id={}", id);
    }

    // ------------------------------------------------------------------ //
    //  Internal helpers                                                     //
    // ------------------------------------------------------------------ //

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new PdfExceptions.InvalidPdfException("No file provided or file is empty.");
        }

        // Detect real MIME type from bytes (not just the declared content-type header)
        try {
            String detectedMime = tika.detect(file.getInputStream());
            if (!"application/pdf".equalsIgnoreCase(detectedMime)) {
                throw new PdfExceptions.InvalidPdfException(
                        "Only PDF files are accepted. Detected type: " + detectedMime);
            }
        } catch (IOException e) {
            throw new PdfExceptions.InvalidPdfException("Cannot read uploaded file: " + e.getMessage());
        }
    }

    private PdfDocument storeInDatabase(MultipartFile file, String description) {
        try {
            return PdfDocument.builder()
                    .fileName(sanitizeFilename(file.getOriginalFilename()))
                    .contentType("application/pdf")
                    .fileSize(file.getSize())
                    .fileData(file.getBytes())
                    .description(description)
                    .build();
        } catch (IOException e) {
            throw new PdfExceptions.StorageException("Failed to read file bytes: " + e.getMessage(), e);
        }
    }

    private PdfDocument storeOnFilesystem(MultipartFile file, String description) {
        try {
            Path uploadDir = Paths.get(storageProperties.getFilesystem().getPath());
            Files.createDirectories(uploadDir);

            // Use UUID to avoid filename collisions
            String storedName = UUID.randomUUID() + "_" + sanitizeFilename(file.getOriginalFilename());
            Path destination = uploadDir.resolve(storedName);

            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            log.info("PDF stored on filesystem: {}", destination.toAbsolutePath());

            return PdfDocument.builder()
                    .fileName(sanitizeFilename(file.getOriginalFilename()))
                    .contentType("application/pdf")
                    .fileSize(file.getSize())
                    .filePath(destination.toAbsolutePath().toString())
                    .description(description)
                    .build();
        } catch (IOException e) {
            throw new PdfExceptions.StorageException("Failed to store file on disk: " + e.getMessage(), e);
        }
    }

    private String sanitizeFilename(String original) {
        if (original == null || original.isBlank()) return "unnamed.pdf";
        // Strip any path components (security: prevent directory traversal)
        return Paths.get(original).getFileName().toString()
                .replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }

    // ------------------------------------------------------------------ //
    //  DTO mappers                                                          //
    // ------------------------------------------------------------------ //

    private PdfUploadResponse toUploadResponse(PdfDocument doc) {
        return PdfUploadResponse.builder()
                .id(doc.getId())
                .fileName(doc.getFileName())
                .contentType(doc.getContentType())
                .fileSize(doc.getFileSize())
                .description(doc.getDescription())
                .uploadedAt(doc.getUploadedAt())
                .downloadUrl("/api/v1/pdfs/" + doc.getId() + "/download")
                .viewUrl("/api/v1/pdfs/" + doc.getId() + "/view")
                .build();
    }

    private PdfMetadataResponse toMetadataResponse(PdfDocument doc) {
        return PdfMetadataResponse.builder()
                .id(doc.getId())
                .fileName(doc.getFileName())
                .contentType(doc.getContentType())
                .fileSize(doc.getFileSize())
                .description(doc.getDescription())
                .uploadedAt(doc.getUploadedAt())
                .lastAccessedAt(doc.getLastAccessedAt())
                .downloadUrl("/api/v1/pdfs/" + doc.getId() + "/download")
                .viewUrl("/api/v1/pdfs/" + doc.getId() + "/view")
                .build();
    }
}
