package com.example.Chungbuk.domain.weather.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KmaApiResponse {

    @JsonProperty("response")
    private KmaResponse response;

    public KmaResponse getResponse() {
        return response;
    }

    public void setResponse(KmaResponse response) {
        this.response = response;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KmaResponse {

        private KmaHeader header;
        private KmaBody body;

        public KmaHeader getHeader() {
            return header;
        }

        public void setHeader(KmaHeader header) {
            this.header = header;
        }

        public KmaBody getBody() {
            return body;
        }

        public void setBody(KmaBody body) {
            this.body = body;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KmaHeader {

        private String resultCode;
        private String resultMsg;

        public String getResultCode() {
            return resultCode;
        }

        public void setResultCode(String resultCode) {
            this.resultCode = resultCode;
        }

        public String getResultMsg() {
            return resultMsg;
        }

        public void setResultMsg(String resultMsg) {
            this.resultMsg = resultMsg;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KmaBody {

        private String dataType;
        private KmaItems items;
        private Integer pageNo;
        private Integer numOfRows;
        private Integer totalCount;

        public String getDataType() {
            return dataType;
        }

        public void setDataType(String dataType) {
            this.dataType = dataType;
        }

        public KmaItems getItems() {
            return items;
        }

        public void setItems(KmaItems items) {
            this.items = items;
        }

        public Integer getPageNo() {
            return pageNo;
        }

        public void setPageNo(Integer pageNo) {
            this.pageNo = pageNo;
        }

        public Integer getNumOfRows() {
            return numOfRows;
        }

        public void setNumOfRows(Integer numOfRows) {
            this.numOfRows = numOfRows;
        }

        public Integer getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(Integer totalCount) {
            this.totalCount = totalCount;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KmaItems {

        private List<KmaWeatherItem> item;

        public List<KmaWeatherItem> getItem() {
            return item;
        }

        public void setItem(List<KmaWeatherItem> item) {
            this.item = item;
        }
    }
}