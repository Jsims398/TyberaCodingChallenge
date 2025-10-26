package com.company.ingest.mime;

public class MimeDetector {
    
    /**
     * Detect MIME type from file signature (magic bytes)
     */
    public static String detect(byte[] header) {
        if (header == null || header.length < 4) {
            return "application/octet-stream";
        }
        
        // PDF: %PDF
        if (header.length >= 4 && 
            header[0] == 0x25 && header[1] == 0x50 && 
            header[2] == 0x44 && header[3] == 0x46) {
            return "application/pdf";
        }
        
        // PNG: 89 50 4E 47 0D 0A 1A 0A
        if (header.length >= 8 &&
            header[0] == (byte)0x89 && header[1] == 0x50 && 
            header[2] == 0x4E && header[3] == 0x47 &&
            header[4] == 0x0D && header[5] == 0x0A &&
            header[6] == 0x1A && header[7] == 0x0A) {
            return "image/png";
        }
        
        // DOCX (ZIP-based): PK (50 4B) + look for [Content_Types].xml
        if (header.length >= 4 &&
            header[0] == 0x50 && header[1] == 0x4B &&
            (header[2] == 0x03 || header[2] == 0x05 || header[2] == 0x07) &&
            (header[3] == 0x04 || header[3] == 0x06 || header[3] == 0x08)) {
            
            // Check if it contains Office Open XML signatures
            String headerStr = new String(header, 0, Math.min(header.length, 512));
            if (headerStr.contains("word/") || headerStr.contains("[Content_Types].xml")) {
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            }
            
            return "application/zip";
        }
        
        // JPEG: FF D8 FF
        if (header.length >= 3 &&
            header[0] == (byte)0xFF && header[1] == (byte)0xD8 && header[2] == (byte)0xFF) {
            return "image/jpeg";
        }
        
        return "application/octet-stream";
    }
    
    /**
     * Normalize MIME type (strip parameters like charset)
     */
    public static String normalize(String mime) {
        if (mime == null) {
            return "application/octet-stream";
        }
        
        int semicolon = mime.indexOf(';');
        return semicolon > 0 ? mime.substring(0, semicolon).trim() : mime.trim();
    }
}