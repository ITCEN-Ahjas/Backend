package com.example.Chungbuk.domain.accommodation.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "accommodation_room")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccommodationRoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String roomTitle;
    private String roomSize;
    private String roomCount;
    private String baseCount;
    private String maxCount;
    private String offSeasonMinFee;
    private String offSeasonMaxFee;
    private String peakSeasonMinFee;
    private String peakSeasonMaxFee;
    private String roomImageUrl;
    private String bathFacility;
    private String internet;
    private String airCondition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id")
    private AccommodationEntity accommodation;

    void assignAccommodation(AccommodationEntity accommodation) {
        this.accommodation = accommodation;
    }
}
