package com.company.ingest.tools;

import com.company.ingest.core.Ingestor;
import com.company.ingest.io.ByteSource;
import com.company.ingest.io.InputStreamByteSource;
import com.company.ingest.model.IngestConfig;
import com.company.ingest.model.IngestResult;
import com.company.ingest.model.UploadMeta;
import com.company.ingest.sink.IngestSink;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;

/**
 * Small manual runner to exercise Ingestor without running the test framework.
 * Run from project root after `mvn package` with:
 *   java -cp target/classes;target/dependency/* com.company.ingest.tools.ManualTestRunner
 */
public class ManualTestRunner {

    public static void main(String[] args) throws Exception {
        System.out.println("ManualTestRunner starting...");

        Ingestor ingestor = new Ingestor();

        IngestConfig config = new IngestConfig(
                1_000_000,
                Set.of("application/pdf", "image/png", "application/octet-stream")
        );

        // Simple PDF-like payload
        byte[] pdfData = new byte[]{'%','P','D','F', ' ', 'm','o','c','k'};

        UploadMeta meta = new UploadMeta("manual.pdf", "application/pdf", Optional.of((long) pdfData.length));

        SimpleSink sink = new SimpleSink();

        try (ByteSource src = new InputStreamByteSource(new ByteArrayInputStream(pdfData))) {
            ingestor.ingest(meta, config, src, sink);
        }

        IngestResult res = sink.getLastResult();
        System.out.println("Ingest result: ok=" + res.isOk());
        System.out.println("Detected MIME: " + res.getDetectedMime());
        System.out.println("Size: " + res.getSize());
        System.out.println("SHA-256: " + res.getSha256());
        System.out.println("Errors: " + res.getErrors());

        System.out.println("bytes consumed by sink: " + sink.getBytesConsumed());
    }

    private static class SimpleSink implements IngestSink {
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
            try {
                data.close();
            } catch (IOException ignored) {}
        }

        public long getBytesConsumed() { return bytesConsumed; }
        public UploadMeta getLastMeta() { return lastMeta; }
        public IngestResult getLastResult() { return lastResult; }
    }
}
