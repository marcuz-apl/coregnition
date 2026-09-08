package io.github.marcuzapl.coregnition.backend.workflow;

public record AssetRecord(String id, String projectId, String originalName, String relativePath, String sha256, int width, int height, Integer bitDepth, Integer colorType, String createdAt) {}
