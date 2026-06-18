package com.example.Chungbuk.domain.festival.mapper;

import com.example.Chungbuk.domain.festival.constant.SupportedContentType;
import com.example.Chungbuk.domain.festival.dto.response.ContentInfoResponse;
import com.example.Chungbuk.domain.festival.dto.response.ExperienceListResponse;
import com.example.Chungbuk.domain.festival.dto.response.ExperienceSummaryResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalDetailResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalListResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalSummaryResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class FestivalMapper {

    private static final String DEFAULT_FESTIVAL_CONTENT_TYPE_ID = "15";

    private static final DateTimeFormatter TOUR_API_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final Pattern HREF_PATTERN = Pattern.compile(
            "href\\s*=\\s*[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern URL_PATTERN = Pattern.compile(
            "(https?://[^\\s\"'<>]+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern WWW_PATTERN = Pattern.compile(
            "(www\\.[^\\s\"'<>]+)",
            Pattern.CASE_INSENSITIVE
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    public FestivalListResponse toFestivalListResponse(
            String rawJson,
            int page,
            int size
    ) {
        try {
            JsonNode body = getBody(rawJson);
            JsonNode itemNode = body.path("items").path("item");

            List<FestivalSummaryResponse> items = toItemNodeList(itemNode)
                    .stream()
                    .map(this::toFestivalSummaryResponse)
                    .filter(item ->
                            SupportedContentType.isSupported(
                                    item.getContentTypeId()
                            )
                    )
                    .toList();

            return FestivalListResponse.builder()
                    .items(items)
                    .page(page)
                    .size(size)
                    .totalCount(items.size())
                    .build();
        } catch (Exception e) {
            return FestivalListResponse.builder()
                    .items(List.of())
                    .page(page)
                    .size(size)
                    .totalCount(0)
                    .build();
        }
    }

    public ExperienceListResponse toExperienceListResponse(
            String rawJson,
            int page,
            int size
    ) {
        try {
            JsonNode body = getBody(rawJson);
            JsonNode itemNode = body.path("items").path("item");

            List<ExperienceSummaryResponse> items = toItemNodeList(itemNode)
                    .stream()
                    .map(this::toExperienceSummaryResponse)
                    .filter(item ->
                            SupportedContentType.isExperienceSupported(
                                    item.getContentTypeId()
                            )
                    )
                    .toList();

            return ExperienceListResponse.builder()
                    .items(items)
                    .page(page)
                    .size(size)
                    .totalCount(items.size())
                    .build();
        } catch (Exception e) {
            return ExperienceListResponse.builder()
                    .items(List.of())
                    .page(page)
                    .size(size)
                    .totalCount(0)
                    .build();
        }
    }

    public FestivalDetailResponse toFestivalDetailResponse(
            String detailCommonRawJson,
            String detailIntroRawJson,
            String detailImageRawJson,
            String fallbackListRawJson,
            String contentId
    ) {
        try {
            JsonNode commonItem = firstItem(detailCommonRawJson);
            JsonNode introItem = firstItem(detailIntroRawJson);
            JsonNode fallbackItem = findItemByContentId(
                    fallbackListRawJson,
                    contentId
            );

            boolean hasIntroItem = hasDetailItem(detailIntroRawJson);

            String resolvedContentId = firstNonBlank(
                    text(commonItem, "contentid"),
                    text(introItem, "contentid"),
                    text(fallbackItem, "contentid"),
                    contentId
            );

            String contentTypeId = firstNonBlank(
                    text(commonItem, "contenttypeid"),
                    text(introItem, "contenttypeid"),
                    text(fallbackItem, "contenttypeid"),
                    DEFAULT_FESTIVAL_CONTENT_TYPE_ID
            );

            String cat1 = firstNonBlank(
                    text(commonItem, "cat1"),
                    text(fallbackItem, "cat1")
            );

            String cat2 = firstNonBlank(
                    text(commonItem, "cat2"),
                    text(fallbackItem, "cat2")
            );

            String cat3 = firstNonBlank(
                    text(commonItem, "cat3"),
                    text(fallbackItem, "cat3")
            );

            String title = firstNonBlank(
                    text(commonItem, "title"),
                    text(fallbackItem, "title")
            );

            String address = combineAddress(
                    firstNonBlank(
                            text(commonItem, "addr1"),
                            text(fallbackItem, "addr1")
                    ),
                    firstNonBlank(
                            text(commonItem, "addr2"),
                            text(fallbackItem, "addr2")
                    )
            );

            String region = extractRegion(address);

            String startDate = firstNonBlank(
                    text(introItem, "eventstartdate"),
                    text(fallbackItem, "eventstartdate")
            );

            String endDate = firstNonBlank(
                    text(introItem, "eventenddate"),
                    text(fallbackItem, "eventenddate")
            );

            String category = resolveCategory(
                    contentTypeId,
                    cat2,
                    cat3,
                    title
            );

            String themeCategory = resolveThemeCategory(
                    contentTypeId,
                    cat2,
                    cat3,
                    title,
                    address
            );

            String eventPlace = firstNonBlank(
                    cleanHtml(text(introItem, "eventplace")),
                    address
            );

            String playTime = cleanHtml(text(introItem, "playtime"));

            String useTimeFestival = firstNonBlank(
                    cleanHtml(text(introItem, "usetimefestival")),
                    cleanHtml(text(introItem, "discountinfofestival")),
                    cleanHtml(text(introItem, "usefee")),
                    cleanHtml(text(introItem, "usefeeleports"))
            );

            String sponsor = combineSponsor(
                    cleanHtml(text(introItem, "sponsor1")),
                    cleanHtml(text(introItem, "sponsor2"))
            );

            String imageUrl = firstNonBlank(
                    text(commonItem, "firstimage"),
                    text(fallbackItem, "firstimage"),
                    text(commonItem, "firstimage2"),
                    text(fallbackItem, "firstimage2")
            );

            List<String> imageUrls = extractImageUrls(
                    detailImageRawJson,
                    imageUrl
            );

            String overview = cleanHtml(text(commonItem, "overview"));

            DescriptionInfo descriptionInfo = buildDescription(
                    overview,
                    title,
                    region,
                    category,
                    themeCategory
            );

            CardDisplayInfo displayInfo = resolveDisplayInfo(
                    contentTypeId,
                    category,
                    startDate,
                    endDate,
                    playTime,
                    eventPlace,
                    address,
                    region,
                    introItem
            );

            List<ContentInfoResponse> mainInfo = hasIntroItem
                    ? buildDetailMainInfo(
                    contentTypeId,
                    category,
                    themeCategory,
                    startDate,
                    endDate,
                    playTime,
                    eventPlace,
                    useTimeFestival,
                    sponsor,
                    introItem,
                    address
            )
                    : List.of();

            return FestivalDetailResponse.builder()
                    .id(resolvedContentId)
                    .contentTypeId(contentTypeId)
                    .cat1(cat1)
                    .cat2(cat2)
                    .cat3(cat3)
                    .title(title)
                    .region(region)
                    .category(category)
                    .themeCategory(themeCategory)
                    .status(calculateStatus(startDate, endDate))
                    .startDate(startDate)
                    .endDate(endDate)
                    .address(address)
                    .imageUrl(imageUrl)
                    .imageUrls(imageUrls)
                    .tel(firstNonBlank(
                            text(commonItem, "tel"),
                            text(fallbackItem, "tel")
                    ))
                    .homepage(extractUrlFromHtmlOrText(
                            firstNonBlank(
                                    text(commonItem, "homepage"),
                                    text(introItem, "eventhomepage")
                            )
                    ))
                    .overview(overview)
                    .description(descriptionInfo.description())
                    .descriptionSource(descriptionInfo.source())
                    .mapX(firstNonBlank(
                            text(commonItem, "mapx"),
                            text(fallbackItem, "mapx")
                    ))
                    .mapY(firstNonBlank(
                            text(commonItem, "mapy"),
                            text(fallbackItem, "mapy")
                    ))
                    .eventPlace(eventPlace)
                    .playTime(playTime)
                    .useTimeFestival(useTimeFestival)
                    .sponsor(sponsor)
                    .timeLabel(displayInfo.timeLabel())
                    .timeValue(displayInfo.timeValue())
                    .extraLabel(displayInfo.extraLabel())
                    .extraValue(displayInfo.extraValue())
                    .mainInfo(mainInfo)
                    .build();
        } catch (Exception e) {
            return FestivalDetailResponse.builder()
                    .id(contentId)
                    .contentTypeId(DEFAULT_FESTIVAL_CONTENT_TYPE_ID)
                    .cat1("")
                    .cat2("")
                    .cat3("")
                    .title("")
                    .region("")
                    .category("")
                    .themeCategory("")
                    .status("")
                    .startDate("")
                    .endDate("")
                    .address("")
                    .imageUrl("")
                    .imageUrls(List.of())
                    .tel("")
                    .homepage("")
                    .overview("")
                    .description("")
                    .descriptionSource("")
                    .mapX("")
                    .mapY("")
                    .eventPlace("")
                    .playTime("")
                    .useTimeFestival("")
                    .sponsor("")
                    .timeLabel("")
                    .timeValue("")
                    .extraLabel("")
                    .extraValue("")
                    .mainInfo(List.of())
                    .build();
        }
    }

    public boolean hasDetailItem(String rawJson) {
        try {
            JsonNode body = getBody(rawJson);

            return !toItemNodeList(
                    body.path("items").path("item")
            ).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public String extractContentTypeId(String rawJson) {
        return text(firstItem(rawJson), "contenttypeid");
    }

    private FestivalSummaryResponse toFestivalSummaryResponse(JsonNode item) {
        String contentTypeId = firstNonBlank(
                text(item, "contenttypeid"),
                DEFAULT_FESTIVAL_CONTENT_TYPE_ID
        );

        String cat1 = text(item, "cat1");
        String cat2 = text(item, "cat2");
        String cat3 = text(item, "cat3");

        String title = text(item, "title");

        String address = combineAddress(
                text(item, "addr1"),
                text(item, "addr2")
        );

        String region = extractRegion(address);

        String startDate = text(item, "eventstartdate");
        String endDate = text(item, "eventenddate");

        String category = resolveCategory(
                contentTypeId,
                cat2,
                cat3,
                title
        );

        String themeCategory = resolveThemeCategory(
                contentTypeId,
                cat2,
                cat3,
                title,
                address
        );

        CardDisplayInfo displayInfo = resolveDisplayInfo(
                contentTypeId,
                category,
                startDate,
                endDate,
                "",
                "",
                address,
                region,
                objectNode()
        );

        return FestivalSummaryResponse.builder()
                .id(text(item, "contentid"))
                .contentTypeId(contentTypeId)
                .cat1(cat1)
                .cat2(cat2)
                .cat3(cat3)
                .title(title)
                .region(region)
                .category(category)
                .themeCategory(themeCategory)
                .status(calculateStatus(startDate, endDate))
                .startDate(startDate)
                .endDate(endDate)
                .address(address)
                .imageUrl(firstNonBlank(
                        text(item, "firstimage"),
                        text(item, "firstimage2")
                ))
                .tel(text(item, "tel"))
                .mapX(text(item, "mapx"))
                .mapY(text(item, "mapy"))
                .timeLabel(displayInfo.timeLabel())
                .timeValue(displayInfo.timeValue())
                .extraLabel(displayInfo.extraLabel())
                .extraValue(displayInfo.extraValue())
                .modifiedTime(text(item, "modifiedtime"))
                .build();
    }

    private ExperienceSummaryResponse toExperienceSummaryResponse(
            JsonNode item
    ) {
        String contentTypeId = text(item, "contenttypeid");

        String cat1 = text(item, "cat1");
        String cat2 = text(item, "cat2");
        String cat3 = text(item, "cat3");

        String title = text(item, "title");

        String address = combineAddress(
                text(item, "addr1"),
                text(item, "addr2")
        );

        String region = extractRegion(address);

        String category = resolveCategory(
                contentTypeId,
                cat2,
                cat3,
                title
        );

        String themeCategory = resolveThemeCategory(
                contentTypeId,
                cat2,
                cat3,
                title,
                address
        );

        CardDisplayInfo displayInfo = resolveDisplayInfo(
                contentTypeId,
                category,
                "",
                "",
                "",
                "",
                address,
                region,
                objectNode()
        );

        return ExperienceSummaryResponse.builder()
                .id(text(item, "contentid"))
                .contentTypeId(contentTypeId)
                .cat1(cat1)
                .cat2(cat2)
                .cat3(cat3)
                .title(title)
                .region(region)
                .category(category)
                .themeCategory(themeCategory)
                .address(address)
                .imageUrl(firstNonBlank(
                        text(item, "firstimage"),
                        text(item, "firstimage2")
                ))
                .tel(text(item, "tel"))
                .mapX(text(item, "mapx"))
                .mapY(text(item, "mapy"))
                .timeLabel(displayInfo.timeLabel())
                .timeValue(displayInfo.timeValue())
                .extraLabel(displayInfo.extraLabel())
                .extraValue(displayInfo.extraValue())
                .modifiedTime(text(item, "modifiedtime"))
                .build();
    }

    private String resolveCategory(
            String contentTypeId,
            String cat2,
            String cat3,
            String title
    ) {
        if ("15".equals(contentTypeId)) {
            return resolveEventCategory(cat2, cat3, title);
        }

        return SupportedContentType.resolveDefaultCategory(contentTypeId);
    }

    private String resolveEventCategory(
            String cat2,
            String cat3,
            String title
    ) {
        String merged = normalizeForSearch(cat2 + " " + cat3 + " " + title);

        if (containsAny(
                merged,
                "a0207",
                "축제",
                "페스티벌",
                "문화제",
                "제전"
        )) {
            return "축제";
        }

        if (containsAny(
                merged,
                "a02080100",
                "a02080200",
                "a02080300",
                "a02080400",
                "a02080800",
                "a02080900",
                "a02081000",
                "전통공연",
                "연극",
                "뮤지컬",
                "오페라",
                "무용",
                "클래식",
                "콘서트",
                "공연"
        )) {
            return "공연";
        }

        return "행사";
    }

    private String resolveThemeCategory(
            String contentTypeId,
            String cat2,
            String cat3,
            String title,
            String address
    ) {
        String merged = normalizeForSearch(
                cat2 + " " + cat3 + " " + title + " " + address
        );

        if ("12".equals(contentTypeId)) {
            if (containsAny(
                    merged,
                    "산",
                    "계곡",
                    "폭포",
                    "호수",
                    "강",
                    "자연",
                    "숲",
                    "휴양림",
                    "수목원"
            )) {
                return "자연관광";
            }

            if (containsAny(
                    merged,
                    "사찰",
                    "절",
                    "문화재",
                    "유적",
                    "성곽",
                    "향교",
                    "고택"
            )) {
                return "역사문화";
            }

            return "관광지";
        }

        if ("14".equals(contentTypeId)) {
            if (containsAny(merged, "박물관")) {
                return "박물관";
            }

            if (containsAny(merged, "미술관")) {
                return "미술관";
            }

            if (containsAny(merged, "전시", "전시관")) {
                return "전시시설";
            }

            if (containsAny(
                    merged,
                    "공연장",
                    "문화관",
                    "문화센터"
            )) {
                return "문화공간";
            }

            return "문화시설";
        }

        if ("15".equals(contentTypeId)) {
            if (containsAny(
                    merged,
                    "먹거리",
                    "음식",
                    "푸드",
                    "와인",
                    "대추",
                    "시장",
                    "야시장"
            )) {
                return "먹거리";
            }

            if (containsAny(
                    merged,
                    "야행",
                    "야간",
                    "밤",
                    "불빛",
                    "라이트"
            )) {
                return "야간행사";
            }

            if (containsAny(
                    merged,
                    "전통",
                    "문화유산",
                    "국가유산",
                    "민속"
            )) {
                return "전통문화";
            }

            if (containsAny(
                    merged,
                    "전시회",
                    "박람회",
                    "컨벤션",
                    "엑스포"
            )) {
                return "전시행사";
            }

            if (containsAny(
                    merged,
                    "공연",
                    "콘서트",
                    "음악",
                    "연극",
                    "뮤지컬"
            )) {
                return "공연";
            }

            if (containsAny(
                    merged,
                    "축제",
                    "페스티벌",
                    "문화제"
            )) {
                return "문화축제";
            }

            return "행사";
        }

        if ("28".equals(contentTypeId)) {
            if (containsAny(
                    merged,
                    "캠핑",
                    "야영장",
                    "카라반",
                    "글램핑"
            )) {
                return "캠핑";
            }

            if (containsAny(
                    merged,
                    "수상",
                    "래프팅",
                    "카약",
                    "카누",
                    "물놀이"
            )) {
                return "수상레포츠";
            }

            if (containsAny(
                    merged,
                    "산악",
                    "등산",
                    "클라이밍",
                    "트레킹"
            )) {
                return "산악레포츠";
            }

            if (containsAny(
                    merged,
                    "자전거",
                    "레일바이크",
                    "바이크"
            )) {
                return "자전거";
            }

            return "레포츠";
        }

        return "기타";
    }

    private CardDisplayInfo resolveDisplayInfo(
            String contentTypeId,
            String category,
            String startDate,
            String endDate,
            String playTime,
            String eventPlace,
            String address,
            String region,
            JsonNode introItem
    ) {
        String place = resolvePlaceValue(
                eventPlace,
                address,
                region
        );

        if ("28".equals(contentTypeId)) {
            return new CardDisplayInfo(
                    "",
                    "",
                    isUsefulText(place) ? "장소" : "",
                    isUsefulText(place) ? place : ""
            );
        }

        if ("12".equals(contentTypeId)) {
            return buildTimeAndPlace(
                    "이용 시간",
                    cleanHtml(text(introItem, "usetime")),
                    place
            );
        }

        if ("14".equals(contentTypeId)) {
            return buildTimeAndPlace(
                    "관람 시간",
                    cleanHtml(text(introItem, "usetimeculture")),
                    place
            );
        }

        String dateRange = formatDateRange(startDate, endDate);

        if ("공연".equals(category)) {
            if (isUsefulText(playTime)) {
                return buildTimeAndPlace(
                        "공연 시간",
                        playTime,
                        place
                );
            }

            return buildTimeAndPlace(
                    "공연 기간",
                    dateRange,
                    place
            );
        }

        if ("축제".equals(category)) {
            return buildTimeAndPlace(
                    "축제 기간",
                    dateRange,
                    place
            );
        }

        return buildTimeAndPlace(
                "행사 기간",
                dateRange,
                place
        );
    }

    private CardDisplayInfo buildTimeAndPlace(
            String timeLabel,
            String timeValue,
            String place
    ) {
        return new CardDisplayInfo(
                isUsefulText(timeValue) ? timeLabel : "",
                isUsefulText(timeValue) ? timeValue : "",
                isUsefulText(place) ? "장소" : "",
                isUsefulText(place) ? place : ""
        );
    }

    private List<ContentInfoResponse> buildDetailMainInfo(
            String contentTypeId,
            String category,
            String themeCategory,
            String startDate,
            String endDate,
            String playTime,
            String eventPlace,
            String useTimeFestival,
            String sponsor,
            JsonNode introItem,
            String address
    ) {
        if ("12".equals(contentTypeId)) {
            return buildTouristMainInfo(
                    introItem,
                    themeCategory,
                    address
            );
        }

        if ("14".equals(contentTypeId)) {
            return buildCultureMainInfo(
                    introItem,
                    themeCategory,
                    address
            );
        }

        if ("28".equals(contentTypeId)) {
            return buildLeportsMainInfo(
                    introItem,
                    themeCategory,
                    address
            );
        }

        return buildEventMainInfo(
                category,
                themeCategory,
                startDate,
                endDate,
                playTime,
                eventPlace,
                useTimeFestival,
                sponsor,
                introItem,
                address
        );
    }

    private List<ContentInfoResponse> buildEventMainInfo(
            String category,
            String themeCategory,
            String startDate,
            String endDate,
            String playTime,
            String eventPlace,
            String useTimeFestival,
            String sponsor,
            JsonNode introItem,
            String address
    ) {
        List<ContentInfoResponse> mainInfo = new ArrayList<>();

        if ("공연".equals(category)) {
            addInfo(mainInfo, "공연 일시", formatDateRange(startDate, endDate));
            addInfo(mainInfo, "공연 시간", playTime);
            addInfo(mainInfo, "공연 장소", firstNonBlank(eventPlace, address));
            addInfo(mainInfo, "공연 유형", themeCategory);
            addInfo(mainInfo, "이용 요금", useTimeFestival);
            addInfo(mainInfo, "주최/주관", sponsor);
            addInfo(mainInfo, "프로그램", cleanHtml(text(introItem, "program")));
            return mainInfo;
        }

        if ("축제".equals(category)) {
            addInfo(mainInfo, "축제 기간", formatDateRange(startDate, endDate));
            addInfo(mainInfo, "축제 시간", playTime);
            addInfo(mainInfo, "축제 장소", firstNonBlank(eventPlace, address));
            addInfo(mainInfo, "축제 테마", themeCategory);
            addInfo(mainInfo, "이용 요금", useTimeFestival);
            addInfo(mainInfo, "주최/주관", sponsor);
            addInfo(mainInfo, "부대 행사", cleanHtml(text(introItem, "subevent")));
            return mainInfo;
        }

        addInfo(mainInfo, "행사 기간", formatDateRange(startDate, endDate));
        addInfo(mainInfo, "행사 시간", playTime);
        addInfo(mainInfo, "행사 장소", firstNonBlank(eventPlace, address));
        addInfo(mainInfo, "행사 유형", themeCategory);
        addInfo(mainInfo, "이용 요금", useTimeFestival);
        addInfo(mainInfo, "주최/주관", sponsor);
        addInfo(mainInfo, "프로그램", cleanHtml(text(introItem, "program")));

        return mainInfo;
    }

    private List<ContentInfoResponse> buildTouristMainInfo(
            JsonNode introItem,
            String themeCategory,
            String address
    ) {
        List<ContentInfoResponse> mainInfo = new ArrayList<>();

        addInfo(mainInfo, "관광 유형", themeCategory);
        addInfo(mainInfo, "이용 시간", cleanHtml(text(introItem, "usetime")));
        addInfo(mainInfo, "쉬는 날", cleanHtml(text(introItem, "restdate")));
        addInfo(mainInfo, "문의처", cleanHtml(text(introItem, "infocenter")));
        addInfo(mainInfo, "주차", cleanHtml(text(introItem, "parking")));
        addInfo(mainInfo, "소요 시간", cleanHtml(text(introItem, "taketime")));
        addInfo(mainInfo, "체험 안내", cleanHtml(text(introItem, "expguide")));
        addInfo(mainInfo, "주소", address);

        return mainInfo;
    }

    private List<ContentInfoResponse> buildCultureMainInfo(
            JsonNode introItem,
            String themeCategory,
            String address
    ) {
        List<ContentInfoResponse> mainInfo = new ArrayList<>();

        addInfo(mainInfo, "시설 유형", themeCategory);
        addInfo(mainInfo, "관람 시간", cleanHtml(text(introItem, "usetimeculture")));
        addInfo(mainInfo, "쉬는 날", cleanHtml(text(introItem, "restdateculture")));
        addInfo(mainInfo, "문의처", cleanHtml(text(introItem, "infocenterculture")));
        addInfo(mainInfo, "이용 요금", cleanHtml(text(introItem, "usefee")));
        addInfo(mainInfo, "주차", cleanHtml(text(introItem, "parkingculture")));
        addInfo(mainInfo, "규모", cleanHtml(text(introItem, "scale")));
        addInfo(mainInfo, "주소", address);

        return mainInfo;
    }

    private List<ContentInfoResponse> buildLeportsMainInfo(
            JsonNode introItem,
            String themeCategory,
            String address
    ) {
        List<ContentInfoResponse> mainInfo = new ArrayList<>();

        addInfo(mainInfo, "활동 유형", themeCategory);
        addInfo(mainInfo, "운영 기간", cleanHtml(text(introItem, "openperiod")));
        addInfo(mainInfo, "이용 시간", cleanHtml(text(introItem, "usetimeleports")));
        addInfo(mainInfo, "쉬는 날", cleanHtml(text(introItem, "restdateleports")));
        addInfo(mainInfo, "문의처", cleanHtml(text(introItem, "infocenterleports")));
        addInfo(mainInfo, "이용 요금", cleanHtml(text(introItem, "usefeeleports")));
        addInfo(mainInfo, "주차", cleanHtml(text(introItem, "parkingleports")));
        addInfo(mainInfo, "주소", address);

        return mainInfo;
    }

    private void addInfo(
            List<ContentInfoResponse> mainInfo,
            String label,
            String value
    ) {
        if (!hasText(label) || !isUsefulText(value)) {
            return;
        }

        mainInfo.add(ContentInfoResponse.builder()
                .label(label.trim())
                .value(value.trim())
                .build());
    }

    private DescriptionInfo buildDescription(
            String overview,
            String title,
            String region,
            String category,
            String themeCategory
    ) {
        if (hasText(overview)) {
            return new DescriptionInfo(
                    overview,
                    "DETAIL_COMMON_OVERVIEW"
            );
        }

        return new DescriptionInfo(
                buildFallbackDescription(
                        title,
                        region,
                        category,
                        themeCategory
                ),
                "DETAIL_COMMON_EMPTY"
        );
    }

    private String buildFallbackDescription(
            String title,
            String region,
            String category,
            String themeCategory
    ) {
        if (!hasText(title)) {
            return "";
        }

        String resolvedRegion = valueOrDefault(region, "충북");
        String resolvedCategory = valueOrDefault(category, "관광 콘텐츠");
        String resolvedThemeCategory = valueOrDefault(
                themeCategory,
                resolvedCategory
        );

        return title + "은(는) " + resolvedRegion + "에서 만날 수 있는 "
                + resolvedThemeCategory + " 성격의 "
                + resolvedCategory + "입니다.";
    }

    private JsonNode getBody(String rawJson) throws Exception {
        if (!hasText(rawJson)) {
            return objectNode();
        }

        JsonNode root = objectMapper.readTree(rawJson);

        JsonNode responseBody = root.path("response").path("body");

        if (!responseBody.isMissingNode() && !responseBody.isNull()) {
            return responseBody;
        }

        JsonNode directBody = root.path("body");

        if (!directBody.isMissingNode() && !directBody.isNull()) {
            return directBody;
        }

        return objectNode();
    }

    private JsonNode firstItem(String rawJson) {
        try {
            JsonNode body = getBody(rawJson);
            List<JsonNode> items = toItemNodeList(
                    body.path("items").path("item")
            );

            return items.isEmpty() ? objectNode() : items.get(0);
        } catch (Exception e) {
            return objectNode();
        }
    }

    private JsonNode findItemByContentId(
            String rawJson,
            String contentId
    ) {
        try {
            JsonNode body = getBody(rawJson);

            for (JsonNode item : toItemNodeList(
                    body.path("items").path("item")
            )) {
                if (hasText(contentId)
                        && contentId.equals(text(item, "contentid"))) {
                    return item;
                }
            }

            return objectNode();
        } catch (Exception e) {
            return objectNode();
        }
    }

    private List<JsonNode> toItemNodeList(JsonNode itemNode) {
        List<JsonNode> items = new ArrayList<>();

        if (itemNode == null
                || itemNode.isMissingNode()
                || itemNode.isNull()
                || itemNode.isTextual()) {
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

    private List<String> extractImageUrls(
            String detailImageRawJson,
            String imageUrl
    ) {
        Set<String> imageUrlSet = new LinkedHashSet<>();

        if (hasText(imageUrl)) {
            imageUrlSet.add(imageUrl);
        }

        try {
            JsonNode body = getBody(detailImageRawJson);

            for (JsonNode item : toItemNodeList(
                    body.path("items").path("item")
            )) {
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

    private String combineSponsor(String sponsor1, String sponsor2) {
        String first = safe(sponsor1);
        String second = safe(sponsor2);

        if (!hasText(first)) {
            return second;
        }

        if (!hasText(second)) {
            return first;
        }

        return first.equals(second) ? first : first + " / " + second;
    }

    private String resolvePlaceValue(
            String eventPlace,
            String address,
            String region
    ) {
        return firstNonBlank(
                cleanHtml(eventPlace),
                address,
                region
        );
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

    private String calculateStatus(String startDate, String endDate) {
        if (!isValidDate(startDate) || !isValidDate(endDate)) {
            return "";
        }

        try {
            LocalDate today = LocalDate.now();
            LocalDate start = LocalDate.parse(
                    startDate,
                    TOUR_API_DATE_FORMAT
            );
            LocalDate end = LocalDate.parse(
                    endDate,
                    TOUR_API_DATE_FORMAT
            );

            if (today.isBefore(start)) {
                return "예정";
            }

            if (today.isAfter(end)) {
                return "종료";
            }

            return "진행중";
        } catch (Exception e) {
            return "";
        }
    }

    private String formatDateRange(String startDate, String endDate) {
        String start = formatDate(startDate);
        String end = formatDate(endDate);

        if (!hasText(start) && !hasText(end)) {
            return "";
        }

        if (start.equals(end)) {
            return start;
        }

        if (!hasText(start)) {
            return end;
        }

        if (!hasText(end)) {
            return start;
        }

        return start + " ~ " + end;
    }

    private String formatDate(String value) {
        if (!isValidDate(value)) {
            return "";
        }

        return value.substring(0, 4)
                + "."
                + value.substring(4, 6)
                + "."
                + value.substring(6, 8);
    }

    private boolean isValidDate(String value) {
        return value != null && value.matches("\\d{8}");
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null
                || node.isMissingNode()
                || node.isNull()) {
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

        Matcher hrefMatcher = HREF_PATTERN.matcher(raw);

        if (hrefMatcher.find()) {
            return safe(hrefMatcher.group(1))
                    .replace("&amp;", "&");
        }

        Matcher urlMatcher = URL_PATTERN.matcher(raw);

        if (urlMatcher.find()) {
            return safe(urlMatcher.group(1))
                    .replace("&amp;", "&");
        }

        Matcher wwwMatcher = WWW_PATTERN.matcher(raw);

        if (wwwMatcher.find()) {
            return "https://"
                    + safe(wwwMatcher.group(1))
                    .replace("&amp;", "&");
        }

        return "";
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

    private boolean containsAny(
            String value,
            String... keywords
    ) {
        if (!hasText(value) || keywords == null) {
            return false;
        }

        for (String keyword : keywords) {
            if (hasText(keyword)
                    && value.contains(keyword.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    private String normalizeForSearch(String value) {
        return safe(value)
                .replace(" ", "")
                .toLowerCase();
    }

    private String valueOrDefault(
            String value,
            String defaultValue
    ) {
        return hasText(value) ? value : defaultValue;
    }

    private boolean isUsefulText(String value) {
        if (!hasText(value)) {
            return false;
        }

        String normalized = value.trim();

        return !normalized.equals("정보 없음")
                && !normalized.equals("상세페이지에서 확인")
                && !normalized.equals("제공된 상세 설명이 없습니다.")
                && !normalized.equals("분류");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private JsonNode objectNode() {
        return objectMapper.createObjectNode();
    }

    private record CardDisplayInfo(
            String timeLabel,
            String timeValue,
            String extraLabel,
            String extraValue
    ) {
    }

    private record DescriptionInfo(
            String description,
            String source
    ) {
    }
}