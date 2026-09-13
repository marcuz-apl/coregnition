package io.github.marcuzapl.coregnition.backend.workflow;

public record PredictionRecord(
    String id,
    String projectId,
    String jobId,
    String segmentId,
    String suggestedLabel,
    double confidence,
    String classScores,
    boolean isUnknown,
    String modelId,
    String modelChecksum,
    String preprocessingVersion,
    String sourceAssetChecksum,
    String createdAt
) {}
