package com.company.ingest.core;

import java.io.IOException;

/**
 * Exception thrown during ingest processing
 */
public class IngestException extends IOException {
    
    public IngestException(String message) {
        super(message);
    }
    
    public IngestException(String message, Throwable cause) {
        super(message, cause);
    }
}