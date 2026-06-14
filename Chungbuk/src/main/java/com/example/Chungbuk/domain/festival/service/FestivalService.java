package com.example.Chungbuk.domain.festival.service;

import com.example.Chungbuk.domain.festival.client.TourApiClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class FestivalService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 30;

    private final TourApiClient tourApiClient;

    public FestivalService(TourApiClient tourApiClient) {
        this.tourApiClient = tourApiClient;
    }

    public String getFestivalListRaw(Integer page, Integer size) {
        int pageNo = validatePage(page);
        int numOfRows = validateSize(size);
        String eventStartDate = getDefaultEventStartDate();

        return tourApiClient.getFestivalListRaw(pageNo, numOfRows, eventStartDate);
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