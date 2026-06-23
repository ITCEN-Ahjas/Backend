package com.example.Chungbuk.domain.camping.service;

import com.example.Chungbuk.domain.camping.client.GoCampingApiClient;
import com.example.Chungbuk.domain.camping.constant.CampingSyncPauseReason;
import com.example.Chungbuk.domain.camping.entity.CampingEntity;
import com.example.Chungbuk.domain.camping.mapper.CampingMapper;
import com.example.Chungbuk.domain.camping.repository.CampingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampingInitialSyncWorker {

    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 50;

    private final GoCampingApiClient goCampingApiClient;
    private final CampingMapper campingMapper;
    private final CampingRepository campingRepository;
    private final CampingSyncStateService campingSyncStateService;
    private final CampingInitialSyncRunGuard campingInitialSyncRunGuard;

    @Async("campingInitialSyncExecutor")
    public void runSync() {
        try {
            long savedCount = runListSync();
            campingSyncStateService.completeSync(savedCount);

            log.info("캠핑장 배치 수집 완료. savedCount={}", savedCount);

        } catch (RestClientException e) {
            campingSyncStateService.markWaiting(
                    CampingSyncPauseReason.API_ERROR,
                    "고캠핑 API 요청 중 오류: " + safeMessage(e)
            );
            log.warn("캠핑장 배치 수집이 API 오류로 중단되었습니다.", e);

        } catch (Exception e) {
            campingSyncStateService.markFailed("배치 수집 중 오류: " + safeMessage(e));
            log.error("캠핑장 배치 수집 중 처리되지 않은 오류가 발생했습니다.", e);

        } finally {
            campingInitialSyncRunGuard.finish();
        }
    }

    private long runListSync() {
        long totalSaved = 0;

        for (int page = 1; page <= MAX_PAGES; page++) {
            String rawJson = goCampingApiClient.getBasedListRaw(page, PAGE_SIZE);

            List<JsonNode> items = campingMapper.extractItems(rawJson);

            if (items.isEmpty()) {
                break;
            }

            for (JsonNode item : items) {
                CampingEntity entity = campingMapper.toEntity(item);

                if (hasText(entity.getContentId())) {
                    campingRepository.save(entity);
                    totalSaved++;
                }
            }

            log.info("캠핑장 목록 수집 중. page={}, items={}, totalSaved={}", page, items.size(), totalSaved);

            if (items.size() < PAGE_SIZE) {
                break;
            }
        }

        return totalSaved;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safeMessage(Exception e) {
        if (e.getMessage() == null || e.getMessage().isBlank()) {
            return e.getClass().getSimpleName();
        }
        return e.getMessage();
    }
}
