package com.getit.global.dto;

/** 전체 번호 단건 조회 응답. 번호가 없으면 null. (이슈 #237) */
public record PhoneNumberResult(String phoneNumber) {
}
