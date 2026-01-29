package com.zingo.app.controller;

import com.zingo.app.dto.LobbyDtos.LobbyJoinRequest;
import com.zingo.app.dto.LobbyDtos.LobbyPresenceUpdate;
import com.zingo.app.dto.LobbyDtos.LobbyUsersResponse;
import com.zingo.app.service.LobbyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lobbies")
public class LobbyController {
  private final LobbyService lobbyService;

  public LobbyController(LobbyService lobbyService) {
    this.lobbyService = lobbyService;
  }

  @PostMapping("/join")
  public LobbyPresenceUpdate join(@Valid @RequestBody LobbyJoinRequest request) {
    return lobbyService.join(request.showtimeId());
  }

  @PostMapping("/heartbeat")
  public LobbyPresenceUpdate heartbeat(@Valid @RequestBody LobbyJoinRequest request) {
    return lobbyService.heartbeat(request.showtimeId());
  }

  @GetMapping("/{showtimeId}/users")
  public LobbyUsersResponse users(@PathVariable Long showtimeId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "24") int size) {
    return lobbyService.listUsers(showtimeId, page, size);
  }
}
