package com.example.Chungbuk.domain.weather.service;

import com.example.Chungbuk.domain.weather.constant.ChungbukRegion;
import com.example.Chungbuk.domain.weather.constant.WeatherTimeSlot;
import com.example.Chungbuk.domain.weather.dto.response.CurrentWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.ForecastWeatherSnapshot;
import com.example.Chungbuk.domain.weather.dto.response.KmaWeatherItem;
import com.example.Chungbuk.global.exception.KmaWeatherApiException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class WeatherDataNormalizeService {

    private static final DateTimeFormatter FORECAST_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    public CurrentWeatherResponse normalize(
            ChungbukRegion region,
            List<KmaWeatherItem> nowcastItems,
            List<KmaWeatherItem> forecastItems
    ) {
        Map<String, String> nowcastValues = toNowcastValues(nowcastItems);
        Map<String, String> forecastValues =
                toNearestForecastValues(forecastItems);

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
                "강수 없음"
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

        return createCurrentWeatherResponse(
                region,
                temperature,
                humidity,
                windSpeed,
                precipitationAmount,
                precipitationCode,
                skyCode,
                precipitationProbability
        );
    }

    public Map<WeatherTimeSlot, ForecastWeatherSnapshot>
    normalizeTimeSlotForecasts(
            ChungbukRegion region,
            List<KmaWeatherItem> forecastItems,
            LocalDateTime referenceDateTime
    ) {
        List<ForecastWeatherSnapshot> snapshots =
                createForecastSnapshots(region, forecastItems);

        List<WeatherTimeSlot> remainingTimeSlots =
                getRemainingTimeSlots(referenceDateTime.toLocalTime());

        LocalDate targetForecastDate;
        List<WeatherTimeSlot> targetTimeSlots;

        if (!remainingTimeSlots.isEmpty()
                && hasTimeSlotForecasts(
                        referenceDateTime.toLocalDate(),
                        snapshots,
                        remainingTimeSlots
                )) {
            targetForecastDate = referenceDateTime.toLocalDate();
            targetTimeSlots = remainingTimeSlots;
        } else {
            targetForecastDate = findNextCompleteForecastDate(
                    region,
                    snapshots,
                    referenceDateTime.toLocalDate().plusDays(1)
            );
            targetTimeSlots = List.of(WeatherTimeSlot.values());
        }

        Map<WeatherTimeSlot, ForecastWeatherSnapshot> result =
                new EnumMap<>(WeatherTimeSlot.class);

        for (WeatherTimeSlot timeSlot : targetTimeSlots) {
            result.put(
                    timeSlot,
                    selectTimeSlotSnapshot(
                            region,
                            timeSlot,
                            snapshots,
                            targetForecastDate
                    )
            );
        }

        return Collections.unmodifiableMap(result);
    }

    private List<WeatherTimeSlot> getRemainingTimeSlots(
            LocalTime referenceTime
    ) {
        return List.of(WeatherTimeSlot.values()).stream()
                .filter(timeSlot -> referenceTime.isBefore(
                        timeSlot.getEndTime()
                ))
                .toList();
    }

    private LocalDate findNextCompleteForecastDate(
            ChungbukRegion region,
            List<ForecastWeatherSnapshot> snapshots,
            LocalDate minimumForecastDate
    ) {
        return snapshots.stream()
                .map(snapshot -> snapshot.getForecastAt().toLocalDate())
                .filter(date -> !date.isBefore(minimumForecastDate))
                .distinct()
                .sorted()
                .filter(date -> hasTimeSlotForecasts(
                        date,
                        snapshots,
                        List.of(WeatherTimeSlot.values())
                ))
                .findFirst()
                .orElseThrow(() -> new KmaWeatherApiException(
                        region.getDisplayName()
                                + " 지역의 다음 날 시간대별 예보 데이터가 없습니다."
                ));
    }

    private boolean hasTimeSlotForecasts(
            LocalDate forecastDate,
            List<ForecastWeatherSnapshot> snapshots,
            List<WeatherTimeSlot> timeSlots
    ) {
        return timeSlots.stream().allMatch(timeSlot -> snapshots.stream()
                .anyMatch(snapshot -> snapshot.getForecastAt()
                        .toLocalDate()
                        .equals(forecastDate)
                        && timeSlot.contains(
                                snapshot.getForecastAt().toLocalTime()
                        )));
    }

    private List<ForecastWeatherSnapshot> createForecastSnapshots(
            ChungbukRegion region,
            List<KmaWeatherItem> forecastItems
    ) {
        if (forecastItems == null || forecastItems.isEmpty()) {
            throw new KmaWeatherApiException(
                    "기상청 단기예보 데이터가 없습니다."
            );
        }

        Map<String, List<KmaWeatherItem>> groupedForecastItems =
                forecastItems.stream()
                        .filter(item -> hasText(item.getFcstDate()))
                        .filter(item -> hasText(item.getFcstTime()))
                        .filter(item -> hasText(item.getCategory()))
                        .filter(item -> hasText(item.getFcstValue()))
                        .collect(Collectors.groupingBy(
                                item -> item.getFcstDate()
                                        + item.getFcstTime(),
                                TreeMap::new,
                                Collectors.toList()
                        ));

        List<ForecastWeatherSnapshot> snapshots = new ArrayList<>();

        for (Map.Entry<String, List<KmaWeatherItem>> entry
                : groupedForecastItems.entrySet()) {
            Map<String, String> forecastValues =
                    toForecastValues(entry.getValue());

            if (!hasText(forecastValues.get("TMP"))) {
                continue;
            }

            snapshots.add(
                    new ForecastWeatherSnapshot(
                            parseForecastDateTime(entry.getKey()),
                            normalizeForecastValues(region, forecastValues)
                    )
            );
        }

        if (snapshots.isEmpty()) {
            throw new KmaWeatherApiException(
                    region.getDisplayName()
                            + " 지역의 시간대별 예보 데이터가 없습니다."
            );
        }

        return List.copyOf(snapshots);
    }

    private ForecastWeatherSnapshot selectTimeSlotSnapshot(
            ChungbukRegion region,
            WeatherTimeSlot timeSlot,
            List<ForecastWeatherSnapshot> snapshots,
            LocalDate targetForecastDate
    ) {
        List<ForecastWeatherSnapshot> candidates = snapshots.stream()
                .filter(snapshot -> snapshot.getForecastAt()
                        .toLocalDate()
                        .equals(targetForecastDate))
                .filter(snapshot -> timeSlot.contains(
                        snapshot.getForecastAt().toLocalTime()
                ))
                .toList();

        if (candidates.isEmpty()) {
            throw new KmaWeatherApiException(
                    region.getDisplayName()
                            + " 지역의 "
                            + timeSlot.getDisplayName()
                            + " 시간대 예보 데이터가 없습니다."
            );
        }

        LocalDateTime representativeDateTime = LocalDateTime.of(
                targetForecastDate,
                timeSlot.getRepresentativeTime()
        );

        return candidates.stream()
                .min((first, second) -> Long.compare(
                        getDistanceMinutes(
                                first.getForecastAt(),
                                representativeDateTime
                        ),
                        getDistanceMinutes(
                                second.getForecastAt(),
                                representativeDateTime
                        )
                ))
                .orElseThrow(() -> new KmaWeatherApiException(
                        region.getDisplayName()
                                + " 지역의 "
                                + timeSlot.getDisplayName()
                                + " 시간대 예보 데이터를 선택할 수 없습니다."
                ));
    }

    private long getDistanceMinutes(
            LocalDateTime first,
            LocalDateTime second
    ) {
        return Math.abs(Duration.between(first, second).toMinutes());
    }

    private CurrentWeatherResponse normalizeForecastValues(
            ChungbukRegion region,
            Map<String, String> forecastValues
    ) {
        double temperature = getRequiredDouble(
                forecastValues,
                "TMP",
                region
        );

        int humidity = getOptionalInteger(
                forecastValues,
                "REH",
                0
        );

        double windSpeed = getOptionalDouble(
                forecastValues,
                "WSD",
                0.0
        );

        String precipitationAmount = getValueOrDefault(
                forecastValues,
                "PCP",
                "강수 없음"
        );

        String precipitationCode = forecastValues.get("PTY");
        String skyCode = forecastValues.get("SKY");

        int precipitationProbability = getOptionalInteger(
                forecastValues,
                "POP",
                0
        );

        return createCurrentWeatherResponse(
                region,
                temperature,
                humidity,
                windSpeed,
                precipitationAmount,
                precipitationCode,
                skyCode,
                precipitationProbability
        );
    }

    private CurrentWeatherResponse createCurrentWeatherResponse(
            ChungbukRegion region,
            double temperature,
            int humidity,
            double windSpeed,
            String precipitationAmount,
            String precipitationCode,
            String skyCode,
            int precipitationProbability
    ) {
        String precipitationType =
                toPrecipitationType(precipitationCode);

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
                                item -> item.getFcstDate()
                                        + item.getFcstTime(),
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

    private LocalDateTime parseForecastDateTime(
            String forecastDateTimeText
    ) {
        try {
            return LocalDateTime.parse(
                    forecastDateTimeText,
                    FORECAST_DATE_TIME_FORMATTER
            );
        } catch (DateTimeParseException exception) {
            throw new KmaWeatherApiException(
                    "기상청 예보 시각 형식이 올바르지 않습니다.",
                    exception
            );
        }
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
