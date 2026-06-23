package com.example.Chungbuk.domain.accommodation.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "accommodation")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccommodationEntity {

    @Id
    @Column(name = "content_id")
    private String id;

    private String cat1;
    private String cat2;
    private String cat3;

    private String title;
    private String region;
    private String category;

    @Column(length = 500)
    private String address;

    @Column(length = 1000)
    private String imageUrl;

    @ElementCollection
    @CollectionTable(name = "accommodation_image", joinColumns = @JoinColumn(name = "content_id"))
    @OrderColumn(name = "image_order")
    @Column(name = "image_url", length = 1000)
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    private String tel;

    @Column(length = 1000)
    private String homepage;

    @Lob
    private String overview;

    @Lob
    private String description;

    private String descriptionSource;

    private String mapX;
    private String mapY;

    private String checkInTime;
    private String checkOutTime;
    private String parking;
    private String cookingAvailable;
    private String roomCount;
    private String infoCenter;

    @Column(name = "detail_synced", nullable = false)
    @Builder.Default
    private boolean detailSynced = false;

    @OneToMany(mappedBy = "accommodation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AccommodationRoomEntity> rooms = new ArrayList<>();

    public void addRoom(AccommodationRoomEntity room) {
        room.assignAccommodation(this);
        this.rooms.add(room);
    }

    public void markDetailSynced() {
        this.detailSynced = true;
    }

    public void updateDetailFields(
            String cat1,
            String cat2,
            String cat3,
            String title,
            String region,
            String category,
            String address,
            String imageUrl,
            List<String> imageUrls,
            String tel,
            String homepage,
            String overview,
            String description,
            String descriptionSource,
            String mapX,
            String mapY,
            String checkInTime,
            String checkOutTime,
            String parking,
            String cookingAvailable,
            String roomCount,
            String infoCenter
    ) {
        this.cat1 = cat1;
        this.cat2 = cat2;
        this.cat3 = cat3;
        this.title = title;
        this.region = region;
        this.category = category;
        this.address = address;
        this.imageUrl = imageUrl;
        this.imageUrls = imageUrls != null ? new ArrayList<>(imageUrls) : new ArrayList<>();
        this.tel = tel;
        this.homepage = homepage;
        this.overview = overview;
        this.description = description;
        this.descriptionSource = descriptionSource;
        this.mapX = mapX;
        this.mapY = mapY;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.parking = parking;
        this.cookingAvailable = cookingAvailable;
        this.roomCount = roomCount;
        this.infoCenter = infoCenter;
        this.detailSynced = true;
    }

    public void replaceRooms(List<AccommodationRoomEntity> newRooms) {
        this.rooms.clear();
        if (newRooms != null) {
            for (AccommodationRoomEntity room : newRooms) {
                room.assignAccommodation(this);
                this.rooms.add(room);
            }
        }
    }
}
