package com.company.ingest.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * ByteSource that reads from a file (for replaying bytes to sink)
 */
public class FileByteSource implements ByteSource {
    private final Path filePath;
    private final InputStream input;
    private final boolean deleteOnClose;
    
    public FileByteSource(Path filePath, boolean deleteOnClose) throws IOException {
        this.filePath = filePath;
        this.input = Files.newInputStream(filePath);
        this.deleteOnClose = deleteOnClose;
    }
    
    @Override
    public byte[] nextChunk() throws IOException {
        byte[] buffer = new byte[8192];
        int bytesRead = input.read(buffer);
        
        if (bytesRead <= 0) {
            return new byte[0];
        }
        
        return bytesRead < buffer.length ? Arrays.copyOf(buffer, bytesRead) : buffer;
    }
    
    @Override
    public void close() throws IOException {
        try {
            input.close();
        } finally {
            if (deleteOnClose) {
                try {
                    Files.deleteIfExists(filePath);
                } catch (IOException e) {
                    System.err.println("Warning: Failed to delete temp file: " + filePath);
                }
            }
        }
    }
}