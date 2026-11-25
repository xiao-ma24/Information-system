package edu.campus.auth;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/** 统一错误响应，便于各组前端与后端按同一格式处理。 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handle(ResponseStatusException e) {
        int status = e.getStatusCode().value();
        String message = e.getReason() == null ? "请求失败" : e.getReason();
        return ResponseEntity.status(status).body(Map.of("error", code(status), "message", message));
    }

    private static String code(int status) {
        return switch (status) {
            case 400 -> "bad_request";
            case 401 -> "unauthenticated";
            case 403 -> "forbidden";
            case 503 -> "service_unavailable";
            default -> "error";
        };
    }
}
