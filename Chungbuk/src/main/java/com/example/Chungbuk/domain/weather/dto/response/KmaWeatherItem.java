package com.example.Chungbuk.domain.weather.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KmaWeatherItem {

    private String baseDate;
    private String baseTime;
    private String category;
    private String nx;
    private String ny;

    private String obsrValue;

    private String fcstDate;
    private String fcstTime;
    private String fcstValue;

    public String getBaseDate() {
        return baseDate;
    }

    public void setBaseDate(String baseDate) {
        this.baseDate = baseDate;
    }

    public String getBaseTime() {
        return baseTime;
    }

    public void setBaseTime(String baseTime) {
        this.baseTime = baseTime;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getNx() {
        return nx;
    }

    public void setNx(String nx) {
        this.nx = nx;
    }

    public String getNy() {
        return ny;
    }

    public void setNy(String ny) {
        this.ny = ny;
    }

    public String getObsrValue() {
        return obsrValue;
    }

    public void setObsrValue(String obsrValue) {
        this.obsrValue = obsrValue;
    }

    public String getFcstDate() {
        return fcstDate;
    }

    public void setFcstDate(String fcstDate) {
        this.fcstDate = fcstDate;
    }

    public String getFcstTime() {
        return fcstTime;
    }

    public void setFcstTime(String fcstTime) {
        this.fcstTime = fcstTime;
    }

    public String getFcstValue() {
        return fcstValue;
    }

    public void setFcstValue(String fcstValue) {
        this.fcstValue = fcstValue;
    }
}