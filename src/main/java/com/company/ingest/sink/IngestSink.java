package com.company.ingest.sink;

import com.company.ingest.io.ByteSource;
import com.company.ingest.model.IngestResult;
import com.company.ingest.model.UploadMeta;

import java.io.IOException;

/**
 * Receives validated uploads for downstream processing
 */
public interface IngestSink {
    /**
     * Persist the upload with its validation result
     */
    void persist(UploadMeta meta, IngestResult result, ByteSource data) throws IOException;
}