package com.maritel.trustay.util;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {

    public static final String FORMAT_DATE_TIME = "yyyyMMddHHmmss";

    public static final String FORMAT_DATE_TIME_SSS = "yyyyMMddHHmmssSSS";

    public static final String FORMAT_DATE = "yyyyMMdd";
    public static final String FORMAT_TIME = "HHmm";

    public static final String FORMAT_DATE_BAR = "yyyy-MM-dd";
    public static final String FORMAT_DATE_TIME_UNIT = "yyyy-MM-dd HH:mm:ss";
    public static final String FORMAT_DATE_UNIT = "yyyy/MM/dd";
    public static final String FORMAT_MONTH_DATE_UNIT = "MM/dd";
    public static final String FORMAT_TIME_UNIT = "HH:mm:ss";

    public static final String FORMAT_DATE_MIN_UNIT_BAR = "yyyy-MM-dd HH:mm";
    public static final String FORMAT_DATE_TIME_UNIT_BAR = "yyyy-MM-dd HH:mm:ss";
    public static final String FORMAT_DATE_UNIT_BAR = "yyyy년 MM월 dd일";

    public static final String FORMAT_HOUR_MINUTE_UNIT_BAR = "HH:mm";
    public static final String FORMAT_MONTH_DAY_UNIT_BAR = "MM-dd";

    public static String getDateString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMAT_DATE);
        LocalDateTime localDateTime = LocalDateTime.now();
        return formatter.format(localDateTime);
    }

    public static WeekType getWeekDay() {
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        switch (dayOfWeek) {
            case 1:
                return WeekType.SUN;
            case 2:
                return WeekType.MON;
            case 3:
                return WeekType.TUE;
            case 4:
                return WeekType.WED;
            case 5:
                return WeekType.THU;
            case 6:
                return WeekType.FRI;
            case 7:
                return WeekType.SAT;
        }
        return WeekType.UNKNOWN;
    }

    public static String getFormatStrToDate(Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(FORMAT_DATE_UNIT_BAR);
        return simpleDateFormat.format(date);
    }

    public static String getTimeString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMAT_TIME);
        LocalDateTime localDateTime = LocalDateTime.now();
        return formatter.format(localDateTime);
    }

    public static String getTimePlusString(int minutesToAdd) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMAT_TIME);
        LocalDateTime localDateTime = LocalDateTime.now();
        LocalDateTime newTime = localDateTime.plusMinutes(minutesToAdd);
        return formatter.format(newTime);
    }

    public static String getDateFull() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMAT_DATE_TIME_SSS);
        LocalDateTime localDateTime = LocalDateTime.now();
        return formatter.format(localDateTime);
    }

    public static Date toDate(LocalDateTime localDateTime) {
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        return Date.from(instant);
    }

    public static String getDateTimeString(LocalDateTime date) {
        SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_DATE_TIME_UNIT);
        return sdf.format(toDate(date));
    }

    public static String getLocalDateTimeString(LocalDateTime date, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(toDate(date));
    }

    /**
     *
     * @param localDate
     * @return 2023-12-08
     */
    public static String getToLocalDate(LocalDate localDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMAT_DATE_UNIT_BAR);
        return formatter.format(localDate);
    }

    public static Date toDate(String from) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_DATE_TIME);
        return sdf.parse(from);
    }

    // 날짜(+시간) 형식의 문자열을 java.time.LocalDateTime 타입으로 변환한다.
    public static LocalDateTime toLocalDateTime(String from) throws DateTimeParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMAT_DATE_TIME_UNIT);
        return LocalDateTime.parse(from, formatter); // yyyy/MM/dd HH:mm:ss
    }

    public static LocalDateTime toLocalDateTime(String from, String format) throws DateTimeParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return LocalDateTime.parse(from, formatter); //
    }

    // 날짜(+시간) 형식의 문자열을 java.time.LocalDate 타입으로 변환한다.
    public static LocalDate toLocalDate(String from) throws DateTimeParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMAT_DATE_UNIT_BAR);
        return LocalDate.parse(from, formatter); // yyyy/MM/dd
    }

    // 날짜(+시간) 형식의 문자열을 java.time.LocalTime 타입으로 변환한다.
    public static LocalTime toLocalTime(String from) throws DateTimeParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(FORMAT_TIME_UNIT);
        return LocalTime.parse(from, formatter); // HH:mm:ss
    }

    public static LocalDate toLocalDate(String from, String format) throws DateTimeParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return LocalDate.parse(from, formatter); // yyyy/MM/dd
    }

    public static LocalDate getLocalDate() {
        return LocalDate.now();
    }

    public static LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

}

