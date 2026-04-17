package com.lhims.api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lhims.api.domain.entity.ComputedScore;
import com.lhims.api.domain.entity.Region;
import com.lhims.api.domain.entity.UserAccount;
import com.lhims.api.domain.enums.AuditActionType;
import com.lhims.api.domain.enums.RiskCategory;
import com.lhims.api.integration.AnalyticsClient;
import com.lhims.api.repository.ComputedScoreRepository;
import com.lhims.api.repository.NotificationRepository;
import com.lhims.api.repository.RegionRepository;
import com.lhims.api.repository.UserRepository;
import com.lhims.api.web.dto.ScoreDtos;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class ScoreServiceTest {

    @Mock
    private ComputedScoreRepository computedScoreRepository;
    @Mock
    private RegionRepository regionRepository;
    @Mock
    private AnalyticsClient analyticsClient;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private HttpServletRequest httpServletRequest;

    private ScoreService scoreService;

    @BeforeEach
    void setUp() {
        scoreService = new ScoreService(
                computedScoreRepository,
                regionRepository,
                analyticsClient,
                notificationRepository,
                userRepository,
                auditService
        );
    }

    @Test
    void computeScoresCreatesNotificationsOnMediumToHighTransition() {
        Region region = org.mockito.Mockito.mock(Region.class);
        when(region.getRegionId()).thenReturn(2L);
        when(region.getName()).thenReturn("Akkar");

        ComputedScore before = new ComputedScore();
        before.setRiskCategory(RiskCategory.MEDIUM);

        ComputedScore after = new ComputedScore();
        after.setRegion(region);
        after.setRiskCategory(RiskCategory.HIGH);
        after.setHaiScore(0.48);
        after.setRviScore(0.61);
        after.setCiriScore(0.55);
        after.setLastComputedAt(LocalDateTime.now());

        UserAccount user1 = new UserAccount();
        user1.setUserId(10L);
        UserAccount user2 = new UserAccount();
        user2.setUserId(11L);

        when(regionRepository.findById(2L)).thenReturn(Optional.of(region));
        when(computedScoreRepository.findTopByRegionRegionIdAndScenarioIsNullOrderByLastComputedAtDesc(2L))
                .thenReturn(Optional.of(before), Optional.of(after));
        when(userRepository.findByIsActiveTrue()).thenReturn(List.of(user1, user2));

        ScoreDtos.ScoreResponse response = scoreService.computeScores(2L, 1L, httpServletRequest);

        assertEquals(RiskCategory.HIGH, response.riskCategory());
        verify(analyticsClient).computeRegion(2L);
        verify(notificationRepository, times(2)).save(any());
        verify(auditService).log(
                eq(1L),
                isNull(),
                eq(2L),
                eq(AuditActionType.COMPUTE),
                eq("regions"),
                eq("2"),
                eq("MEDIUM"),
                eq("HIGH"),
                eq(httpServletRequest)
        );
    }

    @Test
    void computeScoresDoesNotCreateNotificationsWhenRiskDoesNotEscalate() {
        Region region = org.mockito.Mockito.mock(Region.class);
        when(region.getRegionId()).thenReturn(2L);

        ComputedScore before = new ComputedScore();
        before.setRiskCategory(RiskCategory.MEDIUM);

        ComputedScore after = new ComputedScore();
        after.setRegion(region);
        after.setRiskCategory(RiskCategory.MEDIUM);
        after.setHaiScore(0.48);
        after.setRviScore(0.61);
        after.setCiriScore(0.55);
        after.setLastComputedAt(LocalDateTime.now());

        when(regionRepository.findById(2L)).thenReturn(Optional.of(region));
        when(computedScoreRepository.findTopByRegionRegionIdAndScenarioIsNullOrderByLastComputedAtDesc(2L))
                .thenReturn(Optional.of(before), Optional.of(after));

        ScoreDtos.ScoreResponse response = scoreService.computeScores(2L, 1L, httpServletRequest);

        assertEquals(RiskCategory.MEDIUM, response.riskCategory());
        verify(notificationRepository, times(0)).save(any());
    }
}
