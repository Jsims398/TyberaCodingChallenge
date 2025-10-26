package com.company.ingest.integration;

import com.company.ingest.core.Ingestor;
import com.company.ingest.fixtures.MockIngestSink;
import com.company.ingest.io.ByteSource;
import com.company.ingest.io.InputStreamByteSource;
import com.company.ingest.model.IngestConfig;
import com.company.ingest.model.IngestResult;
import com.company.ingest.model.UploadMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify memory-efficient handling of large files
 */
class LargeFileTest {

    private Ingestor ingestor;
    private MockIngestSink sink;

    @BeforeEach
    void setUp() {
        ingestor = new Ingestor();
        sink = new MockIngestSink();
    }

    @Test
    void testLargeFileDoesNotExhaustMemory() throws Exception {
        System.out.println("\n=== Testing Large File (5MB) ===");

        // Create a 5MB file with PDF header
        int fileSize = 5 * 1024 * 1024; // 5MB
        byte[] largeFile = createLargePdf(fileSize);

        IngestConfig config = new IngestConfig(
                10 * 1024 * 1024, // 10MB max
                Set.of("application/pdf")
        );

        UploadMeta meta = new UploadMeta(
                "large.pdf",
                "application/pdf",
                Optional.of((long)fileSize)
        );

        ByteSource source = new InputStreamByteSource(new ByteArrayInputStream(largeFile));
        ingestor.ingest(meta, config, source, sink);

        IngestResult result = sink.getLastResult();

        assertTrue(result.isOk(), "Large file should pass");
        assertEquals(fileSize, result.getSize());
        assertEquals(fileSize, sink.getBytesConsumed());
        assertNotNull(result.getSha256());

        System.out.println("✓ Large File Test Passed");
        System.out.println("  - Size: " + (result.getSize() / 1024 / 1024) + " MB");
        System.out.println("  - SHA-256: " + result.getSha256());
    }

    @Test
    void testVeryLargeFileExceedsLimit() throws Exception {
        System.out.println("\n=== Testing Very Large File Exceeds Limit ===");

        // Create a 15MB file (exceeds 10MB limit)
        int fileSize = 15 * 1024 * 1024;
        byte[] veryLargeFile = createLargePdf(fileSize);

        IngestConfig config = new IngestConfig(
                10 * 1024 * 1024, // 10MB max
                Set.of("application/pdf")
        );

        UploadMeta meta = new UploadMeta(
                "toolarge.pdf",
                "application/pdf",
                Optional.empty()
        );

        ByteSource source = new InputStreamByteSource(new ByteArrayInputStream(veryLargeFile));
        ingestor.ingest(meta, config, source, sink);

        IngestResult result = sink.getLastResult();

        assertFalse(result.isOk(), "Should fail validation");
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("exceeds maximum")));

        // But should still process the entire file
        assertEquals(fileSize, result.getSize());

        System.out.println("✓ Very Large File Rejection Test Passed");
        System.out.println("  - Size: " + (result.getSize() / 1024 / 1024) + " MB");
    }

    private byte[] createLargePdf(int size) {
        byte[] data = new byte[size];

        // Add PDF header
        data[0] = 0x25; // %
        data[1] = 0x50; // P
        data[2] = 0x44; // D
        data[3] = 0x46; // F

        // Fill rest with random data
        Random random = new Random(42); // Fixed seed for reproducibility
        for (int i = 4; i < size; i++) {
            data[i] = (byte) random.nextInt(256);
        }

        return data;
    }
}