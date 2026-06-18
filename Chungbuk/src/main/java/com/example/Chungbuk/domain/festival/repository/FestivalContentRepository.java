package com.example.Chungbuk.domain.festival.repository;

import com.example.Chungbuk.domain.festival.entity.FestivalContent;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FestivalContentRepository extends
        JpaRepository<FestivalContent, Long>,
        JpaSpecificationExecutor<FestivalContent> {

    Optional<FestivalContent> findByContentId(String contentId);

    boolean existsByContentId(String contentId);

    List<FestivalContent> findAllByContentIdIn(Collection<String> contentIds);
}