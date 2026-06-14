package com.example.Chungbuk.domain.festival.service;

import com.example.Chungbuk.domain.festival.client.TourApiClient;
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

    private final TourApiClient tourApiClient;
    private final FestivalMapper festivalMapper;

    public FestivalService(
            TourApiClient tourApiClient,
            FestivalMapper festivalMapper
    ) {
        this.tourApiClient = tourApiClient;
        this.festivalMapper = festivalMapper;
    }

    public FestivalListResponse getFestivalList(Integer page, Integer size) {
        int pageNo = validatePage(page);
        int numOfRows = validateSize(size);
        String eventStartDate = getDefaultEventStartDate();

        String rawJson = tourApiClient.getFestivalListRaw(
                pageNo,
                numOfRows,
                eventStartDate
        );

        return festivalMapper.toFestivalListResponse(
                rawJson,
                pageNo,
                numOfRows
        );
    }

    public String getFestivalListRaw(Integer page, Integer size) {
        int pageNo = validatePage(page);
        int numOfRows = validateSize(size);
        String eventStartDate = getDefaultEventStartDate();

        return tourApiClient.getFestivalListRaw(
                pageNo,
                numOfRows,
                eventStartDate
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

    private String getDefaultEventStartDate() {
        return LocalDate.now()
                .withDayOfYear(1)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }
}