package com.zingo.app.controller;

import com.zingo.app.dto.TrekDtos.CreateTrekGroupRequest;
import com.zingo.app.dto.TrekDtos.CreateTrekJoinRequest;
import com.zingo.app.dto.TrekDtos.TrekDecisionResponse;
import com.zingo.app.dto.TrekDtos.TrekGroupDto;
import com.zingo.app.dto.TrekDtos.TrekHostStatusDto;
import com.zingo.app.dto.TrekDtos.TrekJoinRequestDto;
import com.zingo.app.service.TrekService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/treks")
public class TrekController {
  private final TrekService trekService;

  public TrekController(TrekService trekService) {
    this.trekService = trekService;
  }

  @GetMapping("/host/me")
  public TrekHostStatusDto hostStatus() {
    return trekService.hostStatus();
  }

  @PostMapping("/host/onboard")
  public TrekHostStatusDto onboardHost() {
    return trekService.onboardCurrentUserAsHost();
  }

  @PostMapping("/groups")
  public TrekGroupDto createGroup(@Valid @RequestBody CreateTrekGroupRequest request) {
    return trekService.createGroup(request);
  }

  @GetMapping("/groups")
  public List<TrekGroupDto> listGroups(@RequestParam Long showtimeId) {
    return trekService.listGroups(showtimeId);
  }

  @PostMapping("/groups/{groupId}/requests")
  public TrekJoinRequestDto requestJoin(@PathVariable Long groupId, @RequestBody(required = false) CreateTrekJoinRequest request) {
    return trekService.requestJoin(groupId, request == null ? new CreateTrekJoinRequest(null) : request);
  }

  @GetMapping("/requests/pending")
  public List<TrekJoinRequestDto> pendingRequests() {
    return trekService.listPendingForCurrentHost();
  }

  @PostMapping("/requests/{requestId}/approve")
  public TrekDecisionResponse approve(@PathVariable Long requestId) {
    return trekService.approveRequest(requestId);
  }

  @PostMapping("/requests/{requestId}/decline")
  public TrekJoinRequestDto decline(@PathVariable Long requestId) {
    return trekService.declineRequest(requestId);
  }
}
