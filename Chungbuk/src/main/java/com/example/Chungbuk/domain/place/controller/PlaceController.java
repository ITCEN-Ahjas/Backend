package com.example.Chungbuk.domain.place.controller;

import com.example.Chungbuk.domain.place.constant.PlaceCategory;
import com.example.Chungbuk.domain.place.dto.response.PlaceSearchResponse;
import com.example.Chungbuk.domain.place.service.PlaceSearchService;
import com.example.Chungbuk.global.exception.InvalidRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
@Tag(name = "Place", description = "충청북도 지도 장소 검색 API")
public class PlaceController {

    private final PlaceSearchService placeSearchService;

    @Operation(
            summary = "지도 장소 검색",
            description = "키워드와 카테고리로 충청북도 내 장소를 검색합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "장소 검색 성공",
                    content = @Content(schema = @Schema(implementation = PlaceSearchResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 카테고리 또는 검색 결과 개수",
                    content = @Content(
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "status": 400,
                                              "code": "INVALID_REQUEST",
                                              "message": "요청값이 올바르지 않습니다.",
                                              "path": "/api/places"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Google Places API 통신 실패"
            )
    })
    @GetMapping
    public PlaceSearchResponse searchPlaces(
            @Parameter(description = "장소명 또는 검색어", example = "청남대")
            @RequestParam(required = false)
            String keyword,

            @Parameter(
                    description = "장소 카테고리",
                    example = "TOURIST_ATTRACTION",
                    schema = @Schema(allowableValues = {
                            "ALL",
                            "TOURIST_ATTRACTION",
                            "RESTAURANT",
                            "SHOPPING"
                    })
            )
            @RequestParam(defaultValue = "ALL")
            PlaceCategory category,

            @Parameter(description = "검색 결과 개수", example = "10")
            @RequestParam(defaultValue = "10")
            int size,

            @Parameter(description = "다음 페이지 조회 토큰")
            @RequestParam(required = false)
            String pageToken
    ) {
        validateSize(size);
        return placeSearchService.search(keyword, category, size, pageToken);
    }

    @GetMapping("/photo")
    public ResponseEntity<byte[]> getPlacePhoto(
            @RequestParam("name")
            String name,

            @RequestParam(defaultValue = "320")
            int maxWidthPx
    ) {
        validatePhotoName(name);
        return placeSearchService.getPhotoMedia(name, maxWidthPx);
    }

    private void validateSize(int size) {
        if (size < 1 || size > 20) {
            throw new InvalidRequestException("size는 1 이상 20 이하여야 합니다.");
        }
    }

    private void validatePhotoName(String name) {
        if (name == null || name.isBlank() || !name.startsWith("places/")) {
            throw new InvalidRequestException("올바른 장소 사진 리소스명이 아닙니다.");
        }
    }
}
