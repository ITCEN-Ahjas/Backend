package com.example.Chungbuk.domain.weather.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AiOutfitRecommendationResponse {

    private String region;
    private String travelStyle;
    private String source;
    private OutfitCards outfitCards;
    private List<PreparationItem> preparationItems;

    public AiOutfitRecommendationResponse() {
    }

    public AiOutfitRecommendationResponse(
            String region,
            String travelStyle,
            String source,
            OutfitCards outfitCards,
            List<PreparationItem> preparationItems
    ) {
        this.region = region;
        this.travelStyle = travelStyle;
        this.source = source;
        this.outfitCards = outfitCards;
        this.preparationItems = preparationItems;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getTravelStyle() {
        return travelStyle;
    }

    public void setTravelStyle(String travelStyle) {
        this.travelStyle = travelStyle;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public OutfitCards getOutfitCards() {
        return outfitCards;
    }

    public void setOutfitCards(OutfitCards outfitCards) {
        this.outfitCards = outfitCards;
    }

    public List<PreparationItem> getPreparationItems() {
        return preparationItems;
    }

    public void setPreparationItems(
            List<PreparationItem> preparationItems
    ) {
        this.preparationItems = preparationItems;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OutfitCards {

        private OutfitCard outerwear;
        private OutfitCard top;
        private OutfitCard bottom;
        private OutfitCard shoes;

        public OutfitCards() {
        }

        public OutfitCards(
                OutfitCard outerwear,
                OutfitCard top,
                OutfitCard bottom,
                OutfitCard shoes
        ) {
            this.outerwear = outerwear;
            this.top = top;
            this.bottom = bottom;
            this.shoes = shoes;
        }

        public OutfitCard getOuterwear() {
            return outerwear;
        }

        public void setOuterwear(OutfitCard outerwear) {
            this.outerwear = outerwear;
        }

        public OutfitCard getTop() {
            return top;
        }

        public void setTop(OutfitCard top) {
            this.top = top;
        }

        public OutfitCard getBottom() {
            return bottom;
        }

        public void setBottom(OutfitCard bottom) {
            this.bottom = bottom;
        }

        public OutfitCard getShoes() {
            return shoes;
        }

        public void setShoes(OutfitCard shoes) {
            this.shoes = shoes;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OutfitCard {

        private String name;
        private String description;

        public OutfitCard() {
        }

        public OutfitCard(
                String name,
                String description
        ) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PreparationItem {

        private String code;
        private String name;
        private String description;

        public PreparationItem() {
        }

        public PreparationItem(
                String code,
                String name,
                String description
        ) {
            this.code = code;
            this.name = name;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}