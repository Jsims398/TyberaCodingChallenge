package com.company.ingest.io;

import java.io.IOException;

public interface ByteSource extends AutoCloseable {
    byte[] nextChunk() throws IOException;
    
    @Override
    void close() throws IOException;
}