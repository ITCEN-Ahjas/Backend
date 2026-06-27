package com.example.Chungbuk.domain.recommend.client;

import com.example.Chungbuk.domain.recommend.dto.ai.request.AiRouteRecommendationRequest;
import com.example.Chungbuk.domain.recommend.dto.ai.response.AiRouteRecommendationResponse;
import com.example.Chungbuk.global.config.AiRouteRecommendationProperties;
import com.example.Chungbuk.global.exception.AiRouteRecommendationApiException;
import java.net.URI;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class AiRouteRecommendationClient {

    private final RestTemplate aiRouteRestTemplate;
    private final AiRouteRecommendationProperties properties;

    public AiRouteRecommendationClient(
            @Qualifier("aiRouteRestTemplate")
            RestTemplate aiRouteRestTemplate,
            AiRouteRecommendationProperties properties
    ) {
        this.aiRouteRestTemplate = aiRouteRestTemplate;
        this.properties = properties;
    }

    public AiRouteRecommendationResponse recommend(
            AiRouteRecommendationRequest request
    ) {
        properties.validateBaseUrl();

        URI requestUri = URI.create(properties.getRoutesUrl());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AiRouteRecommendationRequest> requestEntity =
                new HttpEntity<>(request, headers);

        try {
            ResponseEntity<AiRouteRecommendationResponse> responseEntity =
                    aiRouteRestTemplate.exchange(
                            requestUri,
                            HttpMethod.POST,
                            requestEntity,
                            AiRouteRecommendationResponse.class
                    );

            return extractResponse(responseEntity);
        } catch (RestClientException exception) {
            throw new AiRouteRecommendationApiException(
                    "AI route recommendation server is not available.",
                    exception
            );
        }
    }

    private AiRouteRecommendationResponse extractResponse(
            ResponseEntity<AiRouteRecommendationResponse> responseEntity
    ) {
        if (responseEntity == null || responseEntity.getBody() == null) {
            throw new AiRouteRecommendationApiException(
                    "AI route recommendation response is empty."
            );
        }

        return responseEntity.getBody();
    }
}
