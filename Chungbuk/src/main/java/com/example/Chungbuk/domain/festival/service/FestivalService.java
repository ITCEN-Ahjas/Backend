package com.example.Chungbuk.domain.festival.service;

import com.example.Chungbuk.domain.festival.client.TourApiClient;
import com.example.Chungbuk.domain.festival.constant.ChungbukRegion;
import com.example.Chungbuk.domain.festival.dto.response.FestivalListResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalSummaryResponse;
import com.example.Chungbuk.domain.festival.mapper.FestivalMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FestivalService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 30;
    private static final DateTimeFormatter TOUR_API_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private final TourApiClient tourApiClient;
    private final FestivalMapper festivalMapper;

    public FestivalService(
            TourApiClient tourApiClient,
            FestivalMapper festivalMapper
    ) {
        this.tourApiClient = tourApiClient;
        this.festivalMapper = festivalMapper;
    }

    public FestivalListResponse getFestivalList(
            Integer page,
            Integer size,
            String eventStartDate,
            String region,
            String category,
            String keyword
    ) {
        int pageNo = validatePage(page);
        int numOfRows = validateSize(size);
        String startDate = resolveEventStartDate(eventStartDate);
        String sigunguCode = ChungbukRegion.findSigunguCodeByName(region);

        String rawJson = tourApiClient.getFestivalListRaw(
                pageNo,
                numOfRows,
                startDate,
                sigunguCode
        );

        FestivalListResponse response = festivalMapper.toFestivalListResponse(
                rawJson,
                pageNo,
                numOfRows
        );

        FestivalListResponse categoryFilteredResponse =
                applyCategoryFilter(response, category);

        return applyKeywordSearch(categoryFilteredResponse, keyword);
    }

    public String getFestivalListRaw(
            Integer page,
            Integer size,
            String eventStartDate,
            String region
    ) {
        int pageNo = validatePage(page);
        int numOfRows = validateSize(size);
        String startDate = resolveEventStartDate(eventStartDate);
        String sigunguCode = ChungbukRegion.findSigunguCodeByName(region);

        return tourApiClient.getFestivalListRaw(
                pageNo,
                numOfRows,
                startDate,
                sigunguCode
        );
    }

    private FestivalListResponse applyCategoryFilter(
            FestivalListResponse response,
            String category
    ) {
        if (category == null || category.isBlank() || category.equals("전체")) {
            return response;
        }

        List<FestivalSummaryResponse> filteredItems = response.getItems()
                .stream()
                .filter(item -> category.equals(item.getCategory()))
                .collect(Collectors.toList());

        return FestivalListResponse.builder()
                .items(filteredItems)
                .page(response.getPage())
                .size(response.getSize())
                .totalCount(filteredItems.size())
                .build();
    }

    private FestivalListResponse applyKeywordSearch(
            FestivalListResponse response,
            String keyword
    ) {
        if (keyword == null || keyword.isBlank()) {
            return response;
        }

        String normalizedKeyword = keyword.trim().toLowerCase();

        List<FestivalSummaryResponse> searchedItems = response.getItems()
                .stream()
                .filter(item -> containsKeyword(item, normalizedKeyword))
                .collect(Collectors.toList());

        return FestivalListResponse.builder()
                .items(searchedItems)
                .page(response.getPage())
                .size(response.getSize())
                .totalCount(searchedItems.size())
                .build();
    }

    private boolean containsKeyword(
            FestivalSummaryResponse item,
            String keyword
    ) {
        String searchableText = String.join(" ",
                nullToEmpty(item.getTitle()),
                nullToEmpty(item.getRegion()),
                nullToEmpty(item.getCategory()),
                nullToEmpty(item.getAddress())
        ).toLowerCase();

        return searchableText.contains(keyword);
    }

    private String nullToEmpty(String value) {
        if (value == null) {
            return "";
        }

        return value;
    }

    private int validatePage(Integer page) {
        if (page == null || page < 1) {
            return DEFAULT_PAGE;
        }

        return page;
    }

    private int validateSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }

        return Math.min(size, MAX_SIZE);
    }

    private String resolveEventStartDate(String eventStartDate) {
        if (eventStartDate == null || eventStartDate.isBlank()) {
            return getDefaultEventStartDate();
        }

        if (!eventStartDate.matches("\\d{8}")) {
            return getDefaultEventStartDate();
        }

        return eventStartDate;
    }

    private String getDefaultEventStartDate() {
        return LocalDate.now()
                .withDayOfYear(1)
                .format(TOUR_API_DATE_FORMAT);
    }
}