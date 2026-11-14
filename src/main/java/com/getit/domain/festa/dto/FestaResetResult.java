package com.getit.domain.festa.dto;

/** 초기화로 지운 투자자 수. 투자 내역과 전화번호가 함께 지워진다. (이슈 #229) */
public record FestaResetResult(long deletedInvestorCount) { }
