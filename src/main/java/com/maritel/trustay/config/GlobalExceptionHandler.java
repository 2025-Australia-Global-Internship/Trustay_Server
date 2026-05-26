package com.maritel.trustay.config;

import com.maritel.trustay.constant.CommonConstants;
import com.maritel.trustay.dto.res.DataResponse;
import com.maritel.trustay.dto.res.ResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

