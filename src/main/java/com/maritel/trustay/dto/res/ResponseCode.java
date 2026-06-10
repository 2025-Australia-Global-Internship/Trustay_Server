package com.maritel.trustay.dto.res;

import lombok.Getter;

@Getter
public enum ResponseCode {
    /* Success  */
    SUCCESS(200, "Your request was processed successfully."),

    /* 404 Not Found */
    NOT_FOUND(404, "Account does not exist."),

    /* Already Exist */
    ALREADY_EXIST_USER_EMAIL(1001, "This email is already in use."),
    ALREADY_EXIST_USER_MOBILE(1001, "This phone number is already in use."),
    ALREADY_EXIST_USER_PUSH_TOKEN(1001, "This push token is already registered."),
    ALREADY_EXIST_USER_SNS_CONNECTIED(1001, "This account is already linked."),

    /* Not Matched */
    NOT_MATCHED(1002, "The information you entered does not match."),
    NOT_MATCHED_PASSWORD(1002, "The password you entered is incorrect."),

    /* Not Valid */
    NOT_VALID(4000, "We can't process this data."),

    /* Bad Request & Not Entered */
    NOT_ENTERED_REQUEST_BODY(4001, "A required field is missing."),
    NOT_ENTERED_REQUEST_BODY_FIELD(4002, "Please fill in all required fields."),
    NOT_ENTERED_REQUEST_PARAM(4002, "A required parameter is missing."),
    NOT_ENTERED_FILE(4002, "Please attach a file."),

    /* Forbidden */
    FORBIDDEN(4030, "You don't have permission to access this."),
    FORBIDDEN_NOT_VALID(4031, "This account is not valid."),
    FORBIDDEN_NOT_VALID_PASSWORD(4031, "The password format is not valid."),

    /* Not Found */
    NOT_FOUND_USER(4041, "User not found."),
    NOT_FOUND_GALLERY(4041, "This post does not exist."),
    NOT_FOUND_COUNTRY_CODE(4041, "Country code not found."),
    NOT_FOUND_CITY(4041, "City not found."),
    NOT_FOUND_REASON_OF_JOIN(4041, "Reason for joining not found."),
    NOT_FOUND_TERMS(4041, "Terms document not found."),
    NOT_FOUND_SNS_STATUS(4041, "This SNS link status can't be processed."),
    NOT_FOUND_BLOCK(4041, "Block information not found."),
    NOT_FOUND_GOLF_CLUB(4041, "Golf club not found."),
    NOT_FOUND_GOLF_CLUB_CATEGORY(4041, "Golf club category not found."),
    NOT_FOUND_GOLF_CLUB_TAG(4041, "Golf club tag not found."),
    NOT_FOUND_BOOKMARK(4041, "Bookmarked golf club not found."),
    NOT_FOUND_HOTEL(4041, "Hotel not found."),
    NOT_FOUND_HOTEL_ROOM(4041, "Hotel room not found."),
    NOT_FOUND_PICK_UP(4041, "Pickup service not found."),
    NOT_FOUND_LOUNGE(4041, "Lounge not found."),
    NOT_FOUND_LOUNGE_LIKE(4041, "Lounge like not found."),
    NOT_FOUND_LOUNGE_COMMENT(4041, "Lounge comment not found."),
    NOT_FOUND_LOUNGE_COMMENT_LIKE(4041, "Lounge comment like not found."),
    NOT_FOUND_RESERVATION(4041, "Reservation not found."),
    NOT_FOUND_REVIEW(4041, "Review not found."),
    NOT_FOUND_PAYMENT(4042, "Payment not found."),
    NOT_FOUND_CONTRACT(4043, "Contract not found."),

    NOT_CONTRACT_TENANT(4032, "Only the tenant on this contract can make this payment."),

    /* Review */
    REVIEW_NOT_ELIGIBLE(4033, "Only users with a residency history (contract) for this listing can leave a review."),
    REVIEW_ALREADY_EXISTS(4034, "You've already written a review for this listing."),
    REVIEW_FORBIDDEN(4035, "You can only edit or delete reviews you wrote yourself."),

    /* Notification */
    NOT_FOUND_NOTIFICATION(4045, "Notification not found."),
    NOTIFICATION_FORBIDDEN(4036, "You can only manage your own notifications."),

    /* Comment / Post Like */
    NOT_FOUND_COMMENT(4046, "Comment not found."),
    NOT_FOUND_POST(4047, "Post not found."),

    /* Auto Transfer */
    NOT_FOUND_AUTO_TRANSFER(4048, "Auto-transfer schedule not found."),
    AUTO_TRANSFER_FORBIDDEN(4037, "You can only manage your own auto-transfer schedules."),

    TOSS_PAYMENT_FAILED(5001, "Toss payment approval failed."),
    ;

    private final int code;
    private final String message;

    ResponseCode(final int code, final String message) {
        this.code = code;
        this.message = message;
    }
}
