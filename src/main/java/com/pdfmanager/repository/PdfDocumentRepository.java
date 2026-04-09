package com.pdfmanager.repository;

import com.pdfmanager.model.PdfDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository Layer — only data-access concerns live here.
 *
 * Spring Data JPA generates the SQL; we only declare method signatures
 * or @Query for anything non-trivial.
 */
@Repository
public interface PdfDocumentRepository extends JpaRepository<PdfDocument, Long> {

    /**
     * Find a document without loading its binary data
     * (avoids pulling 50 MB into memory just to read metadata).
     */
    @Query("""
            SELECT new com.pdfmanager.model.PdfDocument(
                p.id, p.fileName, p.contentType, p.fileSize,
                null, p.filePath, p.description,
                p.uploadedAt, p.lastAccessedAt)
            FROM PdfDocument p WHERE p.id = :id
            """)
    Optional<PdfDocument> findMetadataById(@Param("id") Long id);

    /**
     * Return metadata for every document (no binary blobs in the list).
     */
    @Query("""
            SELECT new com.pdfmanager.model.PdfDocument(
                p.id, p.fileName, p.contentType, p.fileSize,
                null, p.filePath, p.description,
                p.uploadedAt, p.lastAccessedAt)
            FROM PdfDocument p ORDER BY p.uploadedAt DESC
            """)
    List<PdfDocument> findAllMetadata();

    /** Case-insensitive filename search */
    List<PdfDocument> findByFileNameContainingIgnoreCase(String fileName);

    /** Touch lastAccessedAt after a download */
    @Modifying
    @Transactional
    @Query("UPDATE PdfDocument p SET p.lastAccessedAt = :ts WHERE p.id = :id")
    void updateLastAccessedAt(@Param("id") Long id, @Param("ts") LocalDateTime ts);
}
