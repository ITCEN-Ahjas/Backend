package com.example.Chungbuk.domain.camping.repository;

import com.example.Chungbuk.domain.camping.entity.CampingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampingRepository extends JpaRepository<CampingEntity, String> {
}
