# PDF Manager — Spring Boot

A clean, production-ready REST API for uploading, storing, and downloading PDF files.

## Architecture

```
HTTP Request
     │
     ▼
┌─────────────────────────────────┐
│       PdfController             │  ← HTTP only (routes, headers, status)
│   /api/v1/pdfs/*                │
└───────────────┬─────────────────┘
                │
                ▼
┌─────────────────────────────────┐
│         PdfService              │  ← Business logic (validate, map, audit)
└──────────┬──────────┬───────────┘
           │          │
    DB Path │          │ Filesystem Path
           ▼          ▼
┌─────────────────────────────────┐
│   PdfDocumentRepository         │  ← Data access (Spring Data JPA)
│   (Spring Data JPA)             │
└─────────────────────────────────┘
           │
           ▼
    H2 / PostgreSQL
```

## Features

| Feature | Detail |
|---|---|
| Upload PDF | Multipart POST, 50 MB limit |
| Validate file type | Apache Tika detects actual MIME from bytes |
| Dual storage | **DB** (BLOB) or **Filesystem** (configurable) |
| Download | `Content-Disposition: attachment` |
| View inline | `Content-Disposition: inline` |
| List all | Returns metadata, no binary data |
| Search | Partial filename search |
| Delete | Removes DB record + optional disk file |
| Audit | `uploadedAt`, `lastAccessedAt` auto-managed |
| Error handling | Global handler → clean JSON responses |

## Quick Start

```bash
# Build
./mvnw clean package -DskipTests

# Run
./mvnw spring-boot:run
```

App runs at `http://localhost:8080`  
H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:pdfdb`)

## API Reference

### Upload PDF
```bash
curl -X POST http://localhost:8080/api/v1/pdfs/upload \
  -F "file=@/path/to/document.pdf" \
  -F "description=Q4 Report"
```

**Response:**
```json
{
  "success": true,
  "message": "PDF uploaded successfully.",
  "data": {
    "id": 1,
    "fileName": "document.pdf",
    "fileSize": 245760,
    "uploadedAt": "2024-03-15T10:30:00",
    "downloadUrl": "/api/v1/pdfs/1/download",
    "viewUrl": "/api/v1/pdfs/1/view"
  }
}
```

### List All PDFs
```bash
curl http://localhost:8080/api/v1/pdfs
```

### Get PDF Metadata
```bash
curl http://localhost:8080/api/v1/pdfs/1
```

### Download PDF
```bash
curl -O http://localhost:8080/api/v1/pdfs/1/download
```

### View PDF in Browser
```
http://localhost:8080/api/v1/pdfs/1/view
```

### Search PDFs
```bash
curl "http://localhost:8080/api/v1/pdfs/search?name=invoice"
```

### Delete PDF
```bash
curl -X DELETE http://localhost:8080/api/v1/pdfs/1
```

## Storage Strategy

Switch between DB and filesystem storage in `application.properties`:

```properties
# Store in DB (default)
pdf.storage.strategy=DB

# Store on filesystem
pdf.storage.strategy=FILESYSTEM
pdf.storage.filesystem.path=./uploads/pdfs
```

## Switch to PostgreSQL

1. Uncomment the PostgreSQL dependency in `pom.xml`
2. Update `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/pdfdb
spring.datasource.username=your_user
spring.datasource.password=your_password
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
```

## Running Tests

```bash
./mvnw test
```

## Project Structure

```
src/
├── main/java/com/pdfmanager/
│   ├── PdfManagerApplication.java    # Entry point
│   ├── config/
│   │   ├── StorageStrategy.java      # DB / FILESYSTEM enum
│   │   └── StorageProperties.java    # @ConfigurationProperties
│   ├── controller/
│   │   └── PdfController.java        # REST endpoints
│   ├── service/
│   │   └── PdfService.java           # Business logic
│   ├── repository/
│   │   └── PdfDocumentRepository.java # Data access
│   ├── model/
│   │   └── PdfDocument.java          # JPA entity
│   ├── dto/
│   │   └── PdfDtos.java              # Request/Response DTOs
│   └── exception/
│       ├── PdfExceptions.java        # Custom exceptions
│       └── GlobalExceptionHandler.java
└── resources/
    └── application.properties
```
