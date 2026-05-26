package com.maritel.trustay.util;

import lombok.Getter;

@Getter
public enum WeekType {
    UNKNOWN("UNKNOWN", 0),
    SUN("SUN", 1),
    MON("MON", 2),
    TUE("THU", 3),
    WED("WED", 4),
    THU("THU", 5),
    FRI("FRI", 6),
    SAT("SAT", 7),
    HOLIDAY("HOLIDAY", 8);


    private String week;
    private int no;

    WeekType(String week, int no) {
        this.week = week;
        this.no = no;
    }
}