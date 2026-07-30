package guru.springframework.ghd.controllers.api;

import guru.springframework.ghd.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public abstract class BaseController {

    protected <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(
                ApiResponse.<T>builder()
                        .status(HttpStatus.OK.value())
                        .body(data)
                        .build()
        );
    }

    protected <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<T>builder()
                        .status(HttpStatus.CREATED.value())
                        .body(data)
                        .build());
    }

    protected <T> ResponseEntity<ApiResponse<T>> ng(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<T>builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .message(message)
                        .build());
    }

    protected ResponseEntity<ApiResponse<Void>> noContent() {
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}

