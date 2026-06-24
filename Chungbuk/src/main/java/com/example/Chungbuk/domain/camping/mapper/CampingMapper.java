package com.example.Chungbuk.domain.camping.mapper;

import com.example.Chungbuk.domain.camping.dto.response.CampingDetailResponse;
import com.example.Chungbuk.domain.camping.dto.response.CampingListResponse;
import com.example.Chungbuk.domain.camping.dto.response.CampingSummaryResponse;
import com.example.Chungbuk.domain.camping.entity.CampingEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CampingMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public CampingListResponse toListResponse(String rawJson, int page, int size) {
        try {
            List<JsonNode> items = extractItems(rawJson);

            List<CampingSummaryResponse> summaries = items.stream()
                    .map(this::toSummaryResponse)
                    .toList();

            return CampingListResponse.builder()
                    .items(summaries)
                    .page(page)
                    .size(size)
                    .totalCount(summaries.size())
                    .build();
        } catch (Exception e) {
            return CampingListResponse.builder()
                    .items(List.of())
                    .page(page)
                    .size(size)
                    .totalCount(0)
                    .build();
        }
    }

    public CampingEntity toEntity(JsonNode item) {
        return CampingEntity.builder()
                .contentId(text(item, "contentId"))
                .facltNm(text(item, "facltNm"))
                .lineIntro(text(item, "lineIntro"))
                .intro(text(item, "intro"))
                .featureNm(text(item, "featureNm"))
                .induty(text(item, "induty"))
                .lctCl(text(item, "lctCl"))
                .doNm(text(item, "doNm"))
                .sigunguNm(text(item, "sigunguNm"))
                .addr1(text(item, "addr1"))
                .addr2(text(item, "addr2"))
                .mapX(text(item, "mapX"))
                .mapY(text(item, "mapY"))
                .tel(text(item, "tel"))
                .homepage(text(item, "homepage"))
                .resveUrl(text(item, "resveUrl"))
                .resveCl(text(item, "resveCl"))
                .manageSttus(text(item, "manageSttus"))
                .gnrlSiteCo(toInt(item, "gnrlSiteCo"))
                .autoSiteCo(toInt(item, "autoSiteCo"))
                .glampSiteCo(toInt(item, "glampSiteCo"))
                .caravSiteCo(toInt(item, "caravSiteCo"))
                .toilet(text(item, "toilet"))
                .swrmCo(text(item, "swrmCo"))
                .sbrsCl(text(item, "sbrsCl"))
                .posblFcltyCl(text(item, "posblFcltyCl"))
                .themaEnvrnCl(text(item, "themaEnvrnCl"))
                .animalCmgCl(text(item, "animalCmgCl"))
                .firstImageUrl(text(item, "firstImageUrl"))
                .build();
    }

    public CampingDetailResponse toDetailResponse(CampingEntity entity) {
        return CampingDetailResponse.builder()
                .contentId(entity.getContentId())
                .facltNm(entity.getFacltNm())
                .lineIntro(entity.getLineIntro())
                .intro(entity.getIntro())
                .featureNm(entity.getFeatureNm())
                .induty(entity.getInduty())
                .lctCl(entity.getLctCl())
                .doNm(entity.getDoNm())
                .sigunguNm(entity.getSigunguNm())
                .addr1(entity.getAddr1())
                .addr2(entity.getAddr2())
                .mapX(entity.getMapX())
                .mapY(entity.getMapY())
                .tel(entity.getTel())
                .homepage(entity.getHomepage())
                .resveUrl(entity.getResveUrl())
                .resveCl(entity.getResveCl())
                .manageSttus(entity.getManageSttus())
                .gnrlSiteCo(entity.getGnrlSiteCo())
                .autoSiteCo(entity.getAutoSiteCo())
                .glampSiteCo(entity.getGlampSiteCo())
                .caravSiteCo(entity.getCaravSiteCo())
                .toilet(entity.getToilet())
                .swrmCo(entity.getSwrmCo())
                .sbrsCl(entity.getSbrsCl())
                .posblFcltyCl(entity.getPosblFcltyCl())
                .themaEnvrnCl(entity.getThemaEnvrnCl())
                .animalCmgCl(entity.getAnimalCmgCl())
                .firstImageUrl(entity.getFirstImageUrl())
                .build();
    }

    public List<JsonNode> extractItems(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode itemNode = root.path("response").path("body").path("items").path("item");
            return toItemNodeList(itemNode);
        } catch (Exception e) {
            return List.of();
        }
    }

    public int extractTotalCount(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            return root.path("response").path("body").path("totalCount").asInt(0);
        } catch (Exception e) {
            return 0;
        }
    }

    private CampingSummaryResponse toSummaryResponse(JsonNode item) {
        return CampingSummaryResponse.builder()
                .contentId(text(item, "contentId"))
                .facltNm(text(item, "facltNm"))
                .lineIntro(text(item, "lineIntro"))
                .induty(text(item, "induty"))
                .lctCl(text(item, "lctCl"))
                .sigunguNm(text(item, "sigunguNm"))
                .addr1(text(item, "addr1"))
                .mapX(text(item, "mapX"))
                .mapY(text(item, "mapY"))
                .manageSttus(text(item, "manageSttus"))
                .firstImageUrl(text(item, "firstImageUrl"))
                .build();
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

    private String text(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }

        JsonNode value = node.path(fieldName);

        if (value.isMissingNode() || value.isNull()) {
            return "";
        }

        String result = value.asText().trim();
        return result.equals("null") ? "" : result;
    }

    private Integer toInt(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return 0;
        }

        JsonNode value = node.path(fieldName);

        if (value.isMissingNode() || value.isNull()) {
            return 0;
        }

        try {
            return Integer.parseInt(value.asText().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
