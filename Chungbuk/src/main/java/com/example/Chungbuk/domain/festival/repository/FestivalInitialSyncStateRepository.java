package com.example.Chungbuk.domain.festival.repository;

import com.example.Chungbuk.domain.festival.entity.FestivalInitialSyncState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FestivalInitialSyncStateRepository
        extends JpaRepository<FestivalInitialSyncState, Long> {
}