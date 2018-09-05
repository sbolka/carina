package com.qaprosoft.carina.core.foundation.exception;

public class AnnotationResolverException extends RuntimeException {

    private static final long serialVersionUID = -6807389430302668792L;

    public AnnotationResolverException() {
        super();
    }

    public AnnotationResolverException(String message) {
        super("Annotation exception: " + message);
    }
}
