package com.example.Chungbuk.domain.accommodation.repository;

import com.example.Chungbuk.domain.accommodation.entity.AccommodationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccommodationRepository extends JpaRepository<AccommodationEntity, String> {
}
