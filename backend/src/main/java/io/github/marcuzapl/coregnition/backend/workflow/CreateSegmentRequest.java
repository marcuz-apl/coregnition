package io.github.marcuzapl.coregnition.backend.workflow;

public record CreateSegmentRequest(String assetId, Double startDepthFeet, Double endDepthFeet, String orientation, Integer regionX, Integer regionY, Integer regionWidth, Integer regionHeight) {
    public CreateSegmentRequest(String assetId, Double startDepthFeet, Double endDepthFeet, String orientation) { this(assetId, startDepthFeet, endDepthFeet, orientation, null, null, null, null); }
}
