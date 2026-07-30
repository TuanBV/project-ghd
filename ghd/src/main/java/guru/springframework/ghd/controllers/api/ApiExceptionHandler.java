package guru.springframework.ghd.controllers.api;

import guru.springframework.ghd.dto.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MultipartException;

import java.util.*;

@ControllerAdvice(basePackages = "guru.springframework.ghd.controllers.api")
public class ApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);
    // Validation error
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBindErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errorList = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(err -> {
            String fieldName = err.getField();
            String errorMessage = err.getDefaultMessage();

            logger.error("Validation error - Field: '{}', Message: '{}', Rejected Value: [{}]",
                    fieldName, errorMessage, err.getRejectedValue());

            errorList.put(fieldName, errorMessage);
        });

        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("VALIDATION_FAILED")
                .data(errorList)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    // Not Found
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .status(HttpStatus.NOT_FOUND.value())
                        .message(ex.getMessage())
                        .build());
    }

    // Exception
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {

        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("Unexpected error occurred")
                .build();

        return ResponseEntity.internalServerError().body(response);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<String> handleMultipartException(MultipartException e) {
        // This catches FileCountLimitExceededException and MaxUploadSizeExceededException
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("File upload error: Too many files or size limit exceeded.");
    }
}
