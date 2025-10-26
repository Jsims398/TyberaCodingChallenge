package com.company.ingest.manual;

import com.company.ingest.core.Ingestor;
import com.company.ingest.fixtures.MockIngestSink;
import com.company.ingest.io.ByteSource;
import com.company.ingest.io.InputStreamByteSource;
import com.company.ingest.model.IngestConfig;
import com.company.ingest.model.IngestResult;
import com.company.ingest.model.UploadMeta;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;

/**
 * Simple one-off file tester
 * 
 * USAGE: Change the FILE_PATH constant below and run this class
 * 
 * Run from IDE: Right-click → Run 'SimpleFileTest.main()'
 * Run from Maven: mvn exec:java -Dexec.mainClass="com.company.ingest.manual.SimpleFileTest"
 */
public class SimpleFileTest {
    
    // ⚙️ CONFIGURATION - Change these values
    private static final String FILE_PATH = "src/test/resources/test-files/sample.png";
    private static final String CLAIMED_MIME = "image/png";
    private static final long MAX_SIZE_MB = 50;
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║           Simple File Test                                 ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        try {
            // Load file
            Path path = Paths.get(FILE_PATH);
            if (!Files.exists(path)) {
                System.out.println("❌ File not found: " + FILE_PATH);
                System.out.println("   Update FILE_PATH constant in SimpleFileTest.java");
                return;
            }
            
            long fileSize = Files.size(path);
            
            System.out.println("File:         " + path.toAbsolutePath());
            System.out.println("Size:         " + formatBytes(fileSize));
            System.out.println("Claimed MIME: " + CLAIMED_MIME);
            System.out.println("\n" + "─".repeat(60) + "\n");
            
            // Configure
            IngestConfig config = new IngestConfig(
                MAX_SIZE_MB * 1024 * 1024,
                Set.of(
                    "application/pdf",
                    "image/png",
                    "image/jpeg",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                )
            );
            
            // Create components
            UploadMeta meta = new UploadMeta(
                path.getFileName().toString(),
                CLAIMED_MIME,
                Optional.of(fileSize)
            );
            
            ByteSource source = new InputStreamByteSource(new FileInputStream(path.toFile()));
            MockIngestSink sink = new MockIngestSink();
            Ingestor ingestor = new Ingestor();
            
            // Process
            System.out.println("Processing...\n");
            long start = System.currentTimeMillis();
            ingestor.ingest(meta, config, source, sink);
            long duration = System.currentTimeMillis() - start;
            
            // Results
            IngestResult result = sink.getLastResult();
            
            System.out.println("═".repeat(60));
            System.out.println("RESULTS");
            System.out.println("═".repeat(60));
            
            if (result.isOk()) {
                System.out.println("✅ PASSED - File is valid!\n");
            } else {
                System.out.println("❌ FAILED - File has validation errors\n");
            }
            
            System.out.println("Detected MIME: " + result.getDetectedMime());
            System.out.println("File Size:     " + formatBytes(result.getSize()));
            System.out.println("SHA-256:       " + result.getSha256());
            System.out.println("Processing:    " + duration + " ms");
            
            if (!result.getErrors().isEmpty()) {
                System.out.println("\nErrors:");
                for (String error : result.getErrors()) {
                    System.out.println("  • " + error);
                }
            }
            
            System.out.println("\n" + "═".repeat(60));
            
        } catch (Exception e) {
            System.out.println("\n❌ Exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / 1024.0 / 1024.0);
        return String.format("%.2f GB", bytes / 1024.0 / 1024.0 / 1024.0);
    }
}
