package com.pdfmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * All DTOs are in this file for easy reference.
 *
 * PdfUploadResponse  — returned after a successful upload
 * PdfMetadataResponse — lightweight info about a stored PDF (no binary data)
 * ApiResponse<T>     — generic envelope for all responses
 */
public final class PdfDtos {

    private PdfDtos() {}

    // ------------------------------------------------------------------ //
    //  Upload Response                                                      //
    // ------------------------------------------------------------------ //

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PdfUploadResponse {
        private Long id;
        private String fileName;
        private String contentType;
        private Long fileSize;
        private String description;
        private LocalDateTime uploadedAt;
        private String downloadUrl;
        private String viewUrl;
    }

    // ------------------------------------------------------------------ //
    //  Metadata (list / get-by-id without binary payload)                  //
    // ------------------------------------------------------------------ //

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PdfMetadataResponse {
        private Long id;
        private String fileName;
        private String contentType;
        private Long fileSize;
        private String description;
        private LocalDateTime uploadedAt;
        private LocalDateTime lastAccessedAt;
        private String downloadUrl;
        private String viewUrl;
    }

    // ------------------------------------------------------------------ //
    //  Generic API envelope                                                 //
    // ------------------------------------------------------------------ //

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> ApiResponse<T> ok(String message, T data) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message(message)
                    .data(data)
                    .build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder()
                    .success(false)
                    .message(message)
                    .build();
        }
    }
}
