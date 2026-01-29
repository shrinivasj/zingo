package com.zingo.app.controller;

import com.zingo.app.dto.ChatDtos.ConversationDto;
import com.zingo.app.dto.ChatDtos.IcebreakerResponse;
import com.zingo.app.dto.ChatDtos.MessageDto;
import com.zingo.app.dto.ChatDtos.SendMessageRequest;
import com.zingo.app.service.ConversationService;
import com.zingo.app.service.IcebreakerService;
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
@RequestMapping("/api")
public class ConversationController {
  private final ConversationService conversationService;
  private final IcebreakerService icebreakerService;

  public ConversationController(ConversationService conversationService, IcebreakerService icebreakerService) {
    this.conversationService = conversationService;
    this.icebreakerService = icebreakerService;
  }

  @GetMapping("/conversations")
  public List<ConversationDto> listConversations() {
    return conversationService.listForCurrentUser();
  }

  @GetMapping("/conversations/{id}/messages")
  public List<MessageDto> listMessages(@PathVariable Long id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return conversationService.listMessages(id, page, size);
  }

  @PostMapping("/conversations/{id}/messages")
  public MessageDto sendMessage(@PathVariable Long id, @Valid @RequestBody SendMessageRequest request) {
    return conversationService.sendMessage(id, request.text());
  }

  @PostMapping("/conversations/{id}/leave")
  public void leave(@PathVariable Long id) {
    conversationService.leaveConversation(id);
  }

  @GetMapping("/icebreakers")
  public IcebreakerResponse icebreakers(@RequestParam Long showtimeId) {
    return icebreakerService.suggestionsForShowtime(showtimeId);
  }
}
