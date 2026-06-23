package com.example.Chungbuk.domain.weather.client;

import com.example.Chungbuk.domain.weather.dto.request.AiOutfitBatchRecommendationRequest;
import com.example.Chungbuk.domain.weather.dto.request.AiOutfitRecommendationRequest;
import com.example.Chungbuk.domain.weather.dto.response.AiOutfitBatchRecommendationResponse;
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
        return requestAiRecommendation(
                aiOutfitApiProperties.getRecommendUrl(),
                request,
                AiOutfitRecommendationResponse.class
        );
    }

    public AiOutfitBatchRecommendationResponse recommendBatch(
            AiOutfitBatchRecommendationRequest request
    ) {
        return requestAiRecommendation(
                aiOutfitApiProperties.getBatchRecommendUrl(),
                request,
                AiOutfitBatchRecommendationResponse.class
        );
    }

    private <T> T requestAiRecommendation(
            String requestUrl,
            Object requestBody,
            Class<T> responseType
    ) {
        aiOutfitApiProperties.validateBaseUrl();

        URI requestUri = URI.create(requestUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Object> requestEntity =
                new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<T> responseEntity =
                    aiOutfitRestTemplate.exchange(
                            requestUri,
                            HttpMethod.POST,
                            requestEntity,
                            responseType
                    );

            return extractResponse(responseEntity);
        } catch (RestClientException exception) {
            throw new AiOutfitApiException(
                    "AI 옷차림 추천 서버에 연결할 수 없습니다.",
                    exception
            );
        }
    }

    private <T> T extractResponse(
            ResponseEntity<T> responseEntity
    ) {
        if (responseEntity == null || responseEntity.getBody() == null) {
            throw new AiOutfitApiException(
                    "AI 옷차림 추천 서버의 응답이 비어 있습니다."
            );
        }

        return responseEntity.getBody();
    }
}