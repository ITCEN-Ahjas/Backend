package com.example.Chungbuk.domain.weather.service;

import com.example.Chungbuk.domain.weather.dto.response.CurrentWeatherResponse;
import com.example.Chungbuk.domain.weather.dto.response.FeelsLikeWeatherResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FeelsLikeWeatherService {

    public FeelsLikeWeatherResponse create(
            CurrentWeatherResponse currentWeather
    ) {
        double temperature = currentWeather.getTemperature();
        double humidity = currentWeather.getHumidity();
        double windSpeed = currentWeather.getWindSpeed();

        double feelsLikeTemperature = calculateFeelsLikeTemperature(
                temperature,
                humidity,
                windSpeed
        );

        double temperatureDifference = round(
                feelsLikeTemperature - temperature
        );

        List<String> factors = createFactors(
                temperature,
                humidity,
                windSpeed,
                currentWeather.getPrecipitationType()
        );

        return new FeelsLikeWeatherResponse(
                feelsLikeTemperature,
                temperatureDifference,
                createDescription(
                        temperatureDifference,
                        currentWeather.getPrecipitationType()
                ),
                createSummary(
                        temperature,
                        feelsLikeTemperature,
                        temperatureDifference
                ),
                createDetail(factors),
                factors
        );
    }

    private double calculateFeelsLikeTemperature(
            double temperature,
            double humidity,
            double windSpeed
    ) {
        if (temperature >= 27.0 && humidity >= 40.0) {
            return round(calculateHeatIndex(temperature, humidity));
        }

        if (temperature <= 10.0 && windSpeed >= 1.3) {
            return round(calculateWindChill(temperature, windSpeed));
        }

        return round(temperature);
    }

    private double calculateHeatIndex(
            double temperature,
            double humidity
    ) {
        double temperatureFahrenheit = temperature * 9.0 / 5.0 + 32.0;

        double heatIndexFahrenheit =
                -42.379
                        + 2.04901523 * temperatureFahrenheit
                        + 10.14333127 * humidity
                        - 0.22475541 * temperatureFahrenheit * humidity
                        - 0.00683783
                        * temperatureFahrenheit
                        * temperatureFahrenheit
                        - 0.05481717 * humidity * humidity
                        + 0.00122874
                        * temperatureFahrenheit
                        * temperatureFahrenheit
                        * humidity
                        + 0.00085282
                        * temperatureFahrenheit
                        * humidity
                        * humidity
                        - 0.00000199
                        * temperatureFahrenheit
                        * temperatureFahrenheit
                        * humidity
                        * humidity;

        return (heatIndexFahrenheit - 32.0) * 5.0 / 9.0;
    }

    private double calculateWindChill(
            double temperature,
            double windSpeed
    ) {
        double windSpeedKilometers = windSpeed * 3.6;
        double windFactor = Math.pow(windSpeedKilometers, 0.16);

        return 13.12
                + 0.6215 * temperature
                - 11.37 * windFactor
                + 0.3965 * temperature * windFactor;
    }

    private List<String> createFactors(
            double temperature,
            double humidity,
            double windSpeed,
            String precipitationType
    ) {
        List<String> factors = new ArrayList<>();

        if (temperature >= 27.0 && humidity >= 40.0) {
            factors.add("높은 기온과 습도");
        }

        if (temperature <= 10.0 && windSpeed >= 1.3) {
            factors.add("바람 영향");
        }

        if (!"강수 없음".equals(precipitationType)
                && !"강수 정보 없음".equals(precipitationType)) {
            factors.add("강수 가능성");
        }

        if (factors.isEmpty()) {
            factors.add("현재 기온");
        }

        return factors;
    }

    private String createDescription(
            double temperatureDifference,
            String precipitationType
    ) {
        if (temperatureDifference >= 2.0) {
            return "습도 영향으로 실제 기온보다 덥게 느껴질 수 있습니다.";
        }

        if (temperatureDifference <= -2.0) {
            return "바람 영향으로 실제 기온보다 춥게 느껴질 수 있습니다.";
        }

        if (!"강수 없음".equals(precipitationType)
                && !"강수 정보 없음".equals(precipitationType)) {
            return "강수 가능성이 있어 우산이나 방수 준비가 필요합니다.";
        }

        return "현재 기온과 비슷하게 느껴집니다.";
    }

    private String createSummary(
            double temperature,
            double feelsLikeTemperature,
            double temperatureDifference
    ) {
        String temperatureText = formatTemperature(temperature);
        String feelsLikeTemperatureText =
                formatTemperature(feelsLikeTemperature);

        if (temperatureDifference <= -1.0) {
            return "현재 기온은 "
                    + temperatureText
                    + "°C지만 체감온도는 "
                    + feelsLikeTemperatureText
                    + "°C로, "
                    + formatTemperature(Math.abs(temperatureDifference))
                    + "°C 더 낮게 느껴질 수 있어요.";
        }

        if (temperatureDifference >= 1.0) {
            return "현재 기온은 "
                    + temperatureText
                    + "°C지만 체감온도는 "
                    + feelsLikeTemperatureText
                    + "°C로, "
                    + formatTemperature(Math.abs(temperatureDifference))
                    + "°C 더 높게 느껴질 수 있어요.";
        }

        return "현재 기온은 "
                + temperatureText
                + "°C이고 체감온도도 "
                + feelsLikeTemperatureText
                + "°C로, 비슷하게 느껴집니다.";
    }

    private String createDetail(List<String> factors) {
        if (factors.contains("바람 영향")
                && factors.contains("강수 가능성")) {
            return "바람과 강수 영향으로 야외에서는 실제 기온보다 "
                    + "더 쌀쌀하게 느껴질 수 있습니다.";
        }

        if (factors.contains("바람 영향")) {
            return "충북의 바람 때문에 외국인 여행자에게는 "
                    + "야외에서 더 쌀쌀하게 느껴질 수 있습니다.";
        }

        if (factors.contains("높은 기온과 습도")) {
            return "높은 기온과 습도 때문에 실제 기온보다 "
                    + "더 덥고 답답하게 느껴질 수 있습니다.";
        }

        if (factors.contains("강수 가능성")) {
            return "비나 눈이 내리면 체감온도가 달라질 수 있으니 "
                    + "우산 또는 방수 준비를 권장합니다.";
        }

        return "현재 기온과 체감온도 차이가 크지 않아 "
                + "비교적 편안하게 느껴질 수 있습니다.";
    }

    private String formatTemperature(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.001) {
            return String.valueOf((long) value);
        }

        return String.valueOf(value);
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}