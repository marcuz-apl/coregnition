package io.github.marcuzapl.coregnition.backend.workflow;

public record CreateSegmentRequest(String assetId, Double startDepthFeet, Double endDepthFeet, String orientation) {}
