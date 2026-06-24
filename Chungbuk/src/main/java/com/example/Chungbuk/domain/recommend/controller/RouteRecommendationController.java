package com.example.Chungbuk.domain.recommend.controller;

import com.example.Chungbuk.domain.recommend.dto.request.RouteRecommendationRequest;
import com.example.Chungbuk.domain.recommend.dto.response.RouteRecommendationResponse;
import com.example.Chungbuk.domain.recommend.service.RouteRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommend")
@RequiredArgsConstructor
public class RouteRecommendationController {

    private final RouteRecommendationService routeRecommendationService;

    @PostMapping("/routes")
    public RouteRecommendationResponse recommendRoutes(
            @RequestBody RouteRecommendationRequest request
    ) {
        return routeRecommendationService.recommend(request);
    }
}
