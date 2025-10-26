package com.company.ingest.model;

import java.util.List;

/**
 * Result of validation and processing
 */
public class IngestResult {
    private final String detectedMime;
    private final long size;
    private final String sha256;
    private final boolean ok;
    private final List<String> errors;

    public IngestResult(String detectedMime, long size, String sha256, 
                       boolean ok, List<String> errors) {
        this.detectedMime = detectedMime;
        this.size = size;
        this.sha256 = sha256;
        this.ok = ok;
        this.errors = errors;
    }

    public String getDetectedMime() {
        return detectedMime;
    }

    public long getSize() {
        return size;
    }

    public String getSha256() {
        return sha256;
    }

    public boolean isOk() {
        return ok;
    }

    public List<String> getErrors() {
        return errors;
    }
}