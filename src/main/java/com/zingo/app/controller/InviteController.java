package com.zingo.app.controller;

import com.zingo.app.dto.InviteDtos.InviteAcceptResponse;
import com.zingo.app.dto.InviteDtos.InviteDto;
import com.zingo.app.dto.InviteDtos.InviteRequest;
import com.zingo.app.service.InviteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invites")
public class InviteController {
  private final InviteService inviteService;

  public InviteController(InviteService inviteService) {
    this.inviteService = inviteService;
  }

  @PostMapping
  public InviteDto create(@Valid @RequestBody InviteRequest request) {
    return inviteService.createInvite(request);
  }

  @PostMapping("/{id}/accept")
  public InviteAcceptResponse accept(@PathVariable Long id) {
    return inviteService.acceptInvite(id);
  }

  @PostMapping("/{id}/decline")
  public InviteDto decline(@PathVariable Long id) {
    return inviteService.declineInvite(id);
  }
}
