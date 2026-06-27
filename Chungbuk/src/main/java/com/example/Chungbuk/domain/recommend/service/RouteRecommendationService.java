package com.example.Chungbuk.domain.recommend.service;

import com.example.Chungbuk.domain.recommend.client.AiRouteRecommendationClient;
import com.example.Chungbuk.domain.recommend.dto.ai.request.AiRouteRecommendationRequest;
import com.example.Chungbuk.domain.recommend.dto.ai.response.AiRouteRecommendationResponse;
import com.example.Chungbuk.domain.recommend.dto.request.RouteRecommendationRequest;
import com.example.Chungbuk.domain.recommend.dto.response.RouteRecommendationResponse;
import com.example.Chungbuk.domain.recommend.mapper.RouteRecommendationResponseMapper;
import com.example.Chungbuk.global.exception.AiRouteRecommendationApiException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RouteRecommendationService {

    private final RouteRecommendationDataAssembler dataAssembler;
    private final AiRouteRecommendationClient aiRouteRecommendationClient;
    private final RouteRecommendationResponseMapper responseMapper;
    private final RouteRecommendationFallbackService fallbackService;

    public RouteRecommendationResponse recommend(
            RouteRecommendationRequest request
    ) {
        AiRouteRecommendationRequest aiRequest =
                dataAssembler.assemble(request);

        try {
            AiRouteRecommendationResponse aiResponse =
                    aiRouteRecommendationClient.recommend(aiRequest);
            validateAiResponse(aiResponse);

            return responseMapper.toFrontendResponse(
                    aiResponse,
                    aiRequest.getCandidatePlaces()
            );
        } catch (AiRouteRecommendationApiException exception) {
            return fallbackService.createFallbackResponse(aiRequest);
        }
    }

    private void validateAiResponse(
            AiRouteRecommendationResponse aiResponse
    ) {
        if (aiResponse == null) {
            throw new AiRouteRecommendationApiException(
                    "AI route recommendation response is empty."
            );
        }

        List<AiRouteRecommendationResponse.RoutePlace> itinerary =
                aiResponse.getItinerary();
        if (itinerary == null || itinerary.isEmpty()) {
            throw new AiRouteRecommendationApiException(
                    "AI route recommendation itinerary is empty."
            );
        }

        if (itinerary.stream().anyMatch(place -> place == null)) {
            throw new AiRouteRecommendationApiException(
                    "AI route recommendation itinerary contains invalid place."
            );
        }
    }
}
