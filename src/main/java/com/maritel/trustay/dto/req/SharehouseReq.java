package com.maritel.trustay.dto.req;

import com.maritel.trustay.constant.HouseType;
import com.maritel.trustay.constant.RoomType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class SharehouseReq {

    @NotBlank(message = "Title is required.")
    private String title;

    @NotBlank(message = "Description is required.")
    private String description;

    @NotBlank(message = "Address is required.")
    private String address;

    @NotNull(message = "Please select a house type.")
    private HouseType houseType;

    @NotNull(message = "Rent price is required.")
    private Integer rentPrice;

    private Integer roomCount;
    private Integer bathroomCount;
    private Integer currentResidents;

    private List<String> homeRules;

    private List<String> features;

    // [변경] 파일 객체 대신, 업로드된 이미지 URL 리스트를 받습니다.
    @NotEmpty(message = "Please upload at least one image.")
    private List<String> imageUrls;

    @NotNull(message = "Please select whether bills are included.")
    private Boolean billsIncluded;

    private RoomType roomType;

    @NotNull(message = "Please select a bond type.")
    private Integer bondType;

    @NotNull(message = "Please enter the minimum stay.")
    private Integer minimumStay;

    @NotBlank(message = "Please enter gender preference.")
    private String gender;

    @NotBlank(message = "Please enter age preference.")
    private String age;

    /** 선택 항목 */
    private String religion;

    /** 선택 항목 */
    private String dietaryPreference;
}