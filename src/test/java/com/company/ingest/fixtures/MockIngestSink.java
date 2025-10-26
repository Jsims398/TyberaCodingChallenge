package com.company.ingest.fixtures;

import com.company.ingest.io.ByteSource;
import com.company.ingest.model.IngestResult;
import com.company.ingest.model.UploadMeta;
import com.company.ingest.sink.IngestSink;

import java.io.IOException;

/**
 * Mock sink for testing that counts bytes
 */
public class MockIngestSink implements IngestSink {
    private long bytesConsumed = 0;
    private UploadMeta lastMeta;
    private IngestResult lastResult;
    
    @Override
    public void persist(UploadMeta meta, IngestResult result, ByteSource data) throws IOException {
        this.lastMeta = meta;
        this.lastResult = result;
        this.bytesConsumed = 0;
        
        byte[] chunk;
        while ((chunk = data.nextChunk()).length > 0) {
            bytesConsumed += chunk.length;
        }
    }
    
    public long getBytesConsumed() {
        return bytesConsumed;
    }
    
    public UploadMeta getLastMeta() {
        return lastMeta;
    }
    
    public IngestResult getLastResult() {
        return lastResult;
    }
    
    public void reset() {
        bytesConsumed = 0;
        lastMeta = null;
        lastResult = null;
    }
}