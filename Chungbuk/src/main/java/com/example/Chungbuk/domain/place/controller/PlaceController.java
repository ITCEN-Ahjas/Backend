package com.example.Chungbuk.domain.place.controller;

import com.example.Chungbuk.domain.place.constant.PlaceCategory;
import com.example.Chungbuk.domain.place.dto.response.PlaceDetailResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
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

    @Operation(
            summary = "장소 사진 조회",
            description = "Google Places 사진 리소스명(photoName)을 이용해 장소 사진 이미지를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "장소 사진 조회 성공",
                    content = @Content(
                            mediaType = "image/*",
                            schema = @Schema(type = "string", format = "binary")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 사진 리소스명"
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Google Places 사진 API 통신 실패"
            )
    })
    @GetMapping("/photo")
    public ResponseEntity<byte[]> getPlacePhoto(
            @Parameter(
                    description = "Google Places 사진 리소스명",
                    example = "places/ChIJU01inBL0YzURDZHdPYSkIBw/photos/AaVG..."
            )
            @RequestParam("name")
            String name,

            @Parameter(description = "반환받을 이미지 최대 너비", example = "320")
            @RequestParam(defaultValue = "320")
            int maxWidthPx
    ) {
        validatePhotoName(name);
        return placeSearchService.getPhotoMedia(name, maxWidthPx);
    }

    @Operation(
            summary = "장소 상세 조회",
            description = "검색 결과의 placeId를 이용해 상세 페이지 구성에 필요한 장소 정보와 리뷰 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "장소 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = PlaceDetailResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 장소 ID"
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Google Places 상세 API 통신 실패"
            )
    })
    @GetMapping("/{placeId}")
    public PlaceDetailResponse getPlaceDetail(
            @Parameter(
                    description = "Google Places 장소 ID",
                    example = "ChIJU01inBL0YzURDZHdPYSkIBw"
            )
            @PathVariable
            String placeId
    ) {
        validatePlaceId(placeId);
        return placeSearchService.getPlaceDetail(placeId);
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

    private void validatePlaceId(String placeId) {
        if (placeId == null || placeId.isBlank()) {
            throw new InvalidRequestException("장소 ID가 비어 있습니다.");
        }
    }
}
