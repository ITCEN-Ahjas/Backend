package com.example.Chungbuk.domain.weather.service;

import com.example.Chungbuk.domain.weather.client.AiOutfitRecommendationClient;
import com.example.Chungbuk.domain.weather.constant.TravelStyle;
import com.example.Chungbuk.domain.weather.dto.request.AiOutfitBatchRecommendationRequest;
import com.example.Chungbuk.domain.weather.dto.request.AiOutfitRecommendationRequest;
import com.example.Chungbuk.domain.weather.dto.request.AiTimeSlotOutfitRecommendationRequest;
import com.example.Chungbuk.domain.weather.dto.request.OutfitRecommendationRequest;
import com.example.Chungbuk.domain.weather.dto.request.RegionWeatherRequest;
import com.example.Chungbuk.domain.weather.dto.response.AiOutfitBatchRecommendationResponse;
import com.example.Chungbuk.domain.weather.dto.response.AiOutfitRecommendationResponse;
import com.example.Chungbuk.domain.weather.dto.response.AiTimeSlotOutfitBatchRecommendationResponse;
import com.example.Chungbuk.domain.weather.dto.response.CurrentWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.FeelsLikeWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.RegionBatchOutfitRecommendationResponse;
import com.example.Chungbuk.domain.weather.dto.response.RegionOutfitRecommendationResponse;
import com.example.Chungbuk.domain.weather.dto.response.RegionTimeSlotOutfitRecommendationResponse;
import com.example.Chungbuk.domain.weather.dto.response.RegionTimeSlotWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.ResidenceCityWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.ResidenceWeatherComparisonResponse;
import com.example.Chungbuk.domain.weather.dto.response.TimeSlotOutfitRecommendationResponse;
import com.example.Chungbuk.domain.weather.dto.response.TimeSlotWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.WeatherPageResponse;
import com.example.Chungbuk.global.exception.AiOutfitApiException;
import com.example.Chungbuk.global.exception.InvalidRequestException;
import com.example.Chungbuk.global.exception.ResidenceWeatherApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OutfitRecommendationService {

    private final WeatherService weatherService;

    private final AiOutfitRecommendationClient
            aiOutfitRecommendationClient;

    private final ResidenceCityWeatherService
            residenceCityWeatherService;

    @Autowired
    public OutfitRecommendationService(
            WeatherService weatherService,
            AiOutfitRecommendationClient aiOutfitRecommendationClient,
            ResidenceCityWeatherService residenceCityWeatherService
    ) {
        this.weatherService = weatherService;
        this.aiOutfitRecommendationClient =
                aiOutfitRecommendationClient;
        this.residenceCityWeatherService =
                residenceCityWeatherService;
    }

    OutfitRecommendationService(
            WeatherService weatherService,
            AiOutfitRecommendationClient aiOutfitRecommendationClient
    ) {
        this(
                weatherService,
                aiOutfitRecommendationClient,
                null
        );
    }

    public RegionOutfitRecommendationResponse recommend(
            OutfitRecommendationRequest request
    ) {
        validateRegion(request.getRegion());

        TravelStyle travelStyle =
                TravelStyle.fromDisplayName(
                        request.getTravelStyle()
                );

        WeatherPageResponse weatherPageResponse =
                weatherService.getRegionWeather(
                        createRegionWeatherRequest(request.getRegion())
                );

        AiOutfitRecommendationResponse aiResponse =
                aiOutfitRecommendationClient.recommend(
                        createAiRequest(
                                weatherPageResponse,
                                travelStyle
                        )
                );

        validateAiResponse(aiResponse);

        return new RegionOutfitRecommendationResponse(
                weatherPageResponse.getRegion(),
                weatherPageResponse.getUpdatedAt(),
                travelStyle.getDisplayName(),
                weatherPageResponse.getCurrentWeather(),
                weatherPageResponse.getFeelsLikeWeather(),
                aiResponse.getOutfitCards(),
                aiResponse.getPreparationItems()
        );
    }

    public RegionBatchOutfitRecommendationResponse recommendBatch(
            String region
    ) {
        validateRegion(region);

        WeatherPageResponse weatherPageResponse =
                weatherService.getRegionWeather(
                        createRegionWeatherRequest(region)
                );

        AiOutfitBatchRecommendationResponse aiResponse =
                aiOutfitRecommendationClient.recommendBatch(
                        createAiBatchRequest(weatherPageResponse)
                );

        Map<String, AiOutfitRecommendationResponse>
                recommendations =
                validateAndOrderBatchAiResponse(aiResponse);

        return new RegionBatchOutfitRecommendationResponse(
                weatherPageResponse.getRegion(),
                weatherPageResponse.getUpdatedAt(),
                aiResponse.getSource(),
                weatherPageResponse.getCurrentWeather(),
                weatherPageResponse.getFeelsLikeWeather(),
                recommendations
        );
    }

    public RegionTimeSlotOutfitRecommendationResponse
    recommendTimeSlots(String region) {
        return recommendTimeSlots(region, null, null);
    }

    public RegionTimeSlotOutfitRecommendationResponse
    recommendTimeSlots(
            String region,
            String residenceCity,
            String residenceCountryCode
    ) {
        validateRegion(region);
        validateResidenceLocationPair(
                residenceCity,
                residenceCountryCode
        );

        RegionTimeSlotWeatherResponse weatherResponse =
                weatherService.getRegionTimeSlotWeather(
                        createRegionWeatherRequest(region)
                );

        AiTimeSlotOutfitBatchRecommendationResponse aiResponse =
                aiOutfitRecommendationClient.recommendTimeSlots(
                        createAiTimeSlotRequest(weatherResponse)
                );

        ResidenceCityWeatherResponse residenceWeather =
                getResidenceWeatherOrNull(
                        residenceCity,
                        residenceCountryCode
                );

        List<TimeSlotOutfitRecommendationResponse> recommendations =
                mergeTimeSlotRecommendations(
                        weatherResponse,
                        aiResponse,
                        residenceWeather
                );

        return new RegionTimeSlotOutfitRecommendationResponse(
                weatherResponse.getRegion(),
                weatherResponse.getUpdatedAt(),
                weatherResponse.getForecastDate(),
                aiResponse.getSource(),
                residenceWeather,
                recommendations
        );
    }

    private ResidenceCityWeatherResponse getResidenceWeatherOrNull(
            String residenceCity,
            String residenceCountryCode
    ) {
        if (!hasText(residenceCity)) {
            return null;
        }

        try {
            return residenceCityWeatherService.getCurrentWeather(
                    residenceCountryCode,
                    residenceCity
            );
        } catch (ResidenceWeatherApiException exception) {
            return null;
        }
    }

    private RegionWeatherRequest createRegionWeatherRequest(
            String region
    ) {
        RegionWeatherRequest request =
                new RegionWeatherRequest();

        request.setRegion(region.trim());

        return request;
    }

    private AiOutfitRecommendationRequest createAiRequest(
            WeatherPageResponse weatherPageResponse,
            TravelStyle travelStyle
    ) {
        return new AiOutfitRecommendationRequest(
                weatherPageResponse.getRegion(),
                travelStyle.getDisplayName(),
                createAiCurrentWeather(weatherPageResponse),
                createAiFeelsLikeWeather(weatherPageResponse)
        );
    }

    private AiOutfitBatchRecommendationRequest createAiBatchRequest(
            WeatherPageResponse weatherPageResponse
    ) {
        return new AiOutfitBatchRecommendationRequest(
                weatherPageResponse.getRegion(),
                createAiCurrentWeather(weatherPageResponse),
                createAiFeelsLikeWeather(weatherPageResponse)
        );
    }

    private AiTimeSlotOutfitRecommendationRequest
    createAiTimeSlotRequest(
            RegionTimeSlotWeatherResponse weatherResponse
    ) {
        List<AiTimeSlotOutfitRecommendationRequest.TimeSlotWeather>
                timeSlots = weatherResponse.getTimeSlots().stream()
                .map(this::createAiTimeSlotWeather)
                .toList();

        return new AiTimeSlotOutfitRecommendationRequest(
                weatherResponse.getRegion(),
                timeSlots
        );
    }

    private AiTimeSlotOutfitRecommendationRequest.TimeSlotWeather
    createAiTimeSlotWeather(
            TimeSlotWeatherResponse timeSlotWeather
    ) {
        return new AiTimeSlotOutfitRecommendationRequest.TimeSlotWeather(
                timeSlotWeather.getTimeSlot(),
                timeSlotWeather.getTimeSlotName(),
                timeSlotWeather.getForecastAt(),
                timeSlotWeather.getStartTime(),
                timeSlotWeather.getEndTime(),
                createAiCurrentWeather(timeSlotWeather),
                createAiFeelsLikeWeather(timeSlotWeather)
        );
    }

    private AiOutfitRecommendationRequest.CurrentWeather
    createAiCurrentWeather(
            WeatherPageResponse weatherPageResponse
    ) {
        return createAiCurrentWeather(
                weatherPageResponse.getCurrentWeather()
        );
    }

    private AiOutfitRecommendationRequest.CurrentWeather
    createAiCurrentWeather(
            TimeSlotWeatherResponse timeSlotWeather
    ) {
        return createAiCurrentWeather(
                timeSlotWeather.getCurrentWeather()
        );
    }

    private AiOutfitRecommendationRequest.CurrentWeather
    createAiCurrentWeather(
            CurrentWeatherResponse currentWeather
    ) {
        return new AiOutfitRecommendationRequest.CurrentWeather(
                currentWeather.getTemperature(),
                currentWeather.getHumidity(),
                currentWeather.getWindSpeed(),
                currentWeather.getWindStatus(),
                currentWeather.getPrecipitationAmount(),
                currentWeather.getPrecipitationType(),
                currentWeather.getPrecipitationProbability(),
                currentWeather.getSkyStatus(),
                currentWeather.getWeatherCondition()
        );
    }

    private AiOutfitRecommendationRequest.FeelsLikeWeather
    createAiFeelsLikeWeather(
            WeatherPageResponse weatherPageResponse
    ) {
        return createAiFeelsLikeWeather(
                weatherPageResponse.getFeelsLikeWeather()
        );
    }

    private AiOutfitRecommendationRequest.FeelsLikeWeather
    createAiFeelsLikeWeather(
            TimeSlotWeatherResponse timeSlotWeather
    ) {
        return createAiFeelsLikeWeather(
                timeSlotWeather.getFeelsLikeWeather()
        );
    }

    private AiOutfitRecommendationRequest.FeelsLikeWeather
    createAiFeelsLikeWeather(
            FeelsLikeWeatherResponse feelsLikeWeather
    ) {
        return new AiOutfitRecommendationRequest.FeelsLikeWeather(
                feelsLikeWeather.getFeelsLikeTemperature(),
                feelsLikeWeather.getTemperatureDifference(),
                feelsLikeWeather.getDescription(),
                feelsLikeWeather.getFactors()
        );
    }

    private List<TimeSlotOutfitRecommendationResponse>
    mergeTimeSlotRecommendations(
            RegionTimeSlotWeatherResponse weatherResponse,
            AiTimeSlotOutfitBatchRecommendationResponse aiResponse,
            ResidenceCityWeatherResponse residenceWeather
    ) {
        Map<String, AiTimeSlotOutfitBatchRecommendationResponse
                .TimeSlotOutfitRecommendation> recommendationsByTimeSlot =
                validateAndIndexTimeSlotAiResponse(
                        weatherResponse,
                        aiResponse
                );

        List<TimeSlotOutfitRecommendationResponse> recommendations =
                new ArrayList<>();

        for (TimeSlotWeatherResponse weatherTimeSlot
                : weatherResponse.getTimeSlots()) {
            AiTimeSlotOutfitBatchRecommendationResponse
                    .TimeSlotOutfitRecommendation aiRecommendation =
                    recommendationsByTimeSlot.get(
                            weatherTimeSlot.getTimeSlot()
                    );

            recommendations.add(
                    new TimeSlotOutfitRecommendationResponse(
                            weatherTimeSlot.getTimeSlot(),
                            weatherTimeSlot.getTimeSlotName(),
                            weatherTimeSlot.getForecastAt(),
                            weatherTimeSlot.getStartTime(),
                            weatherTimeSlot.getEndTime(),
                            weatherTimeSlot.getCurrentWeather(),
                            weatherTimeSlot.getFeelsLikeWeather(),
                            createResidenceComparison(
                                    residenceWeather,
                                    weatherTimeSlot.getFeelsLikeWeather()
                            ),
                            aiRecommendation.getOutfitCards(),
                            aiRecommendation.getPreparationItems()
                    )
            );
        }

        return List.copyOf(recommendations);
    }

    private ResidenceWeatherComparisonResponse
    createResidenceComparison(
            ResidenceCityWeatherResponse residenceWeather,
            FeelsLikeWeatherResponse targetFeelsLikeWeather
    ) {
        if (residenceWeather == null) {
            return null;
        }

        double targetTemperature =
                targetFeelsLikeWeather.getFeelsLikeTemperature();
        double difference = targetTemperature
                - residenceWeather.getFeelsLikeTemperature();
        double roundedDifference = Math.round(difference * 10.0) / 10.0;
        double absoluteDifference = Math.abs(roundedDifference);

        String message = createComparisonMessage(
                residenceWeather.getCity(),
                roundedDifference,
                absoluteDifference
        );

        return new ResidenceWeatherComparisonResponse(
                residenceWeather.getCity(),
                residenceWeather.getCountry(),
                residenceWeather.getFeelsLikeTemperature(),
                targetTemperature,
                roundedDifference,
                message
        );
    }

    private String createComparisonMessage(
            String residenceCity,
            double difference,
            double absoluteDifference
    ) {
        if (absoluteDifference < 1.0) {
            return "현재 거주 도시 " + residenceCity
                    + "와 체감온도가 비슷해요.";
        }

        int roundedGap = Math.max(
                1,
                (int) Math.round(absoluteDifference)
        );

        if (difference < 0) {
            return "현재 거주 도시 " + residenceCity
                    + "보다 약 " + roundedGap + "°C 더 선선해요.";
        }

        return "현재 거주 도시 " + residenceCity
                + "보다 약 " + roundedGap + "°C 더 더워요.";
    }

    private Map<String, AiTimeSlotOutfitBatchRecommendationResponse
            .TimeSlotOutfitRecommendation>
    validateAndIndexTimeSlotAiResponse(
            RegionTimeSlotWeatherResponse weatherResponse,
            AiTimeSlotOutfitBatchRecommendationResponse aiResponse
    ) {
        if (aiResponse == null
                || !isAllowedTimeSlotSource(aiResponse.getSource())
                || aiResponse.getRecommendations() == null
                || aiResponse.getRecommendations().isEmpty()) {

            throw new AiOutfitApiException(
                    "AI 시간대별 옷차림 추천 응답 형식이 올바르지 않습니다."
            );
        }

        Map<String, AiTimeSlotOutfitBatchRecommendationResponse
                .TimeSlotOutfitRecommendation> recommendations =
                new LinkedHashMap<>();

        for (AiTimeSlotOutfitBatchRecommendationResponse
                .TimeSlotOutfitRecommendation recommendation
                : aiResponse.getRecommendations()) {

            validateTimeSlotAiRecommendation(recommendation);

            AiTimeSlotOutfitBatchRecommendationResponse
                    .TimeSlotOutfitRecommendation previous =
                    recommendations.put(
                            recommendation.getTimeSlot(),
                            recommendation
                    );

            if (previous != null) {
                throw new AiOutfitApiException(
                        "AI 시간대별 추천 결과에 중복된 시간대가 있습니다."
                );
            }
        }

        if (recommendations.size() != weatherResponse
                .getTimeSlots()
                .size()) {

            throw new AiOutfitApiException(
                    "AI 시간대별 추천 결과 개수가 날씨 시간대와 다릅니다."
            );
        }

        for (TimeSlotWeatherResponse weatherTimeSlot
                : weatherResponse.getTimeSlots()) {
            if (!recommendations.containsKey(
                    weatherTimeSlot.getTimeSlot()
            )) {
                throw new AiOutfitApiException(
                        "AI 시간대별 추천 결과에 "
                                + weatherTimeSlot.getTimeSlotName()
                                + " 항목이 없습니다."
                );
            }
        }

        return recommendations;
    }

    private boolean isAllowedTimeSlotSource(String source) {
        return "ai".equals(source) || "fallback".equals(source);
    }

    private void validateTimeSlotAiRecommendation(
            AiTimeSlotOutfitBatchRecommendationResponse
                    .TimeSlotOutfitRecommendation recommendation
    ) {
        if (recommendation == null
                || recommendation.getTimeSlot() == null
                || recommendation.getTimeSlot().isBlank()
                || recommendation.getOutfitCards() == null
                || recommendation.getOutfitCards().getOuterwear() == null
                || recommendation.getOutfitCards().getTop() == null
                || recommendation.getOutfitCards().getBottom() == null
                || recommendation.getOutfitCards().getShoes() == null
                || recommendation.getPreparationItems() == null
                || recommendation.getPreparationItems().isEmpty()) {

            throw new AiOutfitApiException(
                    "AI 시간대별 옷차림 추천 항목이 올바르지 않습니다."
            );
        }
    }

    private void validateRegion(String region) {
        if (region == null || region.isBlank()) {
            throw new InvalidRequestException(
                    "지역을 선택해 주세요."
            );
        }
    }

    private void validateResidenceLocationPair(
            String residenceCity,
            String residenceCountryCode
    ) {
        boolean hasCity = hasText(residenceCity);
        boolean hasCountryCode = hasText(residenceCountryCode);

        if (hasCity != hasCountryCode) {
            throw new InvalidRequestException(
                    "현재 거주 도시와 국가 코드를 함께 입력해 주세요."
            );
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void validateAiResponse(
            AiOutfitRecommendationResponse aiResponse
    ) {
        if (aiResponse == null
                || aiResponse.getOutfitCards() == null
                || aiResponse.getOutfitCards().getOuterwear() == null
                || aiResponse.getOutfitCards().getTop() == null
                || aiResponse.getOutfitCards().getBottom() == null
                || aiResponse.getOutfitCards().getShoes() == null
                || aiResponse.getPreparationItems() == null) {

            throw new AiOutfitApiException(
                    "AI 옷차림 추천 응답 형식이 올바르지 않습니다."
            );
        }
    }

    private Map<String, AiOutfitRecommendationResponse>
    validateAndOrderBatchAiResponse(
            AiOutfitBatchRecommendationResponse aiResponse
    ) {
        if (aiResponse == null
                || aiResponse.getRecommendations() == null) {

            throw new AiOutfitApiException(
                    "AI 배치 옷차림 추천 응답 형식이 올바르지 않습니다."
            );
        }

        Map<String, AiOutfitRecommendationResponse>
                orderedRecommendations = new LinkedHashMap<>();

        for (TravelStyle travelStyle : TravelStyle.values()) {
            String styleName = travelStyle.getDisplayName();

            AiOutfitRecommendationResponse recommendation =
                    aiResponse.getRecommendations().get(styleName);

            if (recommendation == null) {
                throw new AiOutfitApiException(
                        "AI 배치 추천 결과에 "
                                + styleName
                                + " 항목이 없습니다."
                );
            }

            validateAiResponse(recommendation);

            orderedRecommendations.put(
                    styleName,
                    recommendation
            );
        }

        return orderedRecommendations;
    }
}
