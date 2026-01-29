package com.zingo.app.service;

import com.zingo.app.dto.SafetyDtos.BlockRequest;
import com.zingo.app.dto.SafetyDtos.ReportRequest;
import com.zingo.app.entity.Block;
import com.zingo.app.entity.Profile;
import com.zingo.app.entity.Report;
import com.zingo.app.exception.BadRequestException;
import com.zingo.app.repository.BlockRepository;
import com.zingo.app.repository.ProfileRepository;
import com.zingo.app.repository.ReportRepository;
import com.zingo.app.security.SecurityUtil;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SafetyService {
  private final BlockRepository blockRepository;
  private final ReportRepository reportRepository;
  private final ProfileRepository profileRepository;

  public SafetyService(BlockRepository blockRepository, ReportRepository reportRepository, ProfileRepository profileRepository) {
    this.blockRepository = blockRepository;
    this.reportRepository = reportRepository;
    this.profileRepository = profileRepository;
  }

  @Transactional
  public void block(BlockRequest request) {
    Long userId = SecurityUtil.currentUserId();
    if (userId.equals(request.blockedId())) {
      throw new BadRequestException("Cannot block yourself");
    }
    if (blockRepository.existsByBlockerIdAndBlockedId(userId, request.blockedId())) {
      return;
    }
    Block block = new Block();
    block.setBlockerId(userId);
    block.setBlockedId(request.blockedId());
    blockRepository.save(block);
  }

  @Transactional
  public void report(ReportRequest request) {
    Long userId = SecurityUtil.currentUserId();
    if (userId.equals(request.reportedId())) {
      throw new BadRequestException("Cannot report yourself");
    }
    Report report = new Report();
    report.setReporterId(userId);
    report.setReportedId(request.reportedId());
    report.setReason(request.reason());
    report.setDetails(request.details());
    reportRepository.save(report);
  }

  public boolean isBlockedBetween(Long userA, Long userB) {
    return blockRepository.existsByBlockerIdAndBlockedId(userA, userB)
        || blockRepository.existsByBlockerIdAndBlockedId(userB, userA);
  }

  public Set<Long> blockedIdsForUser(Long userId) {
    Set<Long> blocked = new HashSet<>();
    List<Block> blocking = blockRepository.findByBlockerId(userId);
    for (Block block : blocking) {
      blocked.add(block.getBlockedId());
    }
    List<Block> blockedBy = blockRepository.findByBlockedId(userId);
    for (Block block : blockedBy) {
      blocked.add(block.getBlockerId());
    }
    blocked.remove(userId);
    return blocked;
  }

  public List<Profile> listBlockedProfiles(Long userId) {
    Set<Long> blocked = blockedIdsForUser(userId);
    return profileRepository.findAllById(blocked);
  }
}
