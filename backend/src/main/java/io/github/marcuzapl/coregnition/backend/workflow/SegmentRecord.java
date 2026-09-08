package io.github.marcuzapl.coregnition.backend.workflow;

public record SegmentRecord(String id, String projectId, String assetId, double startDepthFeet, double endDepthFeet, String orientation, String createdAt) {}
