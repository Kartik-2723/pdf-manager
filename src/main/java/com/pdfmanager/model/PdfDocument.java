package com.pdfmanager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA Entity representing a stored PDF document.
 *
 * When storage strategy = DB:   fileData holds raw bytes, filePath is null.
 * When storage strategy = FS:   filePath holds disk path, fileData is null.
 */
@Entity
@Table(name = "pdf_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Original file name as uploaded by the client */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    /** MIME type — always application/pdf */
    @Column(name = "content_type", nullable = false)
    private String contentType;

    /** Size in bytes */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /**
     * Binary content — populated only when storage strategy = DB.
     * Stored as a BLOB; use @Lob + LONGBLOB for MySQL, OID for PostgreSQL.
     */
    @Lob
    @Column(name = "file_data", columnDefinition = "LONGBLOB")
    private byte[] fileData;

    /**
     * Absolute path on the server filesystem.
     * Populated only when storage strategy = FILESYSTEM.
     */
    @Column(name = "file_path")
    private String filePath;

    /** Human-readable description / title */
    @Column(name = "description")
    private String description;

    /** Upload timestamp */
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    /** Last accessed timestamp */
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }
}
