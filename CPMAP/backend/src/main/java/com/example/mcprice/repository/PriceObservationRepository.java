package com.example.mcprice.repository;

import com.example.mcprice.domain.PriceObservation;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PriceObservationRepository extends JpaRepository<PriceObservation, Long> {

    List<PriceObservation> findByCompetitorListingIdOrderByCapturedAtDesc(Long competitorListingId);

    java.util.Optional<PriceObservation> findFirstByCompetitorListingIdOrderByCapturedAtDesc(Long competitorListingId);

    long countByObservationStatus(com.example.mcprice.domain.ObservationStatus status);

    @Query("select count(po) from PriceObservation po where po.capturedAt < :threshold and po.observationStatus = 'VALID'")
    long countStaleCandidates(@Param("threshold") OffsetDateTime threshold);

    List<PriceObservation> findByCapturedAtBeforeAndObservationStatus(OffsetDateTime threshold,
            com.example.mcprice.domain.ObservationStatus status);
}
