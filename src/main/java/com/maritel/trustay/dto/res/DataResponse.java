package com.maritel.trustay.dto.res;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Slf4j
@Setter
@Getter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataResponse<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = -8194352074034052087L;

    @Builder.Default
    private String dateTime = LocalDateTime.now().toString();

    @Builder.Default
    private String version = "1.0.0";

    @Builder.Default
    private int code = ResponseCode.SUCCESS.getCode();

    @Builder.Default
    private String message = ResponseCode.SUCCESS.getMessage();

    private T data;

    public static DataResponse of() {
        DataResponse response = new DataResponse();
        return response;
    }

    @SuppressWarnings("unchecked")
    public static <T> DataResponse<T> of(T data) {
        DataResponse response = new DataResponse();
        response.setData(data);
        return response;
    }

    public static DataResponse of(ResponseCode responseCode) {
        DataResponse response = new DataResponse();
        response.setCode(responseCode.getCode());
        response.setMessage(responseCode.getMessage());
        return response;
    }

    public static DataResponse of(int code, String message) {
        DataResponse response = new DataResponse();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }

    @SuppressWarnings("unchecked")
    public static <T> DataResponse<T> of(ResponseCode responseCode, T data) {
        DataResponse response = new DataResponse();
        response.setCode(responseCode.getCode());
        response.setMessage(responseCode.getMessage());
        response.setData(data);
        return response;
    }

    @SuppressWarnings("unchecked")
    public static <T> DataResponse<T> of(int code, String message, T data) {
        DataResponse response = new DataResponse();
        response.setCode(code);
        response.setMessage(message);
        response.setData(data);
        return response;
    }

}
