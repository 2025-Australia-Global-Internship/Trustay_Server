package com.maritel.trustay.constant;

import lombok.Getter;
import org.springframework.context.annotation.Bean;

import java.util.regex.Pattern;

@Getter
public class PatternConstants {
    // 어노테이션에서 쓸 수 있도록 public static final 상수로 선언
    public static final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
    public static final String NAME_REGEX = "^[a-zA-Z0-9가-힣]{2,25}$";
    public static final String PASSWORD_REGEX = "^[a-zA-Z0-9!@#$%^&*?_]{8,50}$";
    public static final String BIRTH_REGEX = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$";
    public static final String PHONE_REGEX = "^(0\\d{1,2})-(\\d{3,4})-(\\d{4})$";
    public static final String ACCOUNT_REGEX = "^[0-9]{2,6}(-[0-9]{2,7}){1,3}$\n";

    public static final String EMAIL_MESSAGE = "Please enter a valid email address.";
    public static final String NAME_MESSAGE = "Name can only contain letters.";
    public static final String PASSWORD_MESSAGE = "Password must be 8-50 characters and contain only letters, numbers, or special characters (!@#$%^&*?_).";
    public static final String BIRTH_MESSAGE = "Date of birth must be in the format yyyy-mm-dd.";
    public static final String PHONE_MESSAGE = "Phone number must be in the format 000-0000-0000.";
    public static final String ACCOUNT_MESSAGE = "Please enter a valid account number.";

}
