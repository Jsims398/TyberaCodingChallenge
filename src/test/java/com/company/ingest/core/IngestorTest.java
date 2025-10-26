package com.company.ingest.core;

import com.company.ingest.fixtures.MockIngestSink;
import com.company.ingest.fixtures.TestDataFactory;
import com.company.ingest.io.ByteSource;
import com.company.ingest.io.InputStreamByteSource;
import com.company.ingest.model.IngestConfig;
import com.company.ingest.model.UploadMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the main Ingestor component
 */
class IngestorTest {
    
    private Ingestor ingestor;
    private MockIngestSink sink;
    private IngestConfig defaultConfig;
    
    @BeforeEach
    void setUp() {
        ingestor = new Ingestor();
        sink = new MockIngestSink();
        defaultConfig = new IngestConfig(
            1_000_000,
            Set.of("application/pdf", "image/png", 
                   "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        );
    }
    
    @Test
    void testHappyPathPdf() throws Exception {
        byte[] pdfData = TestDataFactory.createMockPdf();
        
        UploadMeta meta = new UploadMeta(
            "test.pdf",
            "application/pdf",
            Optional.of((long)pdfData.length)
        );
        
        ByteSource source = new InputStreamByteSource(new ByteArrayInputStream(pdfData));
        ingestor.ingest(meta, defaultConfig, source, sink);
        
        assertTrue(sink.getLastResult().isOk(), "Should be ok");
        assertEquals("application/pdf", sink.getLastResult().getDetectedMime());
        assertEquals(pdfData.length, sink.getLastResult().getSize());
        assertEquals(pdfData.length, sink.getBytesConsumed());
        assertNotNull(sink.getLastResult().getSha256());
        assertEquals(64, sink.getLastResult().getSha256().length()); // SHA-256 hex length
    }
    
    @Test
    void testHappyPathPng() throws Exception {
        byte[] pngData = TestDataFactory.createMockPng();
        
        UploadMeta meta = new UploadMeta(
            "test.png",
            "image/png",
            Optional.of((long)pngData.length)
        );
        
        ByteSource source = new InputStreamByteSource(new ByteArrayInputStream(pngData));
        ingestor.ingest(meta, defaultConfig, source, sink);
        
        assertTrue(sink.getLastResult().isOk());
        assertEquals("image/png", sink.getLastResult().getDetectedMime());
        assertEquals(pngData.length, sink.getLastResult().getSize());
    }

    @Test
    void testHappyPathDocx() throws Exception {
        byte[] docxData = TestDataFactory.createMockDocx();

        UploadMeta meta = new UploadMeta(
            "test.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            Optional.of((long)docxData.length)
        );

        ByteSource source = new InputStreamByteSource(new ByteArrayInputStream(docxData));
        ingestor.ingest(meta, defaultConfig, source, sink);

        assertTrue(sink.getLastResult().isOk(), "Docx should be accepted");
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", 
                     sink.getLastResult().getDetectedMime());
        assertEquals(docxData.length, sink.getLastResult().getSize());
        assertEquals(docxData.length, sink.getBytesConsumed());
        assertNotNull(sink.getLastResult().getSha256());
        assertEquals(64, sink.getLastResult().getSha256().length()); // SHA-256 hex length
    }
    
    @Test
    void testContentLengthMismatch() throws Exception {
        byte[] data = TestDataFactory.createMockPdf();
        
        UploadMeta meta = new UploadMeta(
            "test.pdf",
            "application/pdf",
            Optional.of((long)data.length + 1) // Wrong!
        );
        
        ByteSource source = new InputStreamByteSource(new ByteArrayInputStream(data));
        ingestor.ingest(meta, defaultConfig, source, sink);
        
        assertFalse(sink.getLastResult().isOk());
        assertTrue(sink.getLastResult().getErrors().stream()
            .anyMatch(e -> e.contains("Content length mismatch")));
    }
    
    @Test
    void testExceedsMaxSize() throws Exception {
        byte[] data = TestDataFactory.createMockPdf();
        IngestConfig smallConfig = new IngestConfig(10, Set.of("application/pdf"));
        
        UploadMeta meta = new UploadMeta(
            "test.pdf",
            "application/pdf",
            Optional.empty()
        );
        
        ByteSource source = new InputStreamByteSource(new ByteArrayInputStream(data));
        ingestor.ingest(meta, smallConfig, source, sink);
        
        assertFalse(sink.getLastResult().isOk());
        assertTrue(sink.getLastResult().getErrors().stream()
            .anyMatch(e -> e.contains("exceeds maximum")));
    }
    
    @Test
    void testMimeNotAccepted() throws Exception {
        byte[] data = TestDataFactory.createMockPdf();
        IngestConfig restrictiveConfig = new IngestConfig(1_000_000, Set.of("image/png"));
        
        UploadMeta meta = new UploadMeta(
            "test.pdf",
            "application/pdf",
            Optional.empty()
        );
        
        ByteSource source = new InputStreamByteSource(new ByteArrayInputStream(data));
        ingestor.ingest(meta, restrictiveConfig, source, sink);
        
        assertFalse(sink.getLastResult().isOk());
        assertTrue(sink.getLastResult().getErrors().stream()
            .anyMatch(e -> e.contains("not in accepted list")));
    }
    
    @Test
    void testMissingContentLength() throws Exception {
        byte[] data = TestDataFactory.createMockPdf();
        
        UploadMeta meta = new UploadMeta(
            "test.pdf",
            "application/pdf",
            Optional.empty() // No content length
        );
        
        ByteSource source = new InputStreamByteSource(new ByteArrayInputStream(data));
        ingestor.ingest(meta, defaultConfig, source, sink);
        
        assertTrue(sink.getLastResult().isOk());
        assertEquals(data.length, sink.getLastResult().getSize());
    }
    
    @Test
    void testEmptyFile() throws Exception {
        byte[] data = TestDataFactory.createEmptyFile();
        IngestConfig config = new IngestConfig(
            1_000_000,
            Set.of("application/octet-stream")
        );
        
        UploadMeta meta = new UploadMeta(
            "empty.bin",
            "application/octet-stream",
            Optional.of(0L)
        );
        
        ByteSource source = new InputStreamByteSource(new ByteArrayInputStream(data));
        ingestor.ingest(meta, config, source, sink);
        
        assertEquals(0, sink.getLastResult().getSize());
        assertEquals(0, sink.getBytesConsumed());
    }
    
    @Test
    void testMimeMismatchWarning() throws Exception {
        byte[] pdfData = TestDataFactory.createMockPdf();
        
        UploadMeta meta = new UploadMeta(
            "test.pdf",
            "image/png", // Wrong claim!
            Optional.of((long)pdfData.length)
        );
        
        ByteSource source = new InputStreamByteSource(new ByteArrayInputStream(pdfData));
        ingestor.ingest(meta, defaultConfig, source, sink);
        
        // Should detect PDF correctly
        assertEquals("application/pdf", sink.getLastResult().getDetectedMime());
        
        // Should have mismatch error
        assertTrue(sink.getLastResult().getErrors().stream()
            .anyMatch(e -> e.contains("MIME type mismatch")));
    }
}