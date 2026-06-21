package com.example.Chungbuk.domain.weather.service;

import com.example.Chungbuk.domain.weather.constant.ChungbukRegion;
import com.example.Chungbuk.domain.weather.dto.response.CurrentWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.KmaWeatherItem;
import com.example.Chungbuk.global.exception.KmaWeatherApiException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class WeatherDataNormalizeService {

    public CurrentWeatherResponse normalize(
            ChungbukRegion region,
            List<KmaWeatherItem> nowcastItems,
            List<KmaWeatherItem> forecastItems
    ) {
        Map<String, String> nowcastValues = toNowcastValues(nowcastItems);
        Map<String, String> forecastValues = toNearestForecastValues(forecastItems);

        double temperature = getRequiredDouble(
                nowcastValues,
                "T1H",
                region
        );

        int humidity = getOptionalInteger(nowcastValues, "REH", 0);

        double windSpeed = getOptionalDouble(nowcastValues, "WSD", 0.0);

        String precipitationAmount = getValueOrDefault(
                nowcastValues,
                "RN1",
                "강수없음"
        );

        String precipitationCode = getFirstNonBlank(
                nowcastValues.get("PTY"),
                forecastValues.get("PTY")
        );

        String skyCode = forecastValues.get("SKY");

        int precipitationProbability = getOptionalInteger(
                forecastValues,
                "POP",
                0
        );

        String precipitationType = toPrecipitationType(precipitationCode);
        String skyStatus = toSkyStatus(skyCode);
        String weatherCondition = hasPrecipitation(precipitationType)
                ? precipitationType
                : skyStatus;

        return new CurrentWeatherResponse(
                region.getDisplayName(),
                round(temperature),
                humidity,
                round(windSpeed),
                toWindStatus(windSpeed),
                precipitationAmount,
                precipitationType,
                precipitationProbability,
                skyStatus,
                weatherCondition
        );
    }

    private Map<String, String> toNowcastValues(
            List<KmaWeatherItem> nowcastItems
    ) {
        if (nowcastItems == null || nowcastItems.isEmpty()) {
            throw new KmaWeatherApiException(
                    "기상청 초단기실황 데이터가 없습니다."
            );
        }

        return nowcastItems.stream()
                .filter(item -> hasText(item.getCategory()))
                .filter(item -> hasText(item.getObsrValue()))
                .collect(Collectors.toMap(
                        KmaWeatherItem::getCategory,
                        KmaWeatherItem::getObsrValue,
                        (previousValue, currentValue) -> currentValue,
                        LinkedHashMap::new
                ));
    }

    private Map<String, String> toNearestForecastValues(
            List<KmaWeatherItem> forecastItems
    ) {
        if (forecastItems == null || forecastItems.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, List<KmaWeatherItem>> groupedForecastItems =
                forecastItems.stream()
                        .filter(item -> hasText(item.getFcstDate()))
                        .filter(item -> hasText(item.getFcstTime()))
                        .filter(item -> hasText(item.getCategory()))
                        .filter(item -> hasText(item.getFcstValue()))
                        .collect(Collectors.groupingBy(
                                item -> item.getFcstDate() + item.getFcstTime(),
                                TreeMap::new,
                                Collectors.toList()
                        ));

        return groupedForecastItems.values().stream()
                .map(this::toForecastValues)
                .filter(values -> !values.isEmpty())
                .findFirst()
                .orElse(Collections.emptyMap());
    }

    private Map<String, String> toForecastValues(
            List<KmaWeatherItem> forecastItems
    ) {
        return forecastItems.stream()
                .collect(Collectors.toMap(
                        KmaWeatherItem::getCategory,
                        KmaWeatherItem::getFcstValue,
                        (previousValue, currentValue) -> currentValue,
                        LinkedHashMap::new
                ));
    }

    private double getRequiredDouble(
            Map<String, String> values,
            String category,
            ChungbukRegion region
    ) {
        String value = values.get(category);

        if (!hasText(value)) {
            throw new KmaWeatherApiException(
                    region.getDisplayName()
                            + " 지역의 "
                            + category
                            + " 날씨 데이터가 없습니다."
            );
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            throw new KmaWeatherApiException(
                    category + " 날씨 데이터 형식이 올바르지 않습니다.",
                    exception
            );
        }
    }

    private double getOptionalDouble(
            Map<String, String> values,
            String category,
            double defaultValue
    ) {
        String value = values.get(category);

        if (!hasText(value)) {
            return defaultValue;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private int getOptionalInteger(
            Map<String, String> values,
            String category,
            int defaultValue
    ) {
        return (int) Math.round(
                getOptionalDouble(values, category, defaultValue)
        );
    }

    private String getValueOrDefault(
            Map<String, String> values,
            String category,
            String defaultValue
    ) {
        String value = values.get(category);

        return hasText(value) ? value : defaultValue;
    }

    private String getFirstNonBlank(
            String firstValue,
            String secondValue
    ) {
        if (hasText(firstValue)) {
            return firstValue;
        }

        return secondValue;
    }

    private String toPrecipitationType(String precipitationCode) {
        if (!hasText(precipitationCode)) {
            return "강수 없음";
        }

        return switch (precipitationCode) {
            case "0" -> "강수 없음";
            case "1" -> "비";
            case "2" -> "비 또는 눈";
            case "3" -> "눈";
            case "4" -> "소나기";
            case "5" -> "빗방울";
            case "6" -> "빗방울과 눈날림";
            case "7" -> "눈날림";
            default -> "강수 정보 없음";
        };
    }

    private String toSkyStatus(String skyCode) {
        if (!hasText(skyCode)) {
            return "하늘 상태 정보 없음";
        }

        return switch (skyCode) {
            case "1" -> "맑음";
            case "3" -> "구름 많음";
            case "4" -> "흐림";
            default -> "하늘 상태 정보 없음";
        };
    }

    private String toWindStatus(double windSpeed) {
        if (windSpeed < 2.0) {
            return "약함";
        }

        if (windSpeed < 5.0) {
            return "보통";
        }

        return "강함";
    }

    private boolean hasPrecipitation(String precipitationType) {
        return !"강수 없음".equals(precipitationType)
                && !"강수 정보 없음".equals(precipitationType);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}