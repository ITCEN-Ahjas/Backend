package com.example.Chungbuk.domain.accommodation.mapper;

import com.example.Chungbuk.domain.accommodation.constant.AccommodationCategory;
import com.example.Chungbuk.domain.accommodation.dto.response.AccommodationDetailResponse;
import com.example.Chungbuk.domain.accommodation.dto.response.AccommodationListResponse;
import com.example.Chungbuk.domain.accommodation.dto.response.AccommodationSummaryResponse;
import com.example.Chungbuk.domain.accommodation.dto.response.RoomInfoResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AccommodationMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AccommodationListResponse toAccommodationListResponse(String rawJson, int page, int size) {
        try {
            JsonNode body = getBody(rawJson);
            JsonNode itemNode = body.path("items").path("item");

            List<AccommodationSummaryResponse> items = toItemNodeList(itemNode).stream()
                    .map(this::toAccommodationSummaryResponse)
                    .toList();

            return AccommodationListResponse.builder()
                    .items(items)
                    .page(page)
                    .size(size)
                    .totalCount(items.size())
                    .build();
        } catch (Exception e) {
            return AccommodationListResponse.builder()
                    .items(List.of())
                    .page(page)
                    .size(size)
                    .totalCount(0)
                    .build();
        }
    }

    public AccommodationDetailResponse toAccommodationDetailResponse(
            String detailCommonRawJson,
            String detailIntroRawJson,
            String detailImageRawJson,
            String roomInfoRawJson,
            String contentId
    ) {
        try {
            JsonNode commonItem = firstItem(detailCommonRawJson);
            JsonNode introItem = firstItem(detailIntroRawJson);

            String resolvedContentId = firstNonBlank(
                    text(commonItem, "contentid"),
                    contentId
            );

            String cat1 = text(commonItem, "cat1");
            String cat2 = text(commonItem, "cat2");
            String cat3 = text(commonItem, "cat3");

            String title = text(commonItem, "title");
            String address = combineAddress(text(commonItem, "addr1"), text(commonItem, "addr2"));
            String region = extractRegion(address);

            AccommodationCategory categoryEnum = AccommodationCategory.resolve(cat3, title);
            String category = categoryEnum.getDisplayName();

            String imageUrl = firstNonBlank(
                    text(commonItem, "firstimage"),
                    text(commonItem, "firstimage2")
            );

            List<String> imageUrls = extractImageUrls(detailImageRawJson, imageUrl);

            String overview = cleanHtml(text(commonItem, "overview"));

            String description = hasText(overview)
                    ? overview
                    : buildFallbackDescription(title, region, category);

            List<RoomInfoResponse> rooms = toRoomInfoList(roomInfoRawJson);

            return AccommodationDetailResponse.builder()
                    .id(resolvedContentId)
                    .cat1(cat1)
                    .cat2(cat2)
                    .cat3(cat3)
                    .title(title)
                    .region(region)
                    .category(category)
                    .address(address)
                    .imageUrl(imageUrl)
                    .imageUrls(imageUrls)
                    .tel(text(commonItem, "tel"))
                    .homepage(extractUrlFromHtmlOrText(text(commonItem, "homepage")))
                    .overview(overview)
                    .description(description)
                    .descriptionSource(hasText(overview) ? "DETAIL_COMMON_OVERVIEW" : "GENERATED_FROM_CATEGORY")
                    .mapX(text(commonItem, "mapx"))
                    .mapY(text(commonItem, "mapy"))
                    .checkInTime(cleanHtml(text(introItem, "checkintime")))
                    .checkOutTime(cleanHtml(text(introItem, "checkouttime")))
                    .parking(cleanHtml(text(introItem, "parkinglodging")))
                    .cookingAvailable(cleanHtml(text(introItem, "foodplace")))
                    .roomCount(cleanHtml(text(introItem, "roomcount")))
                    .infoCenter(cleanHtml(text(introItem, "infocenterlodging")))
                    .rooms(rooms)
                    .build();
        } catch (Exception e) {
            return AccommodationDetailResponse.builder()
                    .id(contentId)
                    .cat1("")
                    .cat2("")
                    .cat3("")
                    .title("")
                    .region("")
                    .category(AccommodationCategory.ETC.getDisplayName())
                    .address("")
                    .imageUrl("")
                    .imageUrls(List.of())
                    .tel("")
                    .homepage("")
                    .overview("")
                    .description("제공된 상세 설명이 없습니다.")
                    .descriptionSource("EMPTY")
                    .mapX("")
                    .mapY("")
                    .checkInTime("")
                    .checkOutTime("")
                    .parking("")
                    .cookingAvailable("")
                    .roomCount("")
                    .infoCenter("")
                    .rooms(List.of())
                    .build();
        }
    }

    private AccommodationSummaryResponse toAccommodationSummaryResponse(JsonNode item) {
        String cat1 = text(item, "cat1");
        String cat2 = text(item, "cat2");
        String cat3 = text(item, "cat3");

        String title = text(item, "title");
        String address = combineAddress(text(item, "addr1"), text(item, "addr2"));
        String region = extractRegion(address);

        String category = AccommodationCategory.resolve(cat3, title).getDisplayName();

        return AccommodationSummaryResponse.builder()
                .id(text(item, "contentid"))
                .cat1(cat1)
                .cat2(cat2)
                .cat3(cat3)
                .title(title)
                .region(region)
                .category(category)
                .address(address)
                .imageUrl(firstNonBlank(text(item, "firstimage"), text(item, "firstimage2")))
                .tel(text(item, "tel"))
                .mapX(text(item, "mapx"))
                .mapY(text(item, "mapy"))
                .build();
    }

    private List<RoomInfoResponse> toRoomInfoList(String roomInfoRawJson) {
        try {
            JsonNode body = getBody(roomInfoRawJson);
            List<JsonNode> items = toItemNodeList(body.path("items").path("item"));

            return items.stream()
                    .map(item -> RoomInfoResponse.builder()
                            .roomTitle(text(item, "roomtitle"))
                            .roomSize(text(item, "roomsize2"))
                            .roomCount(text(item, "roomcount"))
                            .baseCount(text(item, "roombasecount"))
                            .maxCount(text(item, "roommaxcount"))
                            .offSeasonMinFee(text(item, "roomoffseasonminfee1"))
                            .offSeasonMaxFee(text(item, "roomoffseasonminfee2"))
                            .peakSeasonMinFee(text(item, "roompeakseasonminfee1"))
                            .peakSeasonMaxFee(text(item, "roompeakseasonminfee2"))
                            .roomImageUrl(firstNonBlank(text(item, "roomimg1"), text(item, "roomimg2")))
                            .bathFacility(text(item, "roombathfacility"))
                            .internet(text(item, "roominternet"))
                            .airCondition(text(item, "roomaircondition"))
                            .build())
                    .toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private String buildFallbackDescription(String title, String region, String category) {
        if (!hasText(title)) {
            return "제공된 상세 설명이 없습니다.";
        }

        String resolvedRegion = valueOrDefault(region, "충북");
        String resolvedCategory = valueOrDefault(category, "숙박시설");

        return title + "은(는) " + resolvedRegion + "에서 만날 수 있는 "
                + resolvedCategory + "입니다. 체크인/체크아웃 시간, 객실 정보 등을 확인하고 예약 계획을 세울 수 있습니다.";
    }

    private JsonNode getBody(String rawJson) throws Exception {
        if (!hasText(rawJson)) {
            return objectMapper.createObjectNode();
        }

        JsonNode root = objectMapper.readTree(rawJson);

        JsonNode responseBody = root.path("response").path("body");
        if (!responseBody.isMissingNode() && !responseBody.isNull() && !responseBody.isEmpty()) {
            return responseBody;
        }

        JsonNode directBody = root.path("body");
        if (!directBody.isMissingNode() && !directBody.isNull()) {
            return directBody;
        }

        return objectMapper.createObjectNode();
    }

    private JsonNode firstItem(String rawJson) {
        try {
            JsonNode body = getBody(rawJson);
            List<JsonNode> items = toItemNodeList(body.path("items").path("item"));

            if (items.isEmpty()) {
                return objectMapper.createObjectNode();
            }

            return items.get(0);
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    private List<JsonNode> toItemNodeList(JsonNode itemNode) {
        List<JsonNode> items = new ArrayList<>();

        if (itemNode == null || itemNode.isMissingNode() || itemNode.isNull()) {
            return items;
        }

        if (itemNode.isArray()) {
            itemNode.forEach(items::add);
            return items;
        }

        if (itemNode.isObject()) {
            items.add(itemNode);
        }

        return items;
    }

    private List<String> extractImageUrls(String detailImageRawJson, String imageUrl) {
        Set<String> imageUrlSet = new LinkedHashSet<>();

        if (hasText(imageUrl)) {
            imageUrlSet.add(imageUrl);
        }

        try {
            JsonNode body = getBody(detailImageRawJson);
            List<JsonNode> items = toItemNodeList(body.path("items").path("item"));

            for (JsonNode item : items) {
                String originImageUrl = text(item, "originimgurl");
                String smallImageUrl = text(item, "smallimageurl");

                if (hasText(originImageUrl)) {
                    imageUrlSet.add(originImageUrl);
                }

                if (hasText(smallImageUrl)) {
                    imageUrlSet.add(smallImageUrl);
                }
            }
        } catch (Exception ignored) {
        }

        return new ArrayList<>(imageUrlSet);
    }

    private String combineAddress(String addr1, String addr2) {
        String first = safe(addr1);
        String second = safe(addr2);

        if (!hasText(first)) {
            return second;
        }

        if (!hasText(second)) {
            return first;
        }

        return first + " " + second;
    }

    private String extractRegion(String address) {
        String value = safe(address);

        if (value.contains("청주")) {
            return "청주";
        }

        if (value.contains("충주")) {
            return "충주";
        }

        if (value.contains("제천")) {
            return "제천";
        }

        if (value.contains("단양")) {
            return "단양";
        }

        if (value.contains("보은")) {
            return "보은";
        }

        if (value.contains("영동")) {
            return "영동";
        }

        if (value.contains("옥천")) {
            return "옥천";
        }

        if (value.contains("괴산")) {
            return "괴산";
        }

        if (value.contains("진천")) {
            return "진천";
        }

        if (value.contains("음성")) {
            return "음성";
        }

        if (value.contains("증평")) {
            return "증평";
        }

        return "충북";
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }

        JsonNode value = node.path(fieldName);

        if (value.isMissingNode() || value.isNull()) {
            return "";
        }

        return safe(value.asText());
    }

    private String extractUrlFromHtmlOrText(String value) {
        String raw = safe(value)
                .replace("&amp;", "&")
                .trim();

        if (!hasText(raw)) {
            return "";
        }

        int hrefIndex = raw.toLowerCase().indexOf("href=");
        if (hrefIndex >= 0) {
            int start = hrefIndex + 5;
            char quote = raw.charAt(start);
            if (quote == '"' || quote == '\'') {
                int end = raw.indexOf(quote, start + 1);
                if (end > start) {
                    return raw.substring(start + 1, end);
                }
            }
        }

        if (raw.startsWith("http") || raw.startsWith("www")) {
            return raw.startsWith("www") ? "https://" + raw : raw;
        }

        return raw;
    }

    private String cleanHtml(String value) {
        if (!hasText(value)) {
            return "";
        }

        return value
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("<[^>]*>", "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }

        return "";
    }

    private String valueOrDefault(String value, String defaultValue) {
        if (hasText(value)) {
            return value;
        }

        return defaultValue;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
