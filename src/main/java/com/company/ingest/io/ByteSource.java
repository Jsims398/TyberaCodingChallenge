package com.company.ingest.io;

import java.io.IOException;

/**
 * Represents a finite, read-once source of bytes.
 * Similar to InputStream but with explicit chunking semantics.
 */
public interface ByteSource extends AutoCloseable {
    byte[] nextChunk() throws IOException;
    
    @Override
    void close() throws IOException;
}