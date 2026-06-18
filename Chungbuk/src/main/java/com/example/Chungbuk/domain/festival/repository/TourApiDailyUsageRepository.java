package com.example.Chungbuk.domain.festival.repository;

import com.example.Chungbuk.domain.festival.entity.TourApiDailyUsage;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TourApiDailyUsageRepository
        extends JpaRepository<TourApiDailyUsage, Long> {

    Optional<TourApiDailyUsage> findByUsageDate(
            LocalDate usageDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select usage
            from TourApiDailyUsage usage
            where usage.usageDate = :usageDate
            """)
    Optional<TourApiDailyUsage> findByUsageDateForUpdate(
            @Param("usageDate") LocalDate usageDate
    );
}