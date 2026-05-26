package com.maritel.trustay.dto.res;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class PageResponse<T> {

    private List<T> content;       // 실제 데이터 리스트
    private int page;              // 현재 페이지 (0부터 시작)
    private int size;              // 한 페이지당 개수
    private long totalElements;    // 전체 데이터 개수
    private int totalPages;        // 전체 페이지 수

    // Page 객체를 받아서 필요한 것만 쏙쏙 뽑아내는 생성자
    public PageResponse(Page<T> pageData) {
        this.content = pageData.getContent();
        this.page = pageData.getNumber();
        this.size = pageData.getSize();
        this.totalElements = pageData.getTotalElements();
        this.totalPages = pageData.getTotalPages();
    }
}