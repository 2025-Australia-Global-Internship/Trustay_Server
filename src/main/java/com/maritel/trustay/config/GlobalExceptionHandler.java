package com.maritel.trustay.config;

import com.maritel.trustay.constant.CommonConstants;
import com.maritel.trustay.dto.res.DataResponse;
import com.maritel.trustay.dto.res.ResponseCode;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    //private final HttpClientProperties http;

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<DataResponse<?>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        //전체 에러 항목을 모두 담아 보여줌
//        ex.getBindingResult().getFieldErrors().forEach(error ->
//                errors.put(error.getField(), error.getDefaultMessage())
//        );

        //특정 우선순위에 맞게 하나만 담아 보여줌
        ex.getBindingResult().getFieldErrors().stream()
                .filter(error -> isValidField(error.getField()))
                .findFirst()  // 첫 번째 조건이 맞는 에러가 발견되면 반환
                .ifPresent(error -> {
                    // 원하는 처리를 여기서 할 수 있음
                    errors.put("error", error.getDefaultMessage());
                });

        return ResponseEntity.ok(DataResponse.of(ResponseCode.NOT_VALID, errors));
    }

    /**
     * 컨트롤러/서비스에서 직접 던지는 {@link BadRequestException}.
     * (예: 채팅 이미지 업로드 시 첨부 누락, 지원하지 않는 확장자, 채팅방 참여자 아님 등)
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<DataResponse<?>> handleBadRequest(BadRequestException ex) {
        log.warn("BadRequest: {}", ex.getMessage());
        Map<String, String> body = Map.of("error", safeMessage(ex));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(DataResponse.of(ResponseCode.NOT_VALID, body));
    }

    /**
     * 도메인 검증 실패용 IllegalArgumentException (예: "You are not a participant ...").
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<DataResponse<?>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("IllegalArgument: {}", ex.getMessage());
        Map<String, String> body = Map.of("error", safeMessage(ex));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(DataResponse.of(ResponseCode.NOT_VALID, body));
    }

    /**
     * JPA `findById().orElseThrow(EntityNotFoundException::new)` 케이스 (회원/방 등 없음).
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<DataResponse<?>> handleEntityNotFound(EntityNotFoundException ex) {
        log.warn("EntityNotFound: {}", ex.getMessage());
        Map<String, String> body = Map.of("error", safeMessage(ex));
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(DataResponse.of(ResponseCode.NOT_FOUND, body));
    }

    /**
     * application-*.yaml 의 `spring.servlet.multipart.max-file-size` 초과 시 발생.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<DataResponse<?>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("Max upload size exceeded: {}", ex.getMessage());
        Map<String, String> body = Map.of("error", "Uploaded file is too large.");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(DataResponse.of(ResponseCode.NOT_VALID, body));
    }

    /**
     * 마지막 안전망: 위에서 잡지 못한 모든 예외를 500 으로 응답한다.
     * 이렇게 해야 클라이언트가 `/error` 로 forward 되어 403 으로 가려지는 일이 없다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<DataResponse<?>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        Map<String, String> body = Map.of(
                "error", "Internal server error.",
                "detail", safeMessage(ex)
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(DataResponse.of(ResponseCode.NOT_VALID, body));
    }

    private static String safeMessage(Throwable ex) {
        String msg = ex.getMessage();
        return (msg == null || msg.isBlank()) ? ex.getClass().getSimpleName() : msg;
    }

    private boolean isValidField(String field) {
        if (CommonConstants.VALID_FIELD_NAME.equals(field) ||
                CommonConstants.VALID_FIELD_EMAIL.equals(field) ||
                CommonConstants.VALID_FIELD_PASSWD.equals(field)) {
            return true;
        } else {
            return false;
        }
    }

}

