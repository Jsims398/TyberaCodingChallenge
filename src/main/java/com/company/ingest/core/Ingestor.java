package com.company.ingest.core;

import com.company.ingest.io.ByteSource;
import com.company.ingest.io.FileByteSource;
import com.company.ingest.mime.MimeDetector;
import com.company.ingest.model.IngestConfig;
import com.company.ingest.model.IngestResult;
import com.company.ingest.model.UploadMeta;
import com.company.ingest.sink.IngestSink;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Ingestor {

    public void ingest(UploadMeta meta, IngestConfig config, 
                      ByteSource source, IngestSink sink) throws IOException {
        
        Path tempFile = null;
        
        try {
            tempFile = Files.createTempFile("ingest-", ".tmp");
            
            // Process: compute hash, size, detect MIME, write to temp file
            ProcessResult processResult = processSource(source, tempFile, config.getMaxContentLength());
            
            List<String> errors = validate(meta, config, processResult);
            
            boolean ok = errors.isEmpty();
            IngestResult result = new IngestResult(
                processResult.detectedMime,
                processResult.size,
                processResult.sha256,
                ok,
                errors
            );
            
            // Forward to sink using temp file as source
            try (FileByteSource replaySource = new FileByteSource(tempFile, true)) {
                sink.persist(meta, result, replaySource);
            }
            
            tempFile = null;
            
        }
        catch (NoSuchAlgorithmException e) {
            throw new IngestException("SHA-256 algorithm not available", e);
        }
        finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    System.err.println("Warning: Failed to delete temp file: " + tempFile);
                }
            }
        }
    }
    
    /**
     * Internal result from processing the byte source
     */
    private static class ProcessResult {
        final String detectedMime;
        final long size;
        final String sha256;
        final byte[] header;
        
        ProcessResult(String detectedMime, long size, String sha256, byte[] header) {
            this.detectedMime = detectedMime;
            this.size = size;
            this.sha256 = sha256;
            this.header = header;
        }
    }
    
    /**
     * Read source once: compute hash, size, MIME, and write to temp file
     */
    private ProcessResult processSource(ByteSource source, Path tempFile, long maxContentLength) 
            throws IOException, NoSuchAlgorithmException {
        
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        long totalBytes = 0;
        byte[] header = null;
        
        try (OutputStream tempOut = Files.newOutputStream(tempFile)) {
            
            byte[] chunk;
            while ((chunk = source.nextChunk()).length > 0) {
                
                // Capture header for MIME detection (first 512 bytes)
                if (header == null) {
                    header = Arrays.copyOf(chunk, Math.min(chunk.length, 512));
                } else if (header.length < 512) {
                    byte[] extended = new byte[Math.min(512, header.length + chunk.length)];
                    System.arraycopy(header, 0, extended, 0, header.length);
                    System.arraycopy(chunk, 0, extended, header.length, 
                                   Math.min(chunk.length, 512 - header.length));
                    header = extended;
                }
                
                // Update hash
                digest.update(chunk);
                
                // Write to temp file
                tempOut.write(chunk);
                
                // Track size
                totalBytes += chunk.length;
                
                // Early exit if exceeding max (but still write to temp for completeness)
                if (totalBytes > maxContentLength) {
                    // Continue reading to completion but we know it's too large
                }
            }
        }
        
        if (header == null) {
            header = new byte[0];
        }
        
        String detectedMime = MimeDetector.detect(header);
        String sha256Hex = bytesToHex(digest.digest());
        
        return new ProcessResult(detectedMime, totalBytes, sha256Hex, header);
    }
    
    /**
     * Validate the processed upload
     */
    private List<String> validate(UploadMeta meta, IngestConfig config, ProcessResult proc) {
        List<String> errors = new ArrayList<>();
        
        // Check content length match if provided
        if (meta.getContentLength().isPresent()) {
            long expected = meta.getContentLength().get();
            if (proc.size != expected) {
                errors.add(String.format(
                    "Content length mismatch: expected %d bytes, got %d bytes", 
                    expected, proc.size));
            }
        }
        
        // Check max content length
        if (proc.size > config.getMaxContentLength()) {
            errors.add(String.format(
                "File size %d bytes exceeds maximum allowed %d bytes",
                proc.size, config.getMaxContentLength()));
        }
        
        // Check MIME type acceptance
        String normalizedDetected = MimeDetector.normalize(proc.detectedMime);
        if (!config.getAcceptedMimes().contains(normalizedDetected)) {
            errors.add(String.format(
                "MIME type '%s' is not in accepted list: %s",
                normalizedDetected, config.getAcceptedMimes()));
        }
        
        // Optional: warn if claimed MIME doesn't match detected
        String normalizedClaimed = MimeDetector.normalize(meta.getClaimedMime());
        if (!normalizedClaimed.equals(normalizedDetected)) {
            // This is informational, not necessarily an error
            errors.add(String.format(
                "MIME type mismatch: claimed '%s', detected '%s'",
                normalizedClaimed, normalizedDetected));
        }
        
        return errors;
    }
    
    /**
     * Convert byte array to hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}