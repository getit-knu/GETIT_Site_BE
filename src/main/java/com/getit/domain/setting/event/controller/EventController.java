package com.getit.domain.setting.event.controller;

import com.getit.domain.setting.event.dto.EventRequest;
import com.getit.domain.setting.event.dto.EventResult;
import com.getit.domain.setting.event.service.EventAdminService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Setting", description = "사이트 설정")
@RestController
@RequestMapping("/api/admin/setting/events")
@RequiredArgsConstructor
public class EventController {

  private final EventAdminService eventAdminService;

  @Operation(summary = "행사 목록", description = "명세서 10.14")
  @GetMapping
  public ApiResponse<List<EventResult>> getEvents() {
    return ApiResponse.success(eventAdminService.getEvents());
  }

  @Operation(summary = "행사 추가", description = "명세서 10.15")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<EventResult> createEvent(@Valid @RequestBody EventRequest request) {
    return ApiResponse.success(eventAdminService.createEvent(request));
  }

  @Operation(summary = "행사 수정", description = "명세서 10.16")
  @PutMapping("/{id}")
  public ApiResponse<EventResult> updateEvent(
      @PathVariable Long id,
      @Valid @RequestBody EventRequest request
  ) {
    return ApiResponse.success(eventAdminService.updateEvent(id, request));
  }

  @Operation(summary = "행사 삭제", description = "명세서 10.17")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteEvent(@PathVariable Long id) {
    eventAdminService.deleteEvent(id);
  }
}
