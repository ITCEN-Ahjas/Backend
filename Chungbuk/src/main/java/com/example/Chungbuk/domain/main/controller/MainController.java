package com.example.Chungbuk.domain.main.controller;

import com.example.Chungbuk.domain.main.dto.response.MainSummaryResponse;
import com.example.Chungbuk.domain.main.service.MainSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/main")
@RequiredArgsConstructor
public class MainController {

    private final MainSummaryService mainSummaryService;

    @GetMapping
    public MainSummaryResponse getMainSummary() {
        return mainSummaryService.getMainSummary();
    }
}
