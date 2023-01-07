package com.luckyframework.exception;

import java.io.IOException;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2022/12/20 18:00
 */
public class AnnotationMetadataReaderException extends LuckyIOException {
    public AnnotationMetadataReaderException(IOException ioe) {
        super(ioe);
    }

    public AnnotationMetadataReaderException(String msg, IOException ioe) {
        super(msg, ioe);
    }

    public AnnotationMetadataReaderException(String message) {
        super(message);
    }

    public AnnotationMetadataReaderException(Throwable ex) {
        super(ex);
    }

    public AnnotationMetadataReaderException(String message, Throwable ex) {
        super(message, ex);
    }

    public AnnotationMetadataReaderException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public AnnotationMetadataReaderException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
