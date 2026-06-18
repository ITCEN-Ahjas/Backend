package com.example.Chungbuk.domain.festival.service;

import com.example.Chungbuk.domain.festival.client.TourApiClient;
import com.example.Chungbuk.domain.festival.constant.ChungbukRegion;
import com.example.Chungbuk.domain.festival.constant.SupportedContentType;
import com.example.Chungbuk.domain.festival.dto.response.ContentInfoResponse;
import com.example.Chungbuk.domain.festival.dto.response.ExperienceListResponse;
import com.example.Chungbuk.domain.festival.dto.response.ExperienceSummaryResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalDetailResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalListResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalSummaryResponse;
import com.example.Chungbuk.domain.festival.entity.FestivalContent;
import com.example.Chungbuk.domain.festival.repository.FestivalContentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FestivalService {

    private final TourApiClient tourApiClient;
    private final FestivalContentRepository festivalContentRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;
    private static final String FESTIVAL_CONTENT_TYPE_ID = "15";
    private static final DateTimeFormatter TOUR_API_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Transactional(readOnly = true)
    public FestivalListResponse getFestivals(
            int page,
            int size,
            String eventStartDate,
            String region,
            String category,
            String keyword
    ) {
        int validPage = normalizePage(page);
        int validSize = normalizeSize(size);
        String validEventStartDate = normalizeEventStartDate(eventStartDate);

        Specification<FestivalContent> specification = alwaysTrue()
                .and(activeOnly())
                .and(contentTypeEquals(FESTIVAL_CONTENT_TYPE_ID))
                .and(eventEndDateAfterOrEquals(validEventStartDate))
                .and(regionEquals(region))
                .and(categoryEquals(category))
                .and(keywordContains(keyword));

        PageRequest pageRequest = PageRequest.of(
                validPage - 1,
                validSize,
                Sort.by(
                        Sort.Order.asc("startDate"),
                        Sort.Order.desc("id")
                )
        );

        Page<FestivalContent> result = festivalContentRepository.findAll(specification, pageRequest);

        List<FestivalSummaryResponse> items = result.getContent().stream()
                .map(this::toFestivalSummaryResponse)
                .toList();

        return FestivalListResponse.builder()
                .items(items)
                .page(validPage)
                .size(validSize)
                .totalCount(toIntTotalCount(result.getTotalElements()))
                .build();
    }

    @Transactional(readOnly = true)
    public FestivalDetailResponse getFestivalDetail(String contentId) {
        String validContentId = safe(contentId);

        return festivalContentRepository.findByContentIdAndActiveTrue(validContentId)
                .map(this::toFestivalDetailResponse)
                .orElseGet(() -> emptyFestivalDetailResponse(validContentId));
    }

    @Transactional(readOnly = true)
    public ExperienceListResponse getExperiences(
            int page,
            int size,
            String region,
            String contentTypeId,
            String category,
            String keyword
    ) {
        int validPage = normalizePage(page);
        int validSize = normalizeSize(size);
        String resolvedContentTypeId = resolveExperienceContentTypeId(contentTypeId, category);

        Specification<FestivalContent> specification = alwaysTrue()
                .and(activeOnly())
                .and(experienceContentType(resolvedContentTypeId))
                .and(regionEquals(region))
                .and(categoryEquals(category))
                .and(keywordContains(keyword));

        PageRequest pageRequest = PageRequest.of(
                validPage - 1,
                validSize,
                Sort.by(
                        Sort.Order.asc("category"),
                        Sort.Order.asc("region"),
                        Sort.Order.desc("id")
                )
        );

        Page<FestivalContent> result = festivalContentRepository.findAll(specification, pageRequest);

        List<ExperienceSummaryResponse> items = result.getContent().stream()
                .map(this::toExperienceSummaryResponse)
                .toList();

        return ExperienceListResponse.builder()
                .items(items)
                .page(validPage)
                .size(validSize)
                .totalCount(toIntTotalCount(result.getTotalElements()))
                .build();
    }

    /*
     * raw API는 아직 TourAPI 원본 응답 확인용으로 유지한다.
     * 최종 캐시/Swagger 정리 커밋에서 유지 여부를 다시 결정한다.
     */
    public String getFestivalRaw(
            int page,
            int size,
            String eventStartDate,
            String region
    ) {
        int validPage = normalizePage(page);
        int validSize = normalizeSize(size);
        String validEventStartDate = normalizeEventStartDate(eventStartDate);
        String sigunguCode = resolveSigunguCode(region);

        return tourApiClient.getFestivalListRaw(
                validPage,
                validSize,
                validEventStartDate,
                sigunguCode
        );
    }

    private FestivalSummaryResponse toFestivalSummaryResponse(FestivalContent content) {
        return FestivalSummaryResponse.builder()
                .id(safe(content.getContentId()))
                .contentTypeId(safe(content.getContentTypeId()))
                .cat1(safe(content.getCat1()))
                .cat2(safe(content.getCat2()))
                .cat3(safe(content.getCat3()))
                .title(safe(content.getTitle()))
                .region(safe(content.getRegion()))
                .category(safe(content.getCategory()))
                .themeCategory(safe(content.getThemeCategory()))
                .status(safe(content.getStatus()))
                .startDate(safe(content.getStartDate()))
                .endDate(safe(content.getEndDate()))
                .address(safe(content.getAddress()))
                .imageUrl(safe(content.getImageUrl()))
                .tel(safe(content.getTel()))
                .mapX(safe(content.getMapX()))
                .mapY(safe(content.getMapY()))
                .timeLabel(safe(content.getTimeLabel()))
                .timeValue(safe(content.getTimeValue()))
                .extraLabel(safe(content.getExtraLabel()))
                .extraValue(safe(content.getExtraValue()))
                .build();
    }

    private ExperienceSummaryResponse toExperienceSummaryResponse(FestivalContent content) {
        return ExperienceSummaryResponse.builder()
                .id(safe(content.getContentId()))
                .contentTypeId(safe(content.getContentTypeId()))
                .cat1(safe(content.getCat1()))
                .cat2(safe(content.getCat2()))
                .cat3(safe(content.getCat3()))
                .title(safe(content.getTitle()))
                .region(safe(content.getRegion()))
                .category(safe(content.getCategory()))
                .themeCategory(safe(content.getThemeCategory()))
                .address(safe(content.getAddress()))
                .imageUrl(safe(content.getImageUrl()))
                .tel(safe(content.getTel()))
                .mapX(safe(content.getMapX()))
                .mapY(safe(content.getMapY()))
                .timeLabel(safe(content.getTimeLabel()))
                .timeValue(safe(content.getTimeValue()))
                .extraLabel(safe(content.getExtraLabel()))
                .extraValue(safe(content.getExtraValue()))
                .build();
    }

    private FestivalDetailResponse toFestivalDetailResponse(FestivalContent content) {
        List<String> imageUrls = parseImageUrls(content.getImageUrlsJson(), content.getImageUrl());
        List<ContentInfoResponse> mainInfo = parseMainInfo(content.getMainInfoJson());

        if (mainInfo.isEmpty()) {
            mainInfo = buildFallbackMainInfo(content);
        }

        return FestivalDetailResponse.builder()
                .id(safe(content.getContentId()))
                .contentTypeId(safe(content.getContentTypeId()))
                .cat1(safe(content.getCat1()))
                .cat2(safe(content.getCat2()))
                .cat3(safe(content.getCat3()))
                .title(safe(content.getTitle()))
                .region(safe(content.getRegion()))
                .category(safe(content.getCategory()))
                .themeCategory(safe(content.getThemeCategory()))
                .status(safe(content.getStatus()))
                .startDate(safe(content.getStartDate()))
                .endDate(safe(content.getEndDate()))
                .address(safe(content.getAddress()))
                .imageUrl(safe(content.getImageUrl()))
                .imageUrls(imageUrls)
                .tel(safe(content.getTel()))
                .homepage(safe(content.getHomepage()))
                .overview(safe(content.getOverview()))
                .description(safe(content.getDescription()))
                .descriptionSource(safe(content.getDescriptionSource()))
                .mapX(safe(content.getMapX()))
                .mapY(safe(content.getMapY()))
                .eventPlace(safe(content.getEventPlace()))
                .playTime(safe(content.getPlayTime()))
                .useTimeFestival(safe(content.getUseTimeFestival()))
                .sponsor(safe(content.getSponsor()))
                .timeLabel(safe(content.getTimeLabel()))
                .timeValue(safe(content.getTimeValue()))
                .extraLabel(safe(content.getExtraLabel()))
                .extraValue(safe(content.getExtraValue()))
                .mainInfo(mainInfo)
                .build();
    }

    private FestivalDetailResponse emptyFestivalDetailResponse(String contentId) {
        return FestivalDetailResponse.builder()
                .id(safe(contentId))
                .contentTypeId("")
                .cat1("")
                .cat2("")
                .cat3("")
                .title("")
                .region("")
                .category("")
                .themeCategory("")
                .status("")
                .startDate("")
                .endDate("")
                .address("")
                .imageUrl("")
                .imageUrls(List.of())
                .tel("")
                .homepage("")
                .overview("")
                .description("")
                .descriptionSource("")
                .mapX("")
                .mapY("")
                .eventPlace("")
                .playTime("")
                .useTimeFestival("")
                .sponsor("")
                .timeLabel("")
                .timeValue("")
                .extraLabel("")
                .extraValue("")
                .mainInfo(List.of())
                .build();
    }

    private List<String> parseImageUrls(String imageUrlsJson, String fallbackImageUrl) {
        List<String> imageUrls = new ArrayList<>();

        if (hasText(imageUrlsJson)) {
            try {
                JsonNode root = objectMapper.readTree(imageUrlsJson);

                if (root.isArray()) {
                    for (JsonNode item : root) {
                        String imageUrl = item.asText("");

                        if (hasText(imageUrl)) {
                            imageUrls.add(imageUrl);
                        }
                    }
                }
            } catch (Exception ignored) {
                imageUrls.clear();
            }
        }

        if (imageUrls.isEmpty() && hasText(fallbackImageUrl)) {
            imageUrls.add(fallbackImageUrl.trim());
        }

        return imageUrls;
    }

    private List<ContentInfoResponse> parseMainInfo(String mainInfoJson) {
        List<ContentInfoResponse> mainInfo = new ArrayList<>();

        if (!hasText(mainInfoJson)) {
            return mainInfo;
        }

        try {
            JsonNode root = objectMapper.readTree(mainInfoJson);

            if (!root.isArray()) {
                return mainInfo;
            }

            for (JsonNode item : root) {
                String label = item.path("label").asText("");
                String value = item.path("value").asText("");

                if (hasText(label) && hasText(value)) {
                    mainInfo.add(ContentInfoResponse.builder()
                            .label(label)
                            .value(value)
                            .build());
                }
            }
        } catch (Exception ignored) {
            mainInfo.clear();
        }

        return mainInfo;
    }

    private List<ContentInfoResponse> buildFallbackMainInfo(FestivalContent content) {
        List<ContentInfoResponse> mainInfo = new ArrayList<>();

        addMainInfo(mainInfo, content.getTimeLabel(), content.getTimeValue());
        addMainInfo(mainInfo, content.getExtraLabel(), content.getExtraValue());
        addMainInfo(mainInfo, "주소", content.getAddress());
        addMainInfo(mainInfo, "문의", content.getTel());

        return mainInfo;
    }

    private void addMainInfo(List<ContentInfoResponse> mainInfo, String label, String value) {
        if (!hasText(label) || !hasText(value)) {
            return;
        }

        mainInfo.add(ContentInfoResponse.builder()
                .label(label.trim())
                .value(value.trim())
                .build());
    }

    private Specification<FestivalContent> alwaysTrue() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
    }

    private Specification<FestivalContent> activeOnly() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isTrue(root.get("active"));
    }

    private Specification<FestivalContent> contentTypeEquals(String contentTypeId) {
        if (!hasText(contentTypeId) || "전체".equals(contentTypeId.trim())) {
            return alwaysTrue();
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("contentTypeId"), contentTypeId.trim());
    }

    private Specification<FestivalContent> experienceContentType(String contentTypeId) {
        if (hasText(contentTypeId) && !"전체".equals(contentTypeId.trim())) {
            return contentTypeEquals(contentTypeId);
        }

        return (root, query, criteriaBuilder) ->
                root.get("contentTypeId").in(List.of("12", "14", "28"));
    }

    private Specification<FestivalContent> regionEquals(String region) {
        if (!hasText(region) || "전체".equals(region.trim())) {
            return alwaysTrue();
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("region"), region.trim());
    }

    private Specification<FestivalContent> categoryEquals(String category) {
        if (!hasText(category) || "전체".equals(category.trim())) {
            return alwaysTrue();
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("category"), category.trim());
    }

    private Specification<FestivalContent> eventEndDateAfterOrEquals(String eventStartDate) {
        if (!hasText(eventStartDate)) {
            return alwaysTrue();
        }

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                        criteriaBuilder.isNull(root.get("endDate")),
                        criteriaBuilder.equal(root.get("endDate"), ""),
                        criteriaBuilder.greaterThanOrEqualTo(root.get("endDate"), eventStartDate.trim())
                );
    }

    private Specification<FestivalContent> keywordContains(String keyword) {
        if (!hasText(keyword)) {
            return alwaysTrue();
        }

        String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("title"), "")), likeKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("region"), "")), likeKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("category"), "")), likeKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("themeCategory"), "")), likeKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("address"), "")), likeKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("extraValue"), "")), likeKeyword)
                );
    }

    private String resolveExperienceContentTypeId(String contentTypeId, String category) {
        String contentTypeResult = SupportedContentType.resolveExperienceContentTypeId(contentTypeId);

        if (hasText(contentTypeResult)) {
            return contentTypeResult;
        }

        return SupportedContentType.resolveExperienceContentTypeId(category);
    }

    private int normalizePage(int page) {
        if (page < 1) {
            return DEFAULT_PAGE;
        }

        return page;
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }

        if (size > MAX_SIZE) {
            return MAX_SIZE;
        }

        return size;
    }

    private String normalizeEventStartDate(String eventStartDate) {
        if (hasText(eventStartDate) && eventStartDate.trim().matches("\\d{8}")) {
            return eventStartDate.trim();
        }

        return LocalDate.now().format(TOUR_API_DATE_FORMAT);
    }

    private String resolveSigunguCode(String region) {
        return ChungbukRegion.findSigunguCodeByName(region);
    }

    private int toIntTotalCount(long totalCount) {
        if (totalCount > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return (int) totalCount;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}