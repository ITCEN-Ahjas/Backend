package com.example.Chungbuk.domain.accommodation.repository;

import com.example.Chungbuk.domain.accommodation.entity.AccommodationEntity;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccommodationRepository extends JpaRepository<AccommodationEntity, String> {

    List<AccommodationEntity> findAllByDetailSyncedFalse(Pageable pageable);
}
