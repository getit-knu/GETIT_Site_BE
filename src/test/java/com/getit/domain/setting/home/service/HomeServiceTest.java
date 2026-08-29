package com.getit.domain.setting.home.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.domain.project.dto.ProjectCommand;
import com.getit.domain.project.entity.Project;
import com.getit.domain.project.repository.ProjectRepository;
import com.getit.domain.recruitment.entity.RecruitmentPhase;
import com.getit.domain.recruitment.entity.RecruitmentSchedule;
import com.getit.domain.recruitment.repository.RecruitmentScheduleRepository;
import com.getit.domain.setting.curriculum.entity.Curriculum;
import com.getit.domain.setting.curriculum.repository.CurriculumRepository;
import com.getit.domain.setting.faq.dto.FaqCommand;
import com.getit.domain.setting.faq.entity.Faq;
import com.getit.domain.setting.faq.repository.FaqRepository;
import com.getit.domain.setting.feature.entity.FeatureKey;
import com.getit.domain.setting.feature.entity.FeatureToggle;
import com.getit.domain.setting.feature.repository.FeatureToggleRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.setting.home.dto.HomeResult;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class HomeServiceTest {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

  @Autowired
  private HomeService homeService;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private RecruitmentScheduleRepository recruitmentScheduleRepository;

  @Autowired
  private CurriculumRepository curriculumRepository;

  @Autowired
  private ProjectRepository projectRepository;

  @Autowired
  private FileAssetRepository fileAssetRepository;

  @Autowired
  private FaqRepository faqRepository;

  @Autowired
  private FeatureToggleRepository featureToggleRepository;

  @Test
  @DisplayName("활성 기수가 있으면 각 필드를 채워서 반환한다")
  void returnsAggregatedHomeWhenActiveGenerationExists() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    generationRepository.save(generation);
    // 실제 서비스(RecruitmentStatusService)는 Asia/Seoul Clock 으로 "지금"을 구한다 — JVM 기본
    // 타임존이 다르면(CI 는 흔히 UTC) LocalDateTime.now() 로 만든 값과 어긋나 경계에서 phase 판정이
    // 흔들릴 수 있어, 서비스와 같은 기준으로 한 번만 계산해서 재사용한다(PR #129 Copilot 리뷰 지적).
    LocalDateTime now = LocalDateTime.now(SEOUL);
    recruitmentScheduleRepository.save(RecruitmentSchedule.create(
        generation.getId(),
        now.minusDays(1), now.plusDays(10),
        now.minusDays(1), now.plusDays(5),
        now.plusDays(6)));
    curriculumRepository.save(Curriculum.create(generation.getId(), 1, "Python & 데이터 분석", "부제"));
    FileAsset file = fileAssetRepository.save(
        FileAsset.upload("p1", "p1.png", "https://cdn.getit.com/projects/1.png", 100L, "image/png", 1L));
    projectRepository.save(Project.create(
        new ProjectCommand("주식 포트폴리오 추천 시스템", "팀", "2026-1", "설명", List.of("Python"),
            null, null, true, file.getId()),
        1));
    faqRepository.save(Faq.create(new FaqCommand("동아리 활동 시간은?", "매주 화요일", true), 1));
    featureToggleRepository.save(FeatureToggle.create(FeatureKey.MOCK_INVESTMENT, true));

    HomeResult result = homeService.getHome();

    assertThat(result.generation()).isEqualTo(new HomeResult.GenerationInfo(9, 2026));
    assertThat(result.recruitment().phase()).isEqualTo(RecruitmentPhase.DOCUMENT_OPEN);
    assertThat(result.recruitment().applyEnabled()).isTrue();
    assertThat(result.curriculums()).extracting(HomeResult.CurriculumInfo::title)
        .containsExactly("Python & 데이터 분석");
    assertThat(result.featuredProjects()).hasSize(1);
    assertThat(result.featuredProjects().get(0).thumbnailUrl()).endsWith("/" + file.getStoredKey());
    assertThat(result.faqs()).extracting(HomeResult.FaqInfo::question).containsExactly("동아리 활동 시간은?");
    assertThat(result.features()).isEqualTo(new HomeResult.FeaturesInfo(false, true));
  }

  @Test
  @DisplayName("활성 기수가 없으면 generation 은 null, recruitment 는 CLOSED, curriculums 는 빈 리스트다")
  void returnsGracefulResultWhenNoActiveGeneration() {
    HomeResult result = homeService.getHome();

    assertThat(result.generation()).isNull();
    assertThat(result.recruitment().phase()).isEqualTo(RecruitmentPhase.CLOSED);
    assertThat(result.recruitment().totalStartAt()).isNull();
    assertThat(result.curriculums()).isEmpty();
  }

  @Test
  @DisplayName("featuredProjects 의 fileId 가 없으면 thumbnailUrl 은 null 이다")
  void returnsNullThumbnailWhenFileIdMissing() {
    projectRepository.save(Project.create(
        new ProjectCommand("파일 없는 프로젝트", "팀", "2026-1", "설명", List.of(), null, null, true, null),
        1));

    HomeResult result = homeService.getHome();

    assertThat(result.featuredProjects()).hasSize(1);
    assertThat(result.featuredProjects().get(0).thumbnailUrl()).isNull();
  }

  @Test
  @DisplayName("features 는 시드되지 않은 키도 항상 false 로 채워 반환한다")
  void returnsFeaturesWithDefaultsForUnseededKeys() {
    HomeResult result = homeService.getHome();

    assertThat(result.features()).isEqualTo(new HomeResult.FeaturesInfo(false, false));
  }
}
