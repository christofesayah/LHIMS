package com.lhims.api.service;

import com.lhims.api.domain.entity.ComputedScore;
import com.lhims.api.domain.entity.Notification;
import com.lhims.api.domain.entity.Region;
import com.lhims.api.domain.entity.UserAccount;
import com.lhims.api.domain.enums.AuditActionType;
import com.lhims.api.domain.enums.RiskCategory;
import com.lhims.api.exception.NotFoundException;
import com.lhims.api.integration.AnalyticsClient;
import com.lhims.api.repository.ComputedScoreRepository;
import com.lhims.api.repository.NotificationRepository;
import com.lhims.api.repository.RegionRepository;
import com.lhims.api.repository.UserRepository;
import com.lhims.api.web.dto.ScoreDtos;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScoreService {

    private final ComputedScoreRepository computedScoreRepository;
    private final RegionRepository regionRepository;
    private final AnalyticsClient analyticsClient;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ScoreService(ComputedScoreRepository computedScoreRepository, RegionRepository regionRepository,
                         AnalyticsClient analyticsClient, NotificationRepository notificationRepository,
                         UserRepository userRepository, AuditService auditService) {
        this.computedScoreRepository = computedScoreRepository;
        this.regionRepository = regionRepository;
        this.analyticsClient = analyticsClient;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public ScoreDtos.ScoreResponse getScores(Long regionId) {
        ComputedScore score = computedScoreRepository.findTopByRegionRegionIdAndScenarioIsNullOrderByLastComputedAtDesc(regionId)
                .orElseThrow(() -> new NotFoundException("Score not found"));
        return toDto(score);
    }

    @Transactional(readOnly = true)
    public List<ScoreDtos.ScoreResponse> getAllScores() {
        return regionRepository.findAll().stream()
                .map(region -> computedScoreRepository.findTopByRegionRegionIdAndScenarioIsNullOrderByLastComputedAtDesc(region.getRegionId())
                        .map(this::toDto)
                        .orElse(new ScoreDtos.ScoreResponse(region.getRegionId(), null, null, null, null, null)))
                .toList();
    }

    @Transactional
    public ScoreDtos.ScoreResponse computeScores(Long regionId, Long actorUserId, HttpServletRequest httpServletRequest) {
        Region region = regionRepository.findById(regionId).orElseThrow(() -> new NotFoundException("Region not found"));
        RiskCategory before = computedScoreRepository.findTopByRegionRegionIdAndScenarioIsNullOrderByLastComputedAtDesc(regionId)
                .map(ComputedScore::getRiskCategory)
                .orElse(null);

        analyticsClient.computeRegion(regionId);

        ComputedScore after = computedScoreRepository.findTopByRegionRegionIdAndScenarioIsNullOrderByLastComputedAtDesc(regionId)
                .orElseGet(() -> {
                    ComputedScore fallback = new ComputedScore();
                    fallback.setRegion(region);
                    fallback.setReportingPeriod("latest");
                    fallback.setHaiScore(0.0);
                    fallback.setRviScore(0.0);
                    fallback.setCiriScore(0.0);
                    fallback.setRiskCategory(RiskCategory.LOW);
                    fallback.setLastComputedAt(LocalDateTime.now());
                    return computedScoreRepository.save(fallback);
                });

        if (before == RiskCategory.MEDIUM && after.getRiskCategory() == RiskCategory.HIGH) {
            createRiskChangeNotifications(region, before, after.getRiskCategory());
        }
        auditService.log(
                actorUserId,
                null,
                regionId,
                AuditActionType.COMPUTE,
                "regions",
                String.valueOf(regionId),
                before == null ? null : before.name(),
                after.getRiskCategory() == null ? null : after.getRiskCategory().name(),
                httpServletRequest
        );
        return toDto(after);
    }

    @Transactional(readOnly = true)
    public ScoreDtos.ScoreHistoryResponse history(Long regionId) {
        List<ScoreDtos.ScoreResponse> history = computedScoreRepository
                .findByRegionRegionIdAndScenarioIsNullOrderByLastComputedAtDesc(regionId)
                .stream()
                .map(this::toDto)
                .toList();
        return new ScoreDtos.ScoreHistoryResponse(regionId, history);
    }

    @Transactional(readOnly = true)
    public List<ScoreDtos.ScoreResponse> highRiskDistricts() {
        return computedScoreRepository.findByRiskCategoryAndScenarioIsNull(RiskCategory.HIGH).stream()
                .map(this::toDto)
                .toList();
    }

    private void createRiskChangeNotifications(Region region, RiskCategory before, RiskCategory after) {
        List<UserAccount> users = userRepository.findByIsActiveTrue();
        for (UserAccount user : users) {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setMessage("District " + region.getName() + " changed risk from " + before + " to " + after);
            notification.setIsRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    private ScoreDtos.ScoreResponse toDto(ComputedScore score) {
        return new ScoreDtos.ScoreResponse(
                score.getRegion().getRegionId(),
                score.getHaiScore(),
                score.getRviScore(),
                score.getCiriScore(),
                score.getRiskCategory(),
                score.getLastComputedAt()
        );
    }
}
