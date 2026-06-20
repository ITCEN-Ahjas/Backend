package com.example.Chungbuk.domain.place.service;

import com.example.Chungbuk.domain.place.client.GooglePlacesClient;
import com.example.Chungbuk.domain.place.constant.PlaceCategory;
import com.example.Chungbuk.domain.place.dto.google.request.GooglePlacesTextSearchRequest;
import com.example.Chungbuk.domain.place.dto.google.response.GooglePlacesTextSearchResponse;
import com.example.Chungbuk.domain.place.dto.response.PlaceSearchResponse;
import com.example.Chungbuk.domain.place.dto.response.PlaceSummaryResponse;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaceSearchService {

    private static final String CHUNGBUK_NAME = "충청북도";
    private static final String DEFAULT_LANGUAGE_CODE = "ko";
    private static final String DEFAULT_REGION_CODE = "KR";
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 20;
    private static final int DEFAULT_PHOTO_WIDTH = 320;
    private static final int MIN_PHOTO_WIDTH = 100;
    private static final int MAX_PHOTO_WIDTH = 1200;

    private static final double CHUNGBUK_SOUTH_LATITUDE = 35.97;
    private static final double CHUNGBUK_WEST_LONGITUDE = 127.27;
    private static final double CHUNGBUK_NORTH_LATITUDE = 37.25;
    private static final double CHUNGBUK_EAST_LONGITUDE = 128.65;

    private final GooglePlacesClient googlePlacesClient;

    public PlaceSearchResponse search(
            String keyword,
            PlaceCategory category,
            Integer size,
            String pageToken
    ) {
        PlaceCategory resolvedCategory = category == null ? PlaceCategory.ALL : category;
        int resolvedSize = resolveSize(size);

        GooglePlacesTextSearchResponse googleResponse = googlePlacesClient.searchText(
                createSearchRequest(keyword, resolvedCategory, resolvedSize, pageToken)
        );

        if (googleResponse == null || googleResponse.getPlaces() == null) {
            return PlaceSearchResponse.builder()
                    .items(Collections.emptyList())
                    .size(0)
                    .nextPageToken(null)
                    .build();
        }

        List<PlaceSummaryResponse> items = googleResponse.getPlaces().stream()
                .filter(place -> place != null && place.getLocation() != null)
                .map(place -> toPlaceSummary(place, resolvedCategory))
                .toList();

        return PlaceSearchResponse.builder()
                .items(items)
                .size(items.size())
                .nextPageToken(googleResponse.getNextPageToken())
                .build();
    }

    public ResponseEntity<byte[]> getPhotoMedia(String photoName, Integer maxWidthPx) {
        return googlePlacesClient.getPhotoMedia(
                photoName,
                resolvePhotoWidth(maxWidthPx)
        );
    }

    private GooglePlacesTextSearchRequest createSearchRequest(
            String keyword,
            PlaceCategory category,
            int size,
            String pageToken
    ) {
        return GooglePlacesTextSearchRequest.builder()
                .textQuery(createTextQuery(keyword, category))
                .includedType(category.getGoogleIncludedType())
                .strictTypeFiltering(false)
                .languageCode(DEFAULT_LANGUAGE_CODE)
                .regionCode(DEFAULT_REGION_CODE)
                .pageSize(size)
                .pageToken(normalizeNullable(pageToken))
                .locationRestriction(createChungbukLocationRestriction())
                .build();
    }

    private String createTextQuery(String keyword, PlaceCategory category) {
        String normalizedKeyword = normalizeNullable(keyword);

        if (normalizedKeyword == null) {
            return CHUNGBUK_NAME + " " + category.getSearchTerm();
        }

        return normalizedKeyword + " " + CHUNGBUK_NAME;
    }

    private GooglePlacesTextSearchRequest.LocationRestriction createChungbukLocationRestriction() {
        GooglePlacesTextSearchRequest.LatLng low = GooglePlacesTextSearchRequest.LatLng.builder()
                .latitude(CHUNGBUK_SOUTH_LATITUDE)
                .longitude(CHUNGBUK_WEST_LONGITUDE)
                .build();
        GooglePlacesTextSearchRequest.LatLng high = GooglePlacesTextSearchRequest.LatLng.builder()
                .latitude(CHUNGBUK_NORTH_LATITUDE)
                .longitude(CHUNGBUK_EAST_LONGITUDE)
                .build();
        GooglePlacesTextSearchRequest.Rectangle rectangle =
                GooglePlacesTextSearchRequest.Rectangle.builder()
                        .low(low)
                        .high(high)
                        .build();

        return GooglePlacesTextSearchRequest.LocationRestriction.builder()
                .rectangle(rectangle)
                .build();
    }

    private PlaceSummaryResponse toPlaceSummary(
            GooglePlacesTextSearchResponse.Place place,
            PlaceCategory category
    ) {
        return PlaceSummaryResponse.builder()
                .placeId(place.getId())
                .name(getLocalizedText(place.getDisplayName()))
                .address(place.getFormattedAddress())
                .latitude(place.getLocation().getLatitude())
                .longitude(place.getLocation().getLongitude())
                .category(category.getDisplayName())
                .primaryType(place.getPrimaryType())
                .primaryTypeName(getLocalizedText(place.getPrimaryTypeDisplayName()))
                .types(place.getTypes() == null ? Collections.emptyList() : place.getTypes())
                .rating(place.getRating())
                .userRatingCount(place.getUserRatingCount())
                .photoName(getFirstPhotoName(place.getPhotos()))
                .googleMapsUri(place.getGoogleMapsUri())
                .build();
    }

    private String getLocalizedText(GooglePlacesTextSearchResponse.LocalizedText localizedText) {
        return localizedText == null ? null : localizedText.getText();
    }

    private String getFirstPhotoName(List<GooglePlacesTextSearchResponse.Photo> photos) {
        if (photos == null || photos.isEmpty() || photos.getFirst() == null) {
            return null;
        }

        return photos.getFirst().getName();
    }

    private int resolveSize(Integer size) {
        if (size == null) {
            return DEFAULT_PAGE_SIZE;
        }

        return Math.max(1, Math.min(size, MAX_PAGE_SIZE));
    }

    private int resolvePhotoWidth(Integer maxWidthPx) {
        if (maxWidthPx == null) {
            return DEFAULT_PHOTO_WIDTH;
        }

        return Math.max(MIN_PHOTO_WIDTH, Math.min(maxWidthPx, MAX_PHOTO_WIDTH));
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
