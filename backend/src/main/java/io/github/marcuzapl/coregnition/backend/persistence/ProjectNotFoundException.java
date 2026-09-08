package io.github.marcuzapl.coregnition.backend.persistence;

public class ProjectNotFoundException extends RuntimeException {
    public ProjectNotFoundException(String id) { super("Project not found: " + id); }
}
