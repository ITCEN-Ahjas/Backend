package com.example.Chungbuk.domain.festival.mapper;

import com.example.Chungbuk.domain.festival.dto.response.ExperienceListResponse;
import com.example.Chungbuk.domain.festival.dto.response.ExperienceSummaryResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalDetailResponse;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FestivalMapper {

    private static final DateTimeFormatter TOUR_API_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final Pattern HREF_PATTERN =
            Pattern.compile("href=[\"']([^\"']+)[\"']");

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
            List<FestivalSummaryResponse> items = extractFestivalItems(body);

            return FestivalListResponse.builder()
                    .items(items)
                    .page(page)
                    .size(size)
                    .totalCount(totalCount)
                    .build();

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("TourAPI 축제 목록 응답 파싱에 실패했습니다.", e);
        }
    }

    public ExperienceListResponse toExperienceListResponse(
            String rawJson,
            int page,
            int size
    ) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode body = root.path("response").path("body");

            int totalCount = body.path("totalCount").asInt(0);
            List<ExperienceSummaryResponse> items = extractExperienceItems(body);

            return ExperienceListResponse.builder()
                    .items(items)
                    .page(page)
                    .size(size)
                    .totalCount(totalCount)
                    .build();

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("TourAPI 체험/관광 콘텐츠 응답 파싱에 실패했습니다.", e);
        }
    }

    public FestivalDetailResponse toFestivalDetailResponse(
            String commonRawJson,
            String introRawJson,
            String imageRawJson,
            String fallbackListRawJson,
            String contentId
    ) {
        try {
            JsonNode commonBody = objectMapper.readTree(commonRawJson)
                    .path("response")
                    .path("body");

            JsonNode introBody = objectMapper.readTree(introRawJson)
                    .path("response")
                    .path("body");

            JsonNode imageBody = objectMapper.readTree(imageRawJson)
                    .path("response")
                    .path("body");

            JsonNode fallbackBody = objectMapper.readTree(fallbackListRawJson)
                    .path("response")
                    .path("body");

            JsonNode commonItem = extractFirstItem(commonBody);
            JsonNode introItem = extractFirstItem(introBody);
            JsonNode fallbackItem = extractItemByContentId(fallbackBody, contentId);

            JsonNode baseItem = hasText(getText(commonItem, "title"))
                    ? commonItem
                    : fallbackItem;

            String title = getText(baseItem, "title");
            String address = buildAddress(
                    getText(baseItem, "addr1"),
                    getText(baseItem, "addr2")
            );

            String startDate = firstNonBlank(
                    getText(introItem, "eventstartdate"),
                    getText(baseItem, "eventstartdate")
            );

            String endDate = firstNonBlank(
                    getText(introItem, "eventenddate"),
                    getText(baseItem, "eventenddate")
            );

            List<String> imageUrls = extractImageUrls(baseItem, imageBody);
            String imageUrl = imageUrls.isEmpty()
                    ? resolveImageUrl(baseItem)
                    : imageUrls.get(0);

            return FestivalDetailResponse.builder()
                    .id(firstNonBlank(
                            getText(baseItem, "contentid"),
                            getText(introItem, "contentid"),
                            contentId
                    ))
                    .title(title)
                    .region(extractRegion(address))
                    .category(resolveFestivalCategory(title, address))
                    .status(resolveStatus(startDate, endDate))
                    .startDate(startDate)
                    .endDate(endDate)
                    .address(address)
                    .imageUrl(imageUrl)
                    .imageUrls(imageUrls)
                    .tel(getText(baseItem, "tel"))
                    .homepage(extractHomepageUrl(getText(commonItem, "homepage")))
                    .overview(cleanHtml(getText(commonItem, "overview")))
                    .mapX(getText(baseItem, "mapx"))
                    .mapY(getText(baseItem, "mapy"))
                    .eventPlace(getText(introItem, "eventplace"))
                    .playTime(cleanHtml(getText(introItem, "playtime")))
                    .useTimeFestival(cleanHtml(getText(introItem, "usetimefestival")))
                    .sponsor(resolveSponsor(introItem))
                    .build();

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("TourAPI 상세 응답 파싱에 실패했습니다.", e);
        }
    }

    private List<FestivalSummaryResponse> extractFestivalItems(JsonNode body) {
        List<FestivalSummaryResponse> result = new ArrayList<>();

        JsonNode itemNode = body.path("items").path("item");

        if (itemNode.isMissingNode() || itemNode.isNull()) {
            return result;
        }

        if (itemNode.isArray()) {
            for (JsonNode item : itemNode) {
                result.add(toFestivalSummaryResponse(item));
            }
            return result;
        }

        if (itemNode.isObject()) {
            result.add(toFestivalSummaryResponse(itemNode));
        }

        return result;
    }

    private List<ExperienceSummaryResponse> extractExperienceItems(JsonNode body) {
        List<ExperienceSummaryResponse> result = new ArrayList<>();

        JsonNode itemNode = body.path("items").path("item");

        if (itemNode.isMissingNode() || itemNode.isNull()) {
            return result;
        }

        if (itemNode.isArray()) {
            for (JsonNode item : itemNode) {
                result.add(toExperienceSummaryResponse(item));
            }
            return result;
        }

        if (itemNode.isObject()) {
            result.add(toExperienceSummaryResponse(itemNode));
        }

        return result;
    }

    private JsonNode extractFirstItem(JsonNode body) {
        JsonNode itemNode = body.path("items").path("item");

        if (itemNode.isArray() && itemNode.size() > 0) {
            return itemNode.get(0);
        }

        if (itemNode.isObject()) {
            return itemNode;
        }

        return objectMapper.createObjectNode();
    }

    private JsonNode extractItemByContentId(
            JsonNode body,
            String contentId
    ) {
        JsonNode itemNode = body.path("items").path("item");

        if (itemNode.isArray()) {
            for (JsonNode item : itemNode) {
                if (contentId.equals(getText(item, "contentid"))) {
                    return item;
                }
            }
        }

        if (itemNode.isObject() && contentId.equals(getText(itemNode, "contentid"))) {
            return itemNode;
        }

        return objectMapper.createObjectNode();
    }

    private FestivalSummaryResponse toFestivalSummaryResponse(JsonNode item) {
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
                .category(resolveFestivalCategory(title, address))
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

    private ExperienceSummaryResponse toExperienceSummaryResponse(JsonNode item) {
        String title = getText(item, "title");
        String address = buildAddress(
                getText(item, "addr1"),
                getText(item, "addr2")
        );
        String contentTypeId = getText(item, "contenttypeid");

        return ExperienceSummaryResponse.builder()
                .id(getText(item, "contentid"))
                .title(title)
                .region(extractRegion(address))
                .category(resolveExperienceCategory(contentTypeId))
                .address(address)
                .imageUrl(resolveImageUrl(item))
                .tel(getText(item, "tel"))
                .mapX(getText(item, "mapx"))
                .mapY(getText(item, "mapy"))
                .contentTypeId(contentTypeId)
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

    private List<String> extractImageUrls(
            JsonNode baseItem,
            JsonNode imageBody
    ) {
        Set<String> imageUrlSet = new LinkedHashSet<>();

        String firstImage = resolveImageUrl(baseItem);
        if (!firstImage.isBlank()) {
            imageUrlSet.add(firstImage);
        }

        JsonNode imageItemNode = imageBody.path("items").path("item");

        if (imageItemNode.isArray()) {
            for (JsonNode imageItem : imageItemNode) {
                addImageUrl(imageUrlSet, imageItem);
            }
        }

        if (imageItemNode.isObject()) {
            addImageUrl(imageUrlSet, imageItemNode);
        }

        return new ArrayList<>(imageUrlSet);
    }

    private void addImageUrl(
            Set<String> imageUrlSet,
            JsonNode imageItem
    ) {
        String originImageUrl = getText(imageItem, "originimgurl");
        String smallImageUrl = getText(imageItem, "smallimageurl");

        if (!originImageUrl.isBlank()) {
            imageUrlSet.add(originImageUrl);
            return;
        }

        if (!smallImageUrl.isBlank()) {
            imageUrlSet.add(smallImageUrl);
        }
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

    private String resolveFestivalCategory(String title, String address) {
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

    private String resolveExperienceCategory(String contentTypeId) {
        if (contentTypeId.equals("12")) {
            return "관광지";
        }

        if (contentTypeId.equals("14")) {
            return "문화시설";
        }

        if (contentTypeId.equals("25")) {
            return "여행코스";
        }

        if (contentTypeId.equals("28")) {
            return "레포츠";
        }

        return "체험/관광";
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

    private String extractHomepageUrl(String homepage) {
        if (homepage == null || homepage.isBlank()) {
            return "";
        }

        Matcher matcher = HREF_PATTERN.matcher(homepage);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return cleanHtml(homepage);
    }

    private String cleanHtml(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value
                .replaceAll("<[^>]*>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String resolveSponsor(JsonNode introItem) {
        String sponsor1 = getText(introItem, "sponsor1");

        if (!sponsor1.isBlank()) {
            return sponsor1;
        }

        return getText(introItem, "sponsor2");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return "";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}