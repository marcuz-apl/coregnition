package io.github.marcuzapl.coregnition.backend;

import io.github.marcuzapl.coregnition.backend.persistence.ProjectNotFoundException;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(ProjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String, String> notFound(ProjectNotFoundException exception) { return Map.of("error", exception.getMessage()); }

    @ExceptionHandler({IllegalArgumentException.class, DuplicateKeyException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> badRequest(RuntimeException exception) { return Map.of("error", exception.getMessage()); }
}
