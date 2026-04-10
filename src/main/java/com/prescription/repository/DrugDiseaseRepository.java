package com.prescription.repository;

import com.prescription.entity.DrugDisease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DrugDiseaseRepository extends JpaRepository<DrugDisease, Long> {

    /**
     * Core pipeline query: get indication + MeSH terms for a given RxCUI.
     */
    List<DrugDisease> findByRxcui(String rxcui);

    /**
     * Batch fetch — all disease records for a list of RxCUIs.
     * Used to build the drug→disease section for a full prescription.
     */
    @Query("SELECT dd FROM DrugDisease dd WHERE dd.rxcui IN :rxcuis")
    List<DrugDisease> findAllByRxcuiIn(@Param("rxcuis") List<String> rxcuis);

    /**
     * MeSH term search — find which drugs in a prescription are indicated
     * for a particular disease category (useful for analyst view).
     * nativeQuery=true required: ANY() is PostgreSQL-specific, not valid JPQL.
     */
    @Query(value = """
        SELECT * FROM drugbank_schema.drug_disease
        WHERE rxcui IN (:rxcuis)
          AND :meshTerm = ANY(mesh_terms)
        """, nativeQuery = true)
    List<DrugDisease> findByRxcuisAndMeshTerm(
            @Param("rxcuis") List<String> rxcuis,
            @Param("meshTerm") String meshTerm
    );
}