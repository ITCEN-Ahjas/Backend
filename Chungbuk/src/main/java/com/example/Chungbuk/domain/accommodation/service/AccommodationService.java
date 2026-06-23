package com.example.Chungbuk.domain.accommodation.service;

import com.example.Chungbuk.domain.accommodation.client.AccommodationApiClient;
import com.example.Chungbuk.domain.accommodation.dto.response.AccommodationDetailResponse;
import com.example.Chungbuk.domain.accommodation.dto.response.AccommodationListResponse;
import com.example.Chungbuk.domain.accommodation.dto.response.AccommodationSummaryResponse;
import com.example.Chungbuk.domain.accommodation.dto.response.RoomInfoResponse;
import com.example.Chungbuk.domain.accommodation.entity.AccommodationEntity;
import com.example.Chungbuk.domain.accommodation.entity.AccommodationRoomEntity;
import com.example.Chungbuk.domain.accommodation.mapper.AccommodationMapper;
import com.example.Chungbuk.domain.accommodation.repository.AccommodationRepository;
import com.example.Chungbuk.domain.festival.constant.ChungbukRegion;
import com.example.Chungbuk.global.config.CacheConfig;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccommodationService {

    private final AccommodationApiClient accommodationApiClient;
    private final AccommodationMapper accommodationMapper;
    private final AccommodationRepository accommodationRepository;

    private static final int DEFAULT_PAGE = 1;

    @Cacheable(
            value = CacheConfig.ACCOMMODATION_LIST_CACHE,
            key = "'page=' + #page"
                    + " + ':size=' + #size"
                    + " + ':region=' + (#region == null ? '' : #region)"
                    + " + ':category=' + (#category == null ? '' : #category)"
                    + " + ':keyword=' + (#keyword == null ? '' : #keyword)"
    )
    public AccommodationListResponse getAccommodations(
            int page,
            int size,
            String region,
            String category,
            String keyword
    ) {
        int validPage = normalizePage(page);
        int validSize = normalizeSize(size);
        String sigunguCode = resolveSigunguCode(region);

        String rawJson = accommodationApiClient.getAccommodationListRaw(
                validPage,
                validSize,
                sigunguCode
        );

        AccommodationListResponse response = accommodationMapper.toAccommodationListResponse(
                rawJson,
                validPage,
                validSize
        );

        List<AccommodationSummaryResponse> filteredItems = response.getItems().stream()
                .filter(item -> matchesCategory(item.getCategory(), category))
                .filter(item -> matchesKeyword(item, keyword))
                .toList();

        return AccommodationListResponse.builder()
                .items(filteredItems)
                .page(validPage)
                .size(validSize)
                .totalCount(filteredItems.size())
                .build();
    }

    @Cacheable(
            value = CacheConfig.ACCOMMODATION_DETAIL_CACHE,
            key = "#contentId == null ? '' : #contentId"
    )
    @Transactional
    public AccommodationDetailResponse getAccommodationDetail(String contentId) {
        String validContentId = safe(contentId);

        AccommodationEntity entity = accommodationRepository.findById(validContentId).orElse(null);

        if (entity != null && entity.isDetailSynced()) {
            return accommodationMapper.toAccommodationDetailResponse(entity);
        }

        return fetchAndPersistAccommodationDetail(validContentId, entity);
    }

    @Transactional
    public void syncAccommodationDetail(String contentId) {
        AccommodationEntity entity = accommodationRepository.findById(contentId).orElse(null);

        if (entity != null && entity.isDetailSynced()) {
            return;
        }

        fetchAndPersistAccommodationDetail(contentId, entity);
    }

    public String getAccommodationRaw(int page, int size, String region) {
        int validPage = normalizePage(page);
        int validSize = normalizeSize(size);
        String sigunguCode = resolveSigunguCode(region);

        return accommodationApiClient.getAccommodationListRaw(
                validPage,
                validSize,
                sigunguCode
        );
    }

    private AccommodationDetailResponse fetchAndPersistAccommodationDetail(
            String contentId,
            AccommodationEntity existingEntity
    ) {
        String detailCommonRawJson = accommodationApiClient.getAccommodationDetailCommonRaw(contentId);
        String detailIntroRawJson = accommodationApiClient.getAccommodationDetailIntroRaw(contentId);
        String detailImageRawJson = accommodationApiClient.getAccommodationDetailImageRaw(contentId);
        String roomInfoRawJson = accommodationApiClient.getAccommodationRoomInfoRaw(contentId);

        AccommodationDetailResponse response = accommodationMapper.toAccommodationDetailResponse(
                detailCommonRawJson,
                detailIntroRawJson,
                detailImageRawJson,
                roomInfoRawJson,
                contentId
        );

        if (!hasText(response.getId()) || !hasText(response.getTitle())) {
            return response;
        }

        if (existingEntity != null) {
            existingEntity.updateDetailFields(
                    response.getCat1(),
                    response.getCat2(),
                    response.getCat3(),
                    response.getTitle(),
                    response.getRegion(),
                    response.getCategory(),
                    response.getAddress(),
                    response.getImageUrl(),
                    response.getImageUrls(),
                    response.getTel(),
                    response.getHomepage(),
                    response.getOverview(),
                    response.getDescription(),
                    response.getDescriptionSource(),
                    response.getMapX(),
                    response.getMapY(),
                    response.getCheckInTime(),
                    response.getCheckOutTime(),
                    response.getParking(),
                    response.getCookingAvailable(),
                    response.getRoomCount(),
                    response.getInfoCenter()
            );
            existingEntity.replaceRooms(buildRoomEntities(response.getRooms()));
        } else {
            AccommodationEntity newEntity = accommodationMapper.toEntity(response);
            newEntity.markDetailSynced();
            accommodationRepository.save(newEntity);
        }

        return response;
    }

    private List<AccommodationRoomEntity> buildRoomEntities(List<RoomInfoResponse> rooms) {
        if (rooms == null) {
            return List.of();
        }
        return rooms.stream()
                .map(r -> AccommodationRoomEntity.builder()
                        .roomTitle(r.getRoomTitle())
                        .roomSize(r.getRoomSize())
                        .roomCount(r.getRoomCount())
                        .baseCount(r.getBaseCount())
                        .maxCount(r.getMaxCount())
                        .offSeasonMinFee(r.getOffSeasonMinFee())
                        .offSeasonMaxFee(r.getOffSeasonMaxFee())
                        .peakSeasonMinFee(r.getPeakSeasonMinFee())
                        .peakSeasonMaxFee(r.getPeakSeasonMaxFee())
                        .roomImageUrl(r.getRoomImageUrl())
                        .bathFacility(r.getBathFacility())
                        .internet(r.getInternet())
                        .airCondition(r.getAirCondition())
                        .build())
                .toList();
    }

    private boolean matchesCategory(String itemCategory, String requestedCategory) {
        if (!hasText(requestedCategory) || "전체".equals(requestedCategory.trim())) {
            return true;
        }
        return requestedCategory.trim().equals(itemCategory);
    }

    private boolean matchesKeyword(AccommodationSummaryResponse item, String keyword) {
        if (!hasText(keyword)) {
            return true;
        }
        String normalizedKeyword = normalizeForSearch(keyword);
        String searchTarget = normalizeForSearch(
                item.getTitle() + " "
                        + item.getRegion() + " "
                        + item.getCategory() + " "
                        + item.getAddress()
        );
        return searchTarget.contains(normalizedKeyword);
    }

    private int normalizePage(int page) {
        return page < 1 ? DEFAULT_PAGE : page;
    }

    private int normalizeSize(int size) {
        if (size < 1) return 10;
        if (size > 100) return 100;
        return size;
    }

    private String resolveSigunguCode(String region) {
        return ChungbukRegion.findSigunguCodeByName(region);
    }

    private String normalizeForSearch(String value) {
        return safe(value).replace(" ", "").toLowerCase();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
