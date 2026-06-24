package com.example.Chungbuk.domain.recommend.service;

import com.example.Chungbuk.domain.recommend.dto.request.RouteRecommendationRequest;
import com.example.Chungbuk.domain.recommend.dto.response.RouteRecommendationResponse;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RouteRecommendationService {

    private static final String DEFAULT_REGION = "충북";
    private static final String DEFAULT_START_TIME = "09:00";
    private static final String DEFAULT_PLACE_NAME = "상당산성";

    public RouteRecommendationResponse recommend(RouteRecommendationRequest request) {
        String region = getOrDefault(
                request == null ? null : request.getRegion(),
                DEFAULT_REGION
        );
        String startTime = getOrDefault(
                request == null ? null : request.getStartTime(),
                DEFAULT_START_TIME
        );
        String transportMode = getOrDefault(
                request == null ? null : request.getTransportMode(),
                "car"
        );
        String firstPlaceName = getFirstCandidatePlaceName(request);

        return RouteRecommendationResponse.builder()
                .summary(region + " 여행 조건에 맞춘 당일 추천 코스입니다.")
                .weatherNotes(List.of(
                        "시간대별 날씨가 제공되면 우천과 고온 시간대를 피해 코스를 조정합니다.",
                        "현재 응답은 프론트 연동 확인을 위한 기본 추천입니다."
                ))
                .itinerary(List.of(
                        createItineraryItem(
                                startTime,
                                firstPlaceName,
                                region + "의 대표 장소를 먼저 방문하는 코스입니다.",
                                "야외 활동은 오전 시간대에 배치해 날씨 변화 영향을 줄였습니다.",
                                createMoveTip(transportMode)
                        )
                ))
                .planB(List.of(
                        "비가 올 경우 박물관, 전시관, 카페 등 실내 장소 중심으로 변경하세요.",
                        "강풍이나 폭염이 있으면 야외 체류 시간을 줄이고 이동 거리가 짧은 장소를 선택하세요."
                ))
                .build();
    }

    private RouteRecommendationResponse.ItineraryItem createItineraryItem(
            String time,
            String placeName,
            String description,
            String weatherReason,
            String moveTip
    ) {
        return RouteRecommendationResponse.ItineraryItem.builder()
                .time(time)
                .placeName(placeName)
                .description(description)
                .weatherReason(weatherReason)
                .moveTip(moveTip)
                .build();
    }

    private String getFirstCandidatePlaceName(RouteRecommendationRequest request) {
        if (request == null
                || request.getCandidatePlaces() == null
                || request.getCandidatePlaces().isEmpty()) {
            return DEFAULT_PLACE_NAME;
        }

        Map<String, Object> firstPlace = request.getCandidatePlaces().getFirst();
        for (String key : List.of("placeName", "name", "title")) {
            Object value = firstPlace.get(key);
            if (value instanceof String text && !text.isBlank()) {
                return text;
            }
        }

        return DEFAULT_PLACE_NAME;
    }

    private String createMoveTip(String transportMode) {
        return switch (transportMode) {
            case "walk" -> "도보 이동 시간을 고려해 가까운 장소 위주로 이동하세요.";
            case "publicTransport" -> "대중교통 배차 간격을 확인하고 환승 시간을 여유 있게 잡으세요.";
            default -> "차량 이동 기준으로 주차 가능 여부와 혼잡 시간을 확인하세요.";
        };
    }

    private String getOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }
}
