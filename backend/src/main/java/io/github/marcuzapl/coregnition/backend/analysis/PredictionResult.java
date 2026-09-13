package io.github.marcuzapl.coregnition.backend.analysis;

import java.util.Map;

public record PredictionResult(
    String suggestedLabel,
    double confidence,
    Map<String, Double> classScores,
    boolean isUnknown,
    String modelId,
    String modelChecksum,
    String preprocessingVersion
) {}
