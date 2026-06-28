package com.example.Chungbuk.domain.main.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.Chungbuk.domain.main.dto.response.MainSummaryResponse;
import com.example.Chungbuk.domain.main.dto.response.MainSummaryResponse.FeatureCardResponse;
import com.example.Chungbuk.domain.main.dto.response.MainSummaryResponse.HeroResponse;
import com.example.Chungbuk.domain.main.dto.response.MainSummaryResponse.KeywordResponse;
import com.example.Chungbuk.domain.main.dto.response.MainSummaryResponse.PopularRegionResponse;
import com.example.Chungbuk.domain.main.dto.response.MainSummaryResponse.TodayStatResponse;
import com.example.Chungbuk.domain.main.dto.response.MainSummaryResponse.WeatherRegionResponse;
import com.example.Chungbuk.domain.main.dto.response.MainSummaryResponse.WeatherResponse;
import com.example.Chungbuk.domain.main.service.MainSummaryService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MainControllerTest {

    private MainSummaryService mainSummaryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mainSummaryService = mock(MainSummaryService.class);
        MainController controller = new MainController(mainSummaryService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .build();
    }

    @Test
    void getMainSummaryReturnsFrontendResponseShape() throws Exception {
        when(mainSummaryService.getMainSummary())
                .thenReturn(createResponse());

        mockMvc.perform(get("/api/main"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hero.title").isString())
                .andExpect(jsonPath("$.hero.highlightText").isString())
                .andExpect(jsonPath("$.hero.description").isString())
                .andExpect(jsonPath("$.popularRegions").isArray())
                .andExpect(jsonPath("$.popularRegions[0].id").value("cheongju"))
                .andExpect(jsonPath("$.popularRegions[0].name").value("Cheongju"))
                .andExpect(jsonPath("$.popularRegions[0].placeCount").value(42))
                .andExpect(jsonPath("$.popularRegions[0].href").value("/map?region=CHEONGJU"))
                .andExpect(jsonPath("$.keywords").isArray())
                .andExpect(jsonPath("$.keywords[0].label").value("Rainy day"))
                .andExpect(jsonPath("$.keywords[0].keyword").value("Indoor travel"))
                .andExpect(jsonPath("$.todayStats").isArray())
                .andExpect(jsonPath("$.todayStats[0].value").value(8))
                .andExpect(jsonPath("$.todayStats[0].href").value("/festival"))
                .andExpect(jsonPath("$.weather.primaryRegion").value("Cheongju"))
                .andExpect(jsonPath("$.weather.temperature").value("24°C"))
                .andExpect(jsonPath("$.weather.feelsLike").value("26°C"))
                .andExpect(jsonPath("$.weather.regions").isArray())
                .andExpect(jsonPath("$.weather.regions[0].href").value("/clothing"))
                .andExpect(jsonPath("$.featureCards").isArray())
                .andExpect(jsonPath("$.featureCards[0].href").value("/course"));

        verify(mainSummaryService).getMainSummary();
    }

    private MainSummaryResponse createResponse() {
        return new MainSummaryResponse(
                new HeroResponse(
                        "Chungbuk travel at a glance",
                        "at a glance",
                        "Plan Chungbuk travel easily.",
                        "/images/main-hero.png"
                ),
                List.of(
                        new PopularRegionResponse(
                                "cheongju",
                                "Cheongju",
                                "Good city travel region.",
                                42,
                                "/map?region=CHEONGJU",
                                ""
                        )
                ),
                List.of(
                        new KeywordResponse(
                                "rainy-day",
                                "Rainy day",
                                "Indoor travel",
                                "/map?keyword=Indoor%20travel"
                        )
                ),
                List.of(
                        new TodayStatResponse(
                                "festivals",
                                "Festivals",
                                8,
                                "items",
                                "/festival"
                        )
                ),
                new WeatherResponse(
                        "Cheongju",
                        "24°C",
                        "Cloudy",
                        "26°C",
                        "20%",
                        "60%",
                        "Normal 2.5m/s",
                        "Check weather before traveling.",
                        "/clothing",
                        List.of(
                                new WeatherRegionResponse(
                                        "cheongju-weather",
                                        "Cheongju",
                                        "24°C",
                                        "Cloudy",
                                        "Check weather before traveling.",
                                        "/clothing"
                                )
                        )
                ),
                List.of(
                        new FeatureCardResponse(
                                "course",
                                "AI course",
                                "Travel course",
                                "Recommend a travel course.",
                                "/course",
                                ""
                        )
                )
        );
    }
}
