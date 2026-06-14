package com.example.Chungbuk.domain.festival.mapper;

import com.example.Chungbuk.domain.festival.dto.response.FestivalListResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalSummaryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class FestivalMapper {

    private static final DateTimeFormatter TOUR_API_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ObjectMapper objectMapper = new ObjectMapper();

    public FestivalListResponse toFestivalListResponse(
            String rawJson,
            int page,
            int size
    ) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode body = root.path("response").path("body");

            int totalCount = body.path("totalCount").asInt(0);
            List<FestivalSummaryResponse> items = extractItems(body);

            return FestivalListResponse.builder()
                    .items(items)
                    .page(page)
                    .size(size)
                    .totalCount(totalCount)
                    .build();

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("TourAPI 응답 파싱에 실패했습니다.", e);
        }
    }

    private List<FestivalSummaryResponse> extractItems(JsonNode body) {
        List<FestivalSummaryResponse> result = new ArrayList<>();

        JsonNode itemNode = body.path("items").path("item");

        if (itemNode.isMissingNode() || itemNode.isNull()) {
            return result;
        }

        if (itemNode.isArray()) {
            for (JsonNode item : itemNode) {
                result.add(toSummaryResponse(item));
            }
            return result;
        }

        if (itemNode.isObject()) {
            result.add(toSummaryResponse(itemNode));
        }

        return result;
    }

    private FestivalSummaryResponse toSummaryResponse(JsonNode item) {
        String title = getText(item, "title");
        String address = buildAddress(
                getText(item, "addr1"),
                getText(item, "addr2")
        );
        String startDate = getText(item, "eventstartdate");
        String endDate = getText(item, "eventenddate");

        return FestivalSummaryResponse.builder()
                .id(getText(item, "contentid"))
                .title(title)
                .region(extractRegion(address))
                .category(resolveCategory(title, address))
                .status(resolveStatus(startDate, endDate))
                .startDate(startDate)
                .endDate(endDate)
                .address(address)
                .imageUrl(resolveImageUrl(item))
                .tel(getText(item, "tel"))
                .mapX(getText(item, "mapx"))
                .mapY(getText(item, "mapy"))
                .build();
    }

    private String getText(JsonNode item, String fieldName) {
        return item.path(fieldName).asText("");
    }

    private String buildAddress(String addr1, String addr2) {
        if (addr1.isBlank()) {
            return "";
        }

        if (addr2.isBlank()) {
            return addr1;
        }

        return addr1 + " " + addr2;
    }

    private String resolveImageUrl(JsonNode item) {
        String firstImage = getText(item, "firstimage");

        if (!firstImage.isBlank()) {
            return firstImage;
        }

        return getText(item, "firstimage2");
    }

    private String extractRegion(String address) {
        if (address.contains("청주")) return "청주";
        if (address.contains("충주")) return "충주";
        if (address.contains("제천")) return "제천";
        if (address.contains("단양")) return "단양";
        if (address.contains("보은")) return "보은";
        if (address.contains("영동")) return "영동";
        if (address.contains("옥천")) return "옥천";
        if (address.contains("괴산")) return "괴산";
        if (address.contains("진천")) return "진천";
        if (address.contains("음성")) return "음성";
        if (address.contains("증평")) return "증평";

        return "충북";
    }

    private String resolveCategory(String title, String address) {
        String text = title + " " + address;

        if (containsAny(text, "와인", "대추", "음식", "먹거리", "푸드", "시장")) {
            return "먹거리";
        }

        if (containsAny(text, "야행", "야시장", "밤", "라이트", "불빛")) {
            return "야간행사";
        }

        if (containsAny(text, "체험", "농촌", "자연", "숲", "생태")) {
            return "자연체험";
        }

        if (containsAny(text, "레포츠", "스포츠", "패러글라이딩", "액티비티")) {
            return "액티비티";
        }

        if (containsAny(text, "전통시장", "시장")) {
            return "전통시장";
        }

        return "문화축제";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private String resolveStatus(String startDate, String endDate) {
        if (startDate.isBlank() || endDate.isBlank()) {
            return "정보 없음";
        }

        try {
            LocalDate today = LocalDate.now();
            LocalDate start = LocalDate.parse(startDate, TOUR_API_DATE_FORMAT);
            LocalDate end = LocalDate.parse(endDate, TOUR_API_DATE_FORMAT);

            if (today.isBefore(start)) {
                return "예정";
            }

            if (!today.isBefore(start) && !today.isAfter(end)) {
                return "진행 중";
            }

            return "종료";

        } catch (DateTimeParseException e) {
            return "정보 없음";
        }
    }
}