package com.maritel.trustay.constant;

import java.util.HashSet;
import java.util.Set;

public class CommonConstants {
    public static final String VALID_FIELD_NAME     = "name";
    public static final String VALID_FIELD_EMAIL    = "email";
    public static final String VALID_FIELD_PASSWD   = "passwd";

    private static final Set<String> VALID_FIELDS = new HashSet<>();
    static {
        VALID_FIELDS.add(CommonConstants.VALID_FIELD_NAME);
        VALID_FIELDS.add(CommonConstants.VALID_FIELD_EMAIL);
        VALID_FIELDS.add(CommonConstants.VALID_FIELD_PASSWD);
    }

    // 필드가 유효한지 확인하는 메서드
    public static boolean isValidField(String field) {
        return VALID_FIELDS.contains(field);
    }
}