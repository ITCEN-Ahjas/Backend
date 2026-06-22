package com.example.Chungbuk.domain.weather.service;

import com.example.Chungbuk.domain.weather.client.AiOutfitRecommendationClient;
import com.example.Chungbuk.domain.weather.constant.TravelStyle;
import com.example.Chungbuk.domain.weather.dto.request.AiOutfitRecommendationRequest;
import com.example.Chungbuk.domain.weather.dto.request.OutfitRecommendationRequest;
import com.example.Chungbuk.domain.weather.dto.request.RegionWeatherRequest;
import com.example.Chungbuk.domain.weather.dto.response.AiOutfitRecommendationResponse;
import com.example.Chungbuk.domain.weather.dto.response.CurrentWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.FeelsLikeWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.RegionOutfitRecommendationResponse;
import com.example.Chungbuk.domain.weather.dto.response.WeatherPageResponse;
import com.example.Chungbuk.global.exception.AiOutfitApiException;
import com.example.Chungbuk.global.exception.InvalidRequestException;
import org.springframework.stereotype.Service;

@Service
public class OutfitRecommendationService {

    private final WeatherService weatherService;
    private final AiOutfitRecommendationClient
            aiOutfitRecommendationClient;

    public OutfitRecommendationService(
            WeatherService weatherService,
            AiOutfitRecommendationClient aiOutfitRecommendationClient
    ) {
        this.weatherService = weatherService;
        this.aiOutfitRecommendationClient =
                aiOutfitRecommendationClient;
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
        CurrentWeatherResponse currentWeather =
                weatherPageResponse.getCurrentWeather();

        FeelsLikeWeatherResponse feelsLikeWeather =
                weatherPageResponse.getFeelsLikeWeather();

        return new AiOutfitRecommendationRequest(
                weatherPageResponse.getRegion(),
                travelStyle.getDisplayName(),
                new AiOutfitRecommendationRequest.CurrentWeather(
                        currentWeather.getTemperature(),
                        currentWeather.getHumidity(),
                        currentWeather.getWindSpeed(),
                        currentWeather.getWindStatus(),
                        currentWeather.getPrecipitationAmount(),
                        currentWeather.getPrecipitationType(),
                        currentWeather.getPrecipitationProbability(),
                        currentWeather.getSkyStatus(),
                        currentWeather.getWeatherCondition()
                ),
                new AiOutfitRecommendationRequest.FeelsLikeWeather(
                        feelsLikeWeather.getFeelsLikeTemperature(),
                        feelsLikeWeather.getTemperatureDifference(),
                        feelsLikeWeather.getDescription(),
                        feelsLikeWeather.getFactors()
                )
        );
    }

    private void validateRegion(String region) {
        if (region == null || region.isBlank()) {
            throw new InvalidRequestException(
                    "지역을 선택해 주세요."
            );
        }
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
}