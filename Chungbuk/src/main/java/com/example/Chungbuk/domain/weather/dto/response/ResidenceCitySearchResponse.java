package com.example.Chungbuk.domain.weather.dto.response;

public class ResidenceCitySearchResponse {

    private final String city;
    private final String country;
    private final String countryCode;
    private final String admin1;
    private final double latitude;
    private final double longitude;

    public ResidenceCitySearchResponse(
            String city,
            String country,
            String countryCode,
            String admin1,
            double latitude,
            double longitude
    ) {
        this.city = city;
        this.country = country;
        this.countryCode = countryCode;
        this.admin1 = admin1;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getAdmin1() {
        return admin1;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
