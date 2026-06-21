package com.example.Chungbuk.domain.place.service;

import com.example.Chungbuk.domain.place.client.GooglePlacesClient;
import com.example.Chungbuk.domain.place.constant.PlaceCategory;
import com.example.Chungbuk.domain.place.dto.google.response.GooglePlaceDetailResponse;
import com.example.Chungbuk.domain.place.dto.google.request.GooglePlacesTextSearchRequest;
import com.example.Chungbuk.domain.place.dto.google.response.GooglePlacesTextSearchResponse;
import com.example.Chungbuk.domain.place.dto.response.PlaceDetailResponse;
import com.example.Chungbuk.domain.place.dto.response.PlaceReviewResponse;
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

    public PlaceDetailResponse getPlaceDetail(String placeId) {
        return toPlaceDetail(googlePlacesClient.getPlaceDetail(placeId));
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

    private PlaceDetailResponse toPlaceDetail(GooglePlaceDetailResponse detail) {
        if (detail == null) {
            return PlaceDetailResponse.builder()
                    .types(Collections.emptyList())
                    .photoNames(Collections.emptyList())
                    .weekdayDescriptions(Collections.emptyList())
                    .reviews(Collections.emptyList())
                    .build();
        }

        List<String> photoNames = getPhotoNames(detail.getPhotos());

        return PlaceDetailResponse.builder()
                .placeId(detail.getId())
                .name(getLocalizedText(detail.getDisplayName()))
                .address(detail.getFormattedAddress())
                .latitude(getLatitude(detail.getLocation()))
                .longitude(getLongitude(detail.getLocation()))
                .primaryType(detail.getPrimaryType())
                .primaryTypeName(getLocalizedText(detail.getPrimaryTypeDisplayName()))
                .types(detail.getTypes() == null ? Collections.emptyList() : detail.getTypes())
                .rating(detail.getRating())
                .userRatingCount(detail.getUserRatingCount())
                .photoNames(photoNames)
                .photoName(photoNames.isEmpty() ? null : photoNames.getFirst())
                .googleMapsUri(detail.getGoogleMapsUri())
                .nationalPhoneNumber(detail.getNationalPhoneNumber())
                .internationalPhoneNumber(detail.getInternationalPhoneNumber())
                .websiteUri(detail.getWebsiteUri())
                .weekdayDescriptions(getWeekdayDescriptions(detail.getRegularOpeningHours()))
                .summary(getLocalizedText(detail.getEditorialSummary()))
                .reviews(getReviews(detail.getReviews()))
                .build();
    }

    private List<String> getPhotoNames(List<GooglePlacesTextSearchResponse.Photo> photos) {
        if (photos == null) {
            return Collections.emptyList();
        }

        return photos.stream()
                .map(GooglePlacesTextSearchResponse.Photo::getName)
                .filter(name -> name != null && !name.isBlank())
                .toList();
    }

    private List<String> getWeekdayDescriptions(GooglePlaceDetailResponse.OpeningHours openingHours) {
        if (openingHours == null || openingHours.getWeekdayDescriptions() == null) {
            return Collections.emptyList();
        }

        return openingHours.getWeekdayDescriptions();
    }

    private List<PlaceReviewResponse> getReviews(List<GooglePlaceDetailResponse.Review> reviews) {
        if (reviews == null) {
            return Collections.emptyList();
        }

        return reviews.stream()
                .map(this::toPlaceReview)
                .toList();
    }

    private PlaceReviewResponse toPlaceReview(GooglePlaceDetailResponse.Review review) {
        GooglePlacesTextSearchResponse.LocalizedText text = review.getText();
        GooglePlacesTextSearchResponse.LocalizedText originalText = review.getOriginalText();
        GooglePlaceDetailResponse.AuthorAttribution author = review.getAuthorAttribution();

        return PlaceReviewResponse.builder()
                .reviewId(review.getName())
                .authorName(author == null ? null : author.getDisplayName())
                .authorUri(author == null ? null : author.getUri())
                .authorPhotoUri(author == null ? null : author.getPhotoUri())
                .rating(review.getRating())
                .text(getLocalizedText(text))
                .originalText(getLocalizedText(originalText))
                .languageCode(text == null ? null : text.getLanguageCode())
                .relativePublishTimeDescription(review.getRelativePublishTimeDescription())
                .publishTime(review.getPublishTime())
                .build();
    }

    private Double getLatitude(GooglePlacesTextSearchResponse.LatLng location) {
        return location == null ? null : location.getLatitude();
    }

    private Double getLongitude(GooglePlacesTextSearchResponse.LatLng location) {
        return location == null ? null : location.getLongitude();
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
