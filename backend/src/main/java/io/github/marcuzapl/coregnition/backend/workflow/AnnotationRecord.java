package io.github.marcuzapl.coregnition.backend.workflow;

public record AnnotationRecord(String id, String segmentId, int revision, String label, String reviewState, String createdAt) {}
