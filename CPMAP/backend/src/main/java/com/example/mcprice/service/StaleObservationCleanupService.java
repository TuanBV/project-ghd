package com.example.mcprice.service;

import com.example.mcprice.domain.ObservationStatus;
import com.example.mcprice.domain.PriceObservation;
import com.example.mcprice.repository.PriceObservationRepository;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Quet va danh dau observation qua han (theo max_observation_age_hours cua policy global) thanh STALE. */
@Service
@RequiredArgsConstructor
public class StaleObservationCleanupService {

    private final PriceObservationRepository priceObservationRepository;
    private final PricePolicyService pricePolicyService;

    @Transactional
    public int markStaleObservations() {
        int maxAgeHours = pricePolicyService.getGlobalPolicy().maxObservationAgeHours();
        OffsetDateTime threshold = OffsetDateTime.now().minusHours(maxAgeHours);
        List<PriceObservation> stale = priceObservationRepository
                .findByCapturedAtBeforeAndObservationStatus(threshold, ObservationStatus.VALID);
        for (PriceObservation observation : stale) {
            observation.setObservationStatus(ObservationStatus.STALE);
        }
        priceObservationRepository.saveAll(stale);
        return stale.size();
    }
}
