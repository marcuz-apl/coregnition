package io.github.marcuzapl.coregnition.backend.workflow;

public record SegmentRecord(String id, String projectId, String assetId, double startDepthFeet, double endDepthFeet, String orientation, String createdAt, Integer regionX, Integer regionY, Integer regionWidth, Integer regionHeight) {
    public SegmentRecord(String id, String projectId, String assetId, double startDepthFeet, double endDepthFeet, String orientation, String createdAt) { this(id, projectId, assetId, startDepthFeet, endDepthFeet, orientation, createdAt, null, null, null, null); }
}
