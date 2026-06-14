package com.example.Chungbuk.domain.festival.service;

import com.example.Chungbuk.domain.festival.client.TourApiClient;
import com.example.Chungbuk.domain.festival.constant.ChungbukRegion;
import com.example.Chungbuk.domain.festival.dto.response.FestivalListResponse;
import com.example.Chungbuk.domain.festival.mapper.FestivalMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
            String region
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

        return festivalMapper.toFestivalListResponse(
                rawJson,
                pageNo,
                numOfRows
        );
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