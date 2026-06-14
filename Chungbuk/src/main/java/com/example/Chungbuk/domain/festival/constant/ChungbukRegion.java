package com.example.Chungbuk.domain.festival.constant;

public enum ChungbukRegion {

    ALL("전체"),
    CHEONGJU("청주"),
    CHUNGJU("충주"),
    JECHEON("제천"),
    DANYANG("단양"),
    BOEUN("보은"),
    YEONGDONG("영동"),
    OKCHEON("옥천"),
    GOESAN("괴산"),
    JINCHEON("진천"),
    EUMSEONG("음성"),
    JEUNGPYEONG("증평");

    private final String name;

    ChungbukRegion(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}