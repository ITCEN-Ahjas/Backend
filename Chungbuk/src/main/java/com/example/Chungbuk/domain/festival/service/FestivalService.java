package com.example.Chungbuk.domain.festival.service;

import com.example.Chungbuk.domain.festival.client.TourApiClient;
import com.example.Chungbuk.domain.festival.constant.SupportedContentType;
import com.example.Chungbuk.domain.festival.dto.response.ExperienceListResponse;
import com.example.Chungbuk.domain.festival.dto.response.ExperienceSummaryResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalDetailResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalListResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalSummaryResponse;
import com.example.Chungbuk.domain.festival.mapper.FestivalMapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FestivalService {

    private final TourApiClient tourApiClient;
    private final FestivalMapper festivalMapper;

    private static final int DEFAULT_PAGE = 1;
    private static final int DETAIL_FALLBACK_SIZE = 100;
    private static final String DEFAULT_FESTIVAL_CONTENT_TYPE_ID = "15";
    private static final String DETAIL_FALLBACK_EVENT_START_DATE = "20230101";
    private static final DateTimeFormatter TOUR_API_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

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
        String sigunguCode = resolveSigunguCode(region);

        String rawJson = tourApiClient.getFestivalListRaw(
                validPage,
                validSize,
                validEventStartDate,
                sigunguCode
        );

        FestivalListResponse response = festivalMapper.toFestivalListResponse(rawJson, validPage, validSize);

        List<FestivalSummaryResponse> filteredItems = response.getItems().stream()
                .filter(item -> matchesCategory(item.getCategory(), category))
                .filter(item -> matchesKeywordForFestival(item, keyword))
                .toList();

        return FestivalListResponse.builder()
                .items(filteredItems)
                .page(validPage)
                .size(validSize)
                .totalCount(filteredItems.size())
                .build();
    }

    public FestivalDetailResponse getFestivalDetail(String contentId) {
        String validContentId = safe(contentId);

        String detailCommonRawJson = tourApiClient.getFestivalDetailCommonRaw(validContentId);

        String fallbackListRawJson = tourApiClient.getFestivalListRaw(
                DEFAULT_PAGE,
                DETAIL_FALLBACK_SIZE,
                DETAIL_FALLBACK_EVENT_START_DATE,
                null
        );

        String contentTypeId = firstNonBlank(
                festivalMapper.extractContentTypeId(detailCommonRawJson),
                festivalMapper.extractContentTypeIdFromFallback(fallbackListRawJson, validContentId),
                DEFAULT_FESTIVAL_CONTENT_TYPE_ID
        );

        if (!SupportedContentType.isSupported(contentTypeId)) {
            contentTypeId = DEFAULT_FESTIVAL_CONTENT_TYPE_ID;
        }

        String detailIntroRawJson = tourApiClient.getFestivalDetailIntroRaw(
                validContentId,
                contentTypeId
        );

        String detailImageRawJson = tourApiClient.getFestivalDetailImageRaw(validContentId);

        return festivalMapper.toFestivalDetailResponse(
                detailCommonRawJson,
                detailIntroRawJson,
                detailImageRawJson,
                fallbackListRawJson,
                validContentId
        );
    }

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
        String sigunguCode = resolveSigunguCode(region);
        String resolvedContentTypeId = resolveExperienceContentTypeId(contentTypeId, category);

        if ("__UNSUPPORTED__".equals(resolvedContentTypeId)) {
            return ExperienceListResponse.builder()
                    .items(List.of())
                    .page(validPage)
                    .size(validSize)
                    .totalCount(0)
                    .build();
        }

        String rawJson = tourApiClient.getExperienceListRaw(
                validPage,
                validSize,
                sigunguCode,
                resolvedContentTypeId
        );

        ExperienceListResponse response = festivalMapper.toExperienceListResponse(rawJson, validPage, validSize);

        List<ExperienceSummaryResponse> filteredItems = response.getItems().stream()
                .filter(item -> matchesCategory(item.getCategory(), category))
                .filter(item -> matchesKeywordForExperience(item, keyword))
                .toList();

        return ExperienceListResponse.builder()
                .items(filteredItems)
                .page(validPage)
                .size(validSize)
                .totalCount(filteredItems.size())
                .build();
    }

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

    private String resolveExperienceContentTypeId(String contentTypeId, String category) {
        String contentTypeResult = SupportedContentType.resolveExperienceContentTypeId(contentTypeId);

        if (hasText(contentTypeResult)) {
            return contentTypeResult;
        }

        return SupportedContentType.resolveExperienceContentTypeId(category);
    }

    private boolean matchesCategory(String itemCategory, String requestedCategory) {
        if (!hasText(requestedCategory) || "전체".equals(requestedCategory.trim())) {
            return true;
        }

        return requestedCategory.trim().equals(itemCategory);
    }

    private boolean matchesKeywordForFestival(FestivalSummaryResponse item, String keyword) {
        if (!hasText(keyword)) {
            return true;
        }

        String normalizedKeyword = normalizeForSearch(keyword);

        String searchTarget = normalizeForSearch(
                item.getTitle() + " "
                        + item.getRegion() + " "
                        + item.getCategory() + " "
                        + item.getThemeCategory() + " "
                        + item.getAddress()
        );

        return searchTarget.contains(normalizedKeyword);
    }

    private boolean matchesKeywordForExperience(ExperienceSummaryResponse item, String keyword) {
        if (!hasText(keyword)) {
            return true;
        }

        String normalizedKeyword = normalizeForSearch(keyword);

        String searchTarget = normalizeForSearch(
                item.getTitle() + " "
                        + item.getRegion() + " "
                        + item.getCategory() + " "
                        + item.getThemeCategory() + " "
                        + item.getAddress()
        );

        return searchTarget.contains(normalizedKeyword);
    }

    private int normalizePage(int page) {
        if (page < 1) {
            return DEFAULT_PAGE;
        }

        return page;
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return 10;
        }

        if (size > 100) {
            return 100;
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
        if (!hasText(region) || "전체".equals(region.trim()) || "충북".equals(region.trim())) {
            return null;
        }

        return switch (region.trim()) {
            case "괴산" -> "1";
            case "단양" -> "2";
            case "보은" -> "3";
            case "영동" -> "4";
            case "옥천" -> "5";
            case "음성" -> "6";
            case "제천" -> "7";
            case "진천" -> "8";
            case "청주" -> "10";
            case "충주" -> "11";
            case "증평" -> "12";
            default -> null;
        };
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }

        return "";
    }

    private String normalizeForSearch(String value) {
        return safe(value)
                .replace(" ", "")
                .toLowerCase();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}