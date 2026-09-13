package io.github.marcuzapl.coregnition.backend.workflow;

public record JobRecord(
    String id,
    String projectId,
    String jobType,
    JobStatus status,
    double progress,
    String errorMessage,
    String createdAt,
    String completedAt
) {}
