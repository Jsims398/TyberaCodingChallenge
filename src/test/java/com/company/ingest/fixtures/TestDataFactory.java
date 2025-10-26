package com.company.ingest.fixtures;

/**
 * Factory for creating test data
 */
public class TestDataFactory {
    
    /**
     * Create a mock PDF file
     */
    public static byte[] createMockPdf() {
        byte[] header = {0x25, 0x50, 0x44, 0x46}; // %PDF
        byte[] content = "Mock PDF content for testing".getBytes();
        byte[] result = new byte[header.length + content.length];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(content, 0, result, header.length, content.length);
        return result;
    }
    
    /**
     * Create a mock PNG file
     */
    public static byte[] createMockPng() {
        return new byte[]{
            (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
            0x00, 0x00, 0x00, 0x01  // Minimal content
        };
    }
    
    /**
     * Create a mock DOCX file (simplified ZIP structure)
     */
    public static byte[] createMockDocx() {
        // PK header + word/ reference
        byte[] header = {0x50, 0x4B, 0x03, 0x04};
        byte[] content = "word/document.xml[Content_Types].xml".getBytes();
        byte[] result = new byte[header.length + content.length];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(content, 0, result, header.length, content.length);
        return result;
    }
    
    /**
     * Create empty file
     */
    public static byte[] createEmptyFile() {
        return new byte[0];
    }
}