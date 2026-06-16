package com.example.Chungbuk.domain.accommodation.controller;

import com.example.Chungbuk.domain.accommodation.dto.response.AccommodationDetailResponse;
import com.example.Chungbuk.domain.accommodation.dto.response.AccommodationListResponse;
import com.example.Chungbuk.domain.accommodation.service.AccommodationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accommodations")
@RequiredArgsConstructor
public class AccommodationController {

    private final AccommodationService accommodationService;

    @GetMapping
    public AccommodationListResponse getAccommodations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String region,
            @RequestParam(defaultValue = "전체") String category,
            @RequestParam(required = false) String keyword
    ) {
        return accommodationService.getAccommodations(
                page,
                size,
                region,
                category,
                keyword
        );
    }

    @GetMapping("/raw")
    public String getAccommodationRaw(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String region
    ) {
        return accommodationService.getAccommodationRaw(page, size, region);
    }

    @GetMapping("/{contentId}")
    public AccommodationDetailResponse getAccommodationDetail(
            @PathVariable String contentId
    ) {
        return accommodationService.getAccommodationDetail(contentId);
    }
}
