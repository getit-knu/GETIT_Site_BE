package com.getit.domain.setting.staff.dto;

import com.getit.domain.setting.staff.entity.StaffSection;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 운영진 순서 변경 요청. (API 명세서 10.22) section 안에서만 order 를 1부터 재부여한다. */
public record StaffOrderRequest(
    @NotNull StaffSection section,
    @NotEmpty List<Long> orderedIds
) { }
