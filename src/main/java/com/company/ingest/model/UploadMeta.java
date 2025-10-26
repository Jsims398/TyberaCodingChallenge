package com.company.ingest.model;

import java.util.Optional;

/**
 * Metadata provided by the uploader for a document
 */
public class UploadMeta {
    private final String filename;
    private final String claimedMime;
    private final Optional<Long> contentLength;

    public UploadMeta(String filename, String claimedMime, Optional<Long> contentLength) {
        this.filename = filename;
        this.claimedMime = claimedMime;
        this.contentLength = contentLength;
    }

    public String getFilename() {
        return filename;
    }

    public String getClaimedMime() {
        return claimedMime;
    }

    public Optional<Long> getContentLength() {
        return contentLength;
    }
}