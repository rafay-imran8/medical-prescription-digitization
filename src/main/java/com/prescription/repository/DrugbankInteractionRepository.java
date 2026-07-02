package com.prescription.repository;

import com.prescription.entity.DrugbankInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for drugbank_schema.drug_interactions — DrugBank reference data.
 * Used exclusively by Phase 4 lookups.
 *
 * DO NOT confuse with DrugInteractionRepository which handles
 * app_schema.drug_interactions (per-prescription saved rows).
 */
@Repository
public interface DrugbankInteractionRepository extends JpaRepository<DrugbankInteraction, Long> {

    @Query(value = """
        SELECT * FROM drugbank_schema.drug_interactions
        WHERE (drug1_rxcui = :rxcui1 AND drug2_rxcui = :rxcui2)
           OR (drug1_rxcui = :rxcui2 AND drug2_rxcui = :rxcui1)
        ORDER BY
            CASE severity
                WHEN 'HIGH'     THEN 1
                WHEN 'MODERATE' THEN 2
                WHEN 'LOW'      THEN 3
                ELSE 4
            END
        """, nativeQuery = true)
    List<DrugbankInteraction> findInteractionBetween(
            @Param("rxcui1") String rxcui1,
            @Param("rxcui2") String rxcui2
    );

    @Query(value = """
        SELECT * FROM drugbank_schema.drug_interactions
        WHERE drug1_rxcui = :rxcui
        ORDER BY
            CASE severity
                WHEN 'HIGH'     THEN 1
                WHEN 'MODERATE' THEN 2
                WHEN 'LOW'      THEN 3
                ELSE 4
            END
        """, nativeQuery = true)
    List<DrugbankInteraction> findByDrug1Rxcui(@Param("rxcui") String rxcui);

    @Query(value = """
        SELECT * FROM drugbank_schema.drug_interactions
        WHERE severity = 'HIGH'
          AND (
            (drug1_rxcui = :rxcui1 AND drug2_rxcui = :rxcui2) OR
            (drug1_rxcui = :rxcui2 AND drug2_rxcui = :rxcui1)
          )
        """, nativeQuery = true)
    List<DrugbankInteraction> findHighSeverityBetween(
            @Param("rxcui1") String rxcui1,
            @Param("rxcui2") String rxcui2
    );
}