package com.example.Chungbuk.domain.weather.client;

import com.example.Chungbuk.domain.weather.dto.request.AiOutfitRecommendationRequest;
import com.example.Chungbuk.domain.weather.dto.response.AiOutfitRecommendationResponse;
import com.example.Chungbuk.global.config.AiOutfitApiProperties;
import com.example.Chungbuk.global.exception.AiOutfitApiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Component
public class AiOutfitRecommendationClient {

    private final RestTemplate aiOutfitRestTemplate;
    private final AiOutfitApiProperties aiOutfitApiProperties;

    public AiOutfitRecommendationClient(
            @Qualifier("aiOutfitRestTemplate")
            RestTemplate aiOutfitRestTemplate,
            AiOutfitApiProperties aiOutfitApiProperties
    ) {
        this.aiOutfitRestTemplate = aiOutfitRestTemplate;
        this.aiOutfitApiProperties = aiOutfitApiProperties;
    }

    public AiOutfitRecommendationResponse recommend(
            AiOutfitRecommendationRequest request
    ) {
        aiOutfitApiProperties.validateBaseUrl();

        URI requestUri = URI.create(
                aiOutfitApiProperties.getRecommendUrl()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AiOutfitRecommendationRequest> requestEntity =
                new HttpEntity<>(request, headers);

        try {
            ResponseEntity<AiOutfitRecommendationResponse> responseEntity =
                    aiOutfitRestTemplate.exchange(
                            requestUri,
                            HttpMethod.POST,
                            requestEntity,
                            AiOutfitRecommendationResponse.class
                    );

            return extractResponse(responseEntity);
        } catch (RestClientException exception) {
            throw new AiOutfitApiException(
                    "AI 옷차림 추천 서버에 연결할 수 없습니다.",
                    exception
            );
        }
    }

    private AiOutfitRecommendationResponse extractResponse(
            ResponseEntity<AiOutfitRecommendationResponse> responseEntity
    ) {
        if (responseEntity == null || responseEntity.getBody() == null) {
            throw new AiOutfitApiException(
                    "AI 옷차림 추천 서버의 응답이 비어 있습니다."
            );
        }

        return responseEntity.getBody();
    }
}