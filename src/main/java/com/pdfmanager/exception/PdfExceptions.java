package com.pdfmanager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public final class PdfExceptions  extends GlobalExceptionHandler
{

    private PdfExceptions() {}

    // ------------------------------------------------------------------ //

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class PdfNotFoundException extends RuntimeException {
        public PdfNotFoundException(Long id) {
            super("PDF not found with id: " + id);
        }
        public PdfNotFoundException(String message) {
            super(message);
        }
    }

    // ------------------------------------------------------------------ //

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class InvalidPdfException extends RuntimeException {
        public InvalidPdfException(String message) {
            super(message);
        }
    }

    // ------------------------------------------------------------------ //

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public static class StorageException extends RuntimeException {
        public StorageException(String message) {
            super(message);
        }
        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
