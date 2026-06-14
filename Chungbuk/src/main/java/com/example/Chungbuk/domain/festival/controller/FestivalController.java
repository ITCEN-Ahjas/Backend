package com.example.Chungbuk.domain.festival.controller;

import com.example.Chungbuk.domain.festival.service.FestivalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/festivals")
public class FestivalController {

    private final FestivalService festivalService;

    public FestivalController(FestivalService festivalService) {
        this.festivalService = festivalService;
    }

    @GetMapping("/raw")
    public String getFestivalListRaw(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        return festivalService.getFestivalListRaw(page, size);
    }
}