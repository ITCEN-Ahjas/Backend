package com.example.Chungbuk.domain.main.service;

import com.example.Chungbuk.domain.accommodation.repository.AccommodationRepository;
import com.example.Chungbuk.domain.camping.repository.CampingRepository;
import com.example.Chungbuk.domain.festival.repository.FestivalContentRepository;
import com.example.Chungbuk.domain.main.dto.response.MainSummaryResponse;
import com.example.Chungbuk.domain.main.dto.response.MainSummaryResponse.FeatureCardResponse;
import com.example.Chungbuk.domain.main.dto.response.MainSummaryResponse.HeroResponse;
import com.example.Chungbuk.domain.main.dto.response.MainSummaryResponse.KeywordResponse;
import com.example.Chungbuk.domain.main.dto.response.MainSummaryResponse.PopularRegionResponse;
import com.example.Chungbuk.domain.main.dto.response.MainSummaryResponse.TodayStatResponse;
import com.example.Chungbuk.domain.main.dto.response.MainSummaryResponse.WeatherRegionResponse;
import com.example.Chungbuk.domain.main.dto.response.MainSummaryResponse.WeatherResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MainSummaryService {

    private static final String FESTIVAL_CONTENT_TYPE_ID = "15";

    private final FestivalContentRepository festivalContentRepository;
    private final AccommodationRepository accommodationRepository;
    private final CampingRepository campingRepository;

    @Transactional(readOnly = true)
    public MainSummaryResponse getMainSummary() {
        return new MainSummaryResponse(
                createHero(),
                createPopularRegions(),
                createKeywords(),
                createTodayStats(),
                createDefaultWeather(),
                createFeatureCards()
        );
    }

    private HeroResponse createHero() {
        return new HeroResponse(
                "충북 여행을 한눈에",
                "한눈에",
                "지도 검색부터 체험·축제, 숙박, AI 여행 추천까지 충북 여행을 쉽고 편리하게 계획해보세요.",
                "/images/main-hero.png"
        );
    }

    private List<PopularRegionResponse> createPopularRegions() {
        return List.of(
                new PopularRegionResponse(
                        "cheongju",
                        "청주",
                        "도심 관광, 문화 공간, 음식점을 함께 둘러보기 좋은 지역입니다.",
                        42,
                        "/map?region=CHEONGJU",
                        ""
                ),
                new PopularRegionResponse(
                        "chungju",
                        "충주",
                        "호수와 체험 여행을 함께 계획하기 좋은 지역입니다.",
                        31,
                        "/map?region=CHUNGJU",
                        ""
                ),
                new PopularRegionResponse(
                        "danyang",
                        "단양",
                        "자연 경관과 액티비티 중심 여행에 적합한 지역입니다.",
                        28,
                        "/map?region=DANYANG",
                        ""
                )
        );
    }

    private List<KeywordResponse> createKeywords() {
        return List.of(
                new KeywordResponse(
                        "rainy-day",
                        "비 오는 날",
                        "실내 관광",
                        "/map?keyword=%EC%8B%A4%EB%82%B4%20%EA%B4%80%EA%B4%91"
                ),
                new KeywordResponse(
                        "family",
                        "가족 여행",
                        "체험",
                        "/festival?keyword=%EC%B2%B4%ED%97%98"
                ),
                new KeywordResponse(
                        "food",
                        "맛집",
                        "음식점",
                        "/map?category=RESTAURANT"
                ),
                new KeywordResponse(
                        "stay",
                        "숙소",
                        "숙박",
                        "/lodging"
                )
        );
    }

    private List<TodayStatResponse> createTodayStats() {
        long festivalCount = festivalContentRepository
                .countByContentTypeIdAndActiveTrue(FESTIVAL_CONTENT_TYPE_ID);
        long recommendedPlaceCount = festivalContentRepository.countByActiveTrue()
                + campingRepository.count();
        long accommodationCount = accommodationRepository.count();

        return List.of(
                new TodayStatResponse(
                        "festivals",
                        "진행 중 축제",
                        festivalCount,
                        "개",
                        "/festival"
                ),
                new TodayStatResponse(
                        "places",
                        "추천 장소",
                        recommendedPlaceCount,
                        "곳",
                        "/map"
                ),
                new TodayStatResponse(
                        "lodgings",
                        "숙박 정보",
                        accommodationCount,
                        "곳",
                        "/lodging"
                )
        );
    }

    private WeatherResponse createDefaultWeather() {
        return new WeatherResponse(
                "청주",
                "24°C",
                "구름 많음",
                "26°C",
                "20%",
                "60%",
                "남풍 2.5m/s",
                "가벼운 겉옷과 실내 관광지를 함께 준비하세요.",
                "/clothing",
                List.of(
                        new WeatherRegionResponse(
                                "cheongju-weather",
                                "청주",
                                "24°C",
                                "구름 많음",
                                "가벼운 겉옷과 실내 관광지를 함께 준비하세요.",
                                "/clothing"
                        ),
                        new WeatherRegionResponse(
                                "chungju-weather",
                                "충주",
                                "23°C",
                                "흐림",
                                "호수 주변 산책은 바람을 고려해 계획하세요.",
                                "/clothing"
                        )
                )
        );
    }

    private List<FeatureCardResponse> createFeatureCards() {
        return List.of(
                new FeatureCardResponse(
                        "course",
                        "AI 코스 추천",
                        "날씨와 취향에 맞는 여행 코스",
                        "식사, 관광, 이동 흐름을 고려해 충북 여행 코스를 추천합니다.",
                        "/course",
                        ""
                ),
                new FeatureCardResponse(
                        "map",
                        "지도 검색",
                        "충북 장소를 지도에서 찾기",
                        "지역, 카테고리, 키워드로 관광지와 편의 장소를 탐색합니다.",
                        "/map",
                        ""
                ),
                new FeatureCardResponse(
                        "festival",
                        "축제와 체험",
                        "지금 즐길 수 있는 충북 콘텐츠",
                        "충북의 축제, 체험, 관광 콘텐츠를 한 번에 확인합니다.",
                        "/festival",
                        ""
                ),
                new FeatureCardResponse(
                        "lodging",
                        "숙박",
                        "여행 일정에 맞는 숙소 찾기",
                        "충북 지역 숙박 정보를 확인하고 상세 위치를 볼 수 있습니다.",
                        "/lodging",
                        ""
                )
        );
    }
}
