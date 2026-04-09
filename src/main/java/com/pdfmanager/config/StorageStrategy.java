package com.pdfmanager.config;

/**
 * Supported PDF storage back-ends.
 *
 * DB         — binary stored as a BLOB column in the database.
 * FILESYSTEM — binary stored on the server's local disk;
 *              only the path is persisted to the DB.
 */
public enum StorageStrategy {
    DB,
    FILESYSTEM
}
