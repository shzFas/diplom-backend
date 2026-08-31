package kz.bilimedu.api.system;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Два из четырёх публичных эндпоинтов.
 *
 * <p>/docs пока отдаёт сам контракт: springdoc-openapi на момент написания
 * поддерживает только Spring Boot 3.x. Как выйдет совместимая версия —
 * здесь появится сгенерированная спецификация и Swagger UI.
 */
@RestController
@RequestMapping("/api/v1")
public class SystemController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping(value = "/docs", produces = "text/markdown; charset=UTF-8")
    public ResponseEntity<String> docs() throws IOException {
        ClassPathResource contract = new ClassPathResource("api-v1.md");
        String body = new String(contract.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return ResponseEntity.ok().contentType(MediaType.valueOf("text/markdown; charset=UTF-8")).body(body);
    }
}
