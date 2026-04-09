package com.pdfmanager.exception;

import com.pdfmanager.dto.PdfDtos.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Centralised error handling — translates exceptions into clean JSON responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends Exception{

    @ExceptionHandler(PdfExceptions.PdfNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(PdfExceptions.PdfNotFoundException ex) {
        log.warn("PDF not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(PdfExceptions.InvalidPdfException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalid(PdfExceptions.InvalidPdfException ex) {
        log.warn("Invalid PDF: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(PdfExceptions.StorageException.class)
    public ResponseEntity<ApiResponse<Void>> handleStorage(PdfExceptions.StorageException ex) {
        log.error("Storage error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Storage error: " + ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxSize(MaxUploadSizeExceededException ex) {
        log.warn("Upload size exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error("File size exceeds the allowed limit (50 MB)."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred."));
    }
}
