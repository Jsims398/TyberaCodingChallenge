package com.company.ingest.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Adapter to use InputStream as ByteSource
 */
public class InputStreamByteSource implements ByteSource {
    private final InputStream input;
    private final int chunkSize;
    
    public InputStreamByteSource(InputStream input, int chunkSize) {
        this.input = input;
        this.chunkSize = chunkSize;
    }
    
    public InputStreamByteSource(InputStream input) {
        this(input, 8192); // 8KB default
    }
    
    @Override
    public byte[] nextChunk() throws IOException {
        byte[] buffer = new byte[chunkSize];
        int bytesRead = input.read(buffer);
        
        if (bytesRead <= 0) {
            return new byte[0]; // EOF
        }
        
        if (bytesRead < chunkSize) {
            return Arrays.copyOf(buffer, bytesRead);
        }
        
        return buffer;
    }
    
    @Override
    public void close() throws IOException {
        input.close();
    }
}