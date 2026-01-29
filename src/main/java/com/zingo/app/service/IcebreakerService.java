package com.zingo.app.service;

import com.zingo.app.dto.ChatDtos.IcebreakerResponse;
import com.zingo.app.entity.Event;
import com.zingo.app.entity.Profile;
import com.zingo.app.entity.Showtime;
import com.zingo.app.exception.NotFoundException;
import com.zingo.app.repository.EventRepository;
import com.zingo.app.repository.ProfileRepository;
import com.zingo.app.repository.ShowtimeRepository;
import com.zingo.app.security.SecurityUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class IcebreakerService {
  private final ProfileRepository profileRepository;
  private final ShowtimeRepository showtimeRepository;
  private final EventRepository eventRepository;

  public IcebreakerService(ProfileRepository profileRepository, ShowtimeRepository showtimeRepository,
      EventRepository eventRepository) {
    this.profileRepository = profileRepository;
    this.showtimeRepository = showtimeRepository;
    this.eventRepository = eventRepository;
  }

  public IcebreakerResponse suggestionsForShowtime(Long showtimeId) {
    Long userId = SecurityUtil.currentUserId();
    Profile profile = profileRepository.findById(userId).orElseThrow(() -> new NotFoundException("Profile not found"));
    Showtime showtime = showtimeRepository.findById(showtimeId)
        .orElseThrow(() -> new NotFoundException("Showtime not found"));
    Event event = eventRepository.findById(showtime.getEventId())
        .orElseThrow(() -> new NotFoundException("Event not found"));

    List<String> tags = profile.getPersonalityTags() == null ? List.of() : profile.getPersonalityTags();
    return new IcebreakerResponse(buildSuggestions(tags, event.getTitle()));
  }

  private List<String> buildSuggestions(List<String> tags, String title) {
    List<String> templates = List.of(
        "Want to grab seats together for {title}?",
        "I picked {title} because it looked fun—what stood out to you?",
        "Any snacks you always get at the movies?",
        "First time watching {title} or are you a fan?",
        "I’m {tag}—are you the same or different?",
        "If we could ask the director one question about {title}, what would it be?",
        "What’s the best movie you’ve seen this year?",
        "Should we do a quick hello in the lobby before {title}?",
        "Do you prefer quiet rows or lively rows?",
        "What made you join this showtime of {title}?"
    );

    List<String> suggestions = new ArrayList<>();
    int base = Objects.hash(tags, title);
    for (int i = 0; i < templates.size(); i++) {
      int idx = Math.floorMod(base + i * 31, templates.size());
      String template = templates.get(idx);
      String tag = tags.isEmpty() ? "curious" : tags.get(Math.floorMod(base + i, tags.size()));
      String suggestion = template.replace("{title}", title).replace("{tag}", tag);
      if (!suggestions.contains(suggestion)) {
        suggestions.add(suggestion);
      }
      if (suggestions.size() >= 8) {
        break;
      }
    }
    return suggestions;
  }
}
