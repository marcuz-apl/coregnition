package io.github.marcuzapl.coregnition.backend;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
class HealthController {
    @GetMapping("/health")
    Map<String, String> health() {
        return Map.of("service", "coregnition-backend", "status", "ready");
    }
}
