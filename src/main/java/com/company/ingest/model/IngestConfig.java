package com.company.ingest.model;

import java.util.Set;

/**
 * Configuration for the ingest process
 */
public class IngestConfig {
    private final long maxContentLength;
    private final Set<String> acceptedMimes;

    public IngestConfig(long maxContentLength, Set<String> acceptedMimes) {
        this.maxContentLength = maxContentLength;
        this.acceptedMimes = acceptedMimes;
    }

    public long getMaxContentLength() {
        return maxContentLength;
    }

    public Set<String> getAcceptedMimes() {
        return acceptedMimes;
    }
}