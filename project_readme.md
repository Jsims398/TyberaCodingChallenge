# Document Ingest Pipeline

A protocol-agnostic document validation and forwarding system that handles file uploads from any source (HTTP, gRPC, CLI, etc.), validates them, and forwards them to downstream storage systems.

---

## 📋 Table of Contents

- Overview
- Architecture
- Project Structure
- Key Components
- How It Works
- Running Tests
- Usage Examples

---

## 🎯 Overview

### What This Does

1. **Accepts uploads** from any source (byte stream)
2. **Validates** content in a single pass:
   - Detects MIME type from file content (not filename!)
   - Computes SHA-256 hash
   - Measures file size
   - Checks against validation rules
3. **Forwards** to a pluggable sink (S3, database, filesystem, etc.)
4. **Handles large files** efficiently using temp file streaming (no memory exhaustion)

### Key Features

✅ **Content-based MIME detection** - Don't trust file extensions  
✅ **Single-pass processing** - Read stream only once  
✅ **Memory efficient** - Handles multi-GB files via temp files  
✅ **Pluggable storage** - Swap out sinks without changing validation logic  
✅ **Comprehensive validation** - Size limits, MIME allowlists, hash verification  
✅ **Detailed error reporting** - Know exactly why validation failed

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Upload Stream                            │
│                    (HTTP/gRPC/CLI/etc.)                         │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
                    ┌───────────────┐
                    │  ByteSource   │ ← Abstraction (read-once)
                    └───────┬───────┘
                            │
                            ▼
                    ┌───────────────┐
                    │   Ingestor    │ ← Main validation logic
                    └───────┬───────┘
                            │
                ┌───────────┼───────────┐
                │           │           │
                ▼           ▼           ▼
         [Validate]   [Compute]   [Buffer]
         - Size       - SHA-256   - Temp File
         - MIME       - MIME
         - Rules
                            │
                            ▼
                    ┌───────────────┐
                    │ IngestResult  │ ← Validation outcome
                    └───────┬───────┘
                            │
                            ▼
                    ┌───────────────┐
                    │  IngestSink   │ ← Your storage (pluggable)
                    └───────────────┘
                            │
                ┌───────────┼───────────┐
                │           │           │
                ▼           ▼           ▼
              [S3]      [Database]  [Queue]
```

---

## 📁 Project Structure

```
document-ingest/
├── pom.xml                                    # Maven build config
├── README.md                                  # This file
├── .gitignore
│
├── src/
│   ├── main/
│   │   ├── java/com/company/ingest/
│   │   │   │
│   │   │   ├── model/                        # 📦 Data Models
│   │   │   │   ├── UploadMeta.java           # Upload metadata (filename, claimed MIME, content-length)
│   │   │   │   ├── IngestConfig.java         # Validation rules (max size, accepted MIMEs)
│   │   │   │   └── IngestResult.java         # Validation result (detected MIME, size, SHA-256, errors)
│   │   │   │
│   │   │   ├── io/                           # 📥 I/O Abstractions
│   │   │   │   ├── ByteSource.java           # Interface: read-once byte stream
│   │   │   │   ├── InputStreamByteSource.java # Adapter: InputStream → ByteSource
│   │   │   │   └── FileByteSource.java       # Reads from temp file for replay
│   │   │   │
│   │   │   ├── sink/                         # 📤 Output Interface
│   │   │   │   └── IngestSink.java           # Interface: where validated files go
│   │   │   │
│   │   │   ├── mime/                         # 🔍 MIME Detection
│   │   │   │   └── MimeDetector.java         # Magic byte detection (PDF, PNG, DOCX, JPEG)
│   │   │   │
│   │   │   └── core/                         # ⚙️ Core Logic
│   │   │       ├── Ingestor.java             # Main component: validates & forwards
│   │   │       └── IngestException.java      # Custom exception
│   │   │
│   │   └── resources/
│   │       └── application.properties         # Default configuration
│   │
│   └── test/
│       ├── java/com/company/ingest/
│       │   │
│       │   ├── core/                         # 🧪 Unit Tests
│       │   │   └── IngestorTest.java         # Core validation tests
│       │   │
│       │   ├── integration/                  # 🔬 Integration Tests
│       │   │   ├── RealFileTest.java         # Tests with actual PDF/DOCX/PNG
│       │   │   └── LargeFileTest.java        # Memory efficiency tests
│       │   │
│       │   └── fixtures/                     # 🛠️ Test Utilities
│       │       ├── MockIngestSink.java       # Test sink (counts bytes)
│       │       └── TestDataFactory.java      # Creates mock files
│       │
│       └── resources/
│           └── test-files/                   # 📁 Test Files
│               ├── sample.pdf                # Real PDF for testing
│               ├── sample.docx               # Real DOCX for testing
│               └── sample.png                # Real image for testing
```

---

## 🔑 Key Components

### 1. **ByteSource** (`io/ByteSource.java`)

**What:** Abstraction for a finite, read-once byte stream  
**Why:** Decouples from InputStream, allows chunked reading  
**Implementations:**

- `InputStreamByteSource` - Wraps any InputStream
- `FileByteSource` - Reads from temp file (for replay)

### 2. **Ingestor** (`core/Ingestor.java`)

**What:** Main validation component  
**Responsibilities:**

1. Single-pass processing (hash, size, MIME, temp file)
2. Apply validation rules
3. Forward to sink

**Flow:**

```
source → [process] → tempFile
              ↓
         [validate] → IngestResult
              ↓
         [forward] → sink.persist(meta, result, tempFileSource)
              ↓
         [cleanup] → delete temp file
```

---

### 3. **IngestSink** (`sink/IngestSink.java`)

**What:** Interface for downstream storage  
**Why:** Decouples validation from storage implementation

```java
interface IngestSink {
    void persist(UploadMeta meta, IngestResult result, ByteSource data);
}
```

### 4. **MimeDetector** (`mime/MimeDetector.java`)

**What:** Detects file type from magic bytes  
**Supported Types:**

- PDF: `%PDF` (0x25 0x50 0x44 0x46)
- PNG: `‰PNG` (0x89 0x50 0x4E 0x47...)
- JPEG: `ÿØÿ` (0xFF 0xD8 0xFF)
- DOCX: ZIP signature + Office XML markers

**Why Important:** Don't trust filenames! A `.png` file might actually be JPEG. #sample.png

---

### 5. **Models** (`model/`)

#### UploadMeta

```java
class UploadMeta {
    String filename;              // Original filename
    String claimedMime;           // MIME from uploader (may be wrong!)
    Optional<Long> contentLength; // Size hint (may be absent)
}
```

#### IngestConfig

```java
class IngestConfig {
    long maxContentLength;        // Max file size (bytes)
    Set<String> acceptedMimes;    // Allowlist (e.g., ["application/pdf"])
}
```

#### IngestResult

```java
class IngestResult {
    String detectedMime;          // Actual MIME from content
    long size;                    // Actual size (bytes)
    String sha256;                // SHA-256 hex digest
    boolean ok;                   // true if no errors
    List<String> errors;          // Validation errors
}
```

---

## 🔄 How It Works

### Single-Pass Processing

The key innovation is reading the upload **once** while computing multiple things:

```java
try (OutputStream tempOut = Files.newOutputStream(tempFile)) {
    byte[] chunk;
    while ((chunk = source.nextChunk()).length > 0) {
        // 1. Capture header (first 512 bytes for MIME detection)
        if (header == null) {
            header = Arrays.copyOf(chunk, 512);
        }

        // 2. Update SHA-256 hash
        digest.update(chunk);

        // 3. Write to temp file (for later replay)
        tempOut.write(chunk);

        // 4. Track total size
        totalBytes += chunk.length;
    }
}
```

**Result:** After one pass, we have:

- ✅ MIME type (from header)
- ✅ SHA-256 hash (computed incrementally)
- ✅ Total size (counted)
- ✅ Temp file copy (for forwarding to sink)

### Validation Rules

```java
List<String> errors = new ArrayList<>();

// Rule 1: If content-length provided, must match exactly
if (meta.contentLength.isPresent() && size != meta.contentLength.get()) {
    errors.add("Content length mismatch: expected X, got Y");
}

// Rule 2: Must not exceed max size
if (size > config.maxContentLength) {
    errors.add("File size X exceeds maximum Y");
}

// Rule 3: Detected MIME must be in allowlist
if (!config.acceptedMimes.contains(detectedMime)) {
    errors.add("MIME type 'X' is not in accepted list");
}

// Rule 4: Warn if claimed MIME doesn't match detected
if (claimedMime != detectedMime) {
    errors.add("MIME type mismatch: claimed 'X', detected 'Y'");
}

return errors; // Empty if all rules pass
```

### Forwarding to Sink

```java
// Create a new ByteSource that reads from temp file
try (FileByteSource replaySource = new FileByteSource(tempFile, true)) {
    sink.persist(meta, result, replaySource);
}
// Auto-deletes temp file when done
```

The sink receives:

- ✅ Original metadata (filename, etc.)
- ✅ Validation result (ok/errors)
- ✅ Byte stream to read the file

**Sink decides what to do:**

```java
if (!result.ok) {
    logger.warn("Invalid upload: {}", result.errors);
    // Option A: Don't store
    // Option B: Store in quarantine
    // Option C: Store but flag as invalid
} else {
    s3.upload("bucket", result.sha256 + ".pdf", data);
}
```

## 🧪 Running Tests

### Run All Tests

```bash
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=IngestorTest
mvn test -Dtest=RealFileTest
mvn test -Dtest=LargeFileTest
```

### Run Specific Test Method

```bash
mvn test -Dtest=RealFileTest#testRealPdfFile
```

### Test Coverage

| Test Class      | Purpose           | What It Tests                          |
| --------------- | ----------------- | -------------------------------------- |
| `IngestorTest`  | Unit tests        | Core validation logic with mock data   |
| `RealFileTest`  | Integration tests | Real PDF/DOCX/PNG files from resources |
| `LargeFileTest` | Performance tests | Memory efficiency with 5MB+ files      |

### Expected Test Output

```
=== Running: testRealPdfFile ===
✓ Test passed - SHA-256: a1b2c3d4e5f6...
  - Size: 45123 bytes
  - MIME: application/pdf

=== Running: testContentLengthMismatch ===
✓ Test passed - Error caught: Content length mismatch: expected 100, got 99

=== Testing Real PNG File ===
  - File claims to be: PNG
  - Actually detected as: image/jpeg
  - File size: 24059271 bytes (22 MB)
✓ PNG Test Passed
```

---

## 💻 Usage Examples

### Basic Usage

```java
// 1. Configure validation rules
IngestConfig config = new IngestConfig(
    10_485_760, // 10MB max
    Set.of("application/pdf", "image/png")
);

// 2. Create metadata from upload
UploadMeta meta = new UploadMeta(
    "document.pdf",
    "application/pdf",
    Optional.of(1024L)
);

// 3. Wrap input stream
ByteSource source = new InputStreamByteSource(uploadInputStream);

// 4. Create sink (your storage implementation)
IngestSink sink = new S3IngestSink(s3Client);

// 5. Process!
Ingestor ingestor = new Ingestor();
ingestor.ingest(meta, config, source, sink);
```

### Custom Sink Implementation

```java
public class S3IngestSink implements IngestSink {
    private final AmazonS3 s3Client;
    private final String bucket;

    @Override
    public void persist(UploadMeta meta, IngestResult result, ByteSource data)
            throws IOException {

        if (!result.isOk()) {
            // Handle validation failures
            logger.warn("Upload failed: {}", result.getErrors());
            storeInQuarantine(meta, result, data);
            return;
        }

        // Valid upload - store in S3
        String key = "uploads/" + result.getSha256() + getExtension(result.getDetectedMime());

        // Stream from ByteSource to S3
        try (InputStream inputStream = new ByteSourceInputStream(data)) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(result.getDetectedMime());
            metadata.setContentLength(result.getSize());

            s3Client.putObject(bucket, key, inputStream, metadata);
        }

        logger.info("Uploaded {} ({} bytes) to s3://{}/{}",
            meta.getFilename(), result.getSize(), bucket, key);
    }
}
```

### HTTP Upload Handler Example

```java
@PostMapping("/upload")
public ResponseEntity<UploadResponse> handleUpload(
        @RequestParam("file") MultipartFile file) throws IOException {

    UploadMeta meta = new UploadMeta(
        file.getOriginalFilename(),
        file.getContentType(),
        Optional.of(file.getSize())
    );

    ByteSource source = new InputStreamByteSource(file.getInputStream());

    ingestor.ingest(meta, config, source, sink);

    return ResponseEntity.ok(new UploadResponse("success"));
}
```

---

## 🐛 Common Issues

### Test Files Not Found

**Error:** `sample.pdf not found in test resources`

**Solution:** Place test files in `src/test/resources/test-files/`

```bash
mkdir -p src/test/resources/test-files
cp sample.pdf src/test/resources/test-files/
cp sample.docx src/test/resources/test-files/
cp sample.png src/test/resources/test-files/
```

### File Size Exceeds Maximum

**Error:** `File size X bytes exceeds maximum allowed Y bytes`

**Solution:** Increase `maxContentLength` in config:

```java
IngestConfig config = new IngestConfig(
    50_000_000, // Increase to 50MB
    acceptedMimes
);
```

### MIME Type Not Accepted

**Error:** `MIME type 'image/jpeg' is not in accepted list`

**Solution:** Add the MIME type to `acceptedMimes`:

```java
Set.of(
    "application/pdf",
    "image/png",
    "image/jpeg"  // Add this
)
```

### File Named .png But Actually .jpeg

**This is expected behavior!** The system correctly detected the mismatch.

**Result:**

```java
IngestResult {
    detectedMime: "image/jpeg",
    errors: ["MIME type mismatch: claimed 'image/png', detected 'image/jpeg"]
}
```

The file may still be accepted if `image/jpeg` is in `acceptedMimes`.

---

## 📚 Further Reading

- **MIME Detection:** See `MimeDetector.java` for supported formats
- **Adding New MIME Types:** Add magic byte signatures to `MimeDetector.detect()`
- **Custom Sinks:** Implement `IngestSink` interface
- **Async Processing:** Consider wrapping in CompletableFuture or reactive streams
