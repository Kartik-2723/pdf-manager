package com.pdfmanager.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds the pdf.storage.* keys from application.properties.
 */
@Configuration
@ConfigurationProperties(prefix = "pdf.storage")
@Data
public class StorageProperties {

    /** DB or FILESYSTEM */
    private StorageStrategy strategy = StorageStrategy.DB;

    private Filesystem filesystem = new Filesystem();

    @Data
    public static class Filesystem {
        /** Root directory for PDF files when strategy=FILESYSTEM */
        private String path = "./uploads/pdfs";
    }
}
