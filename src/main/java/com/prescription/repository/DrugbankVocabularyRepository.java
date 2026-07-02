package com.prescription.repository;

import com.prescription.entity.DrugbankVocabulary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DrugbankVocabularyRepository extends JpaRepository<DrugbankVocabulary, String> {

    Optional<DrugbankVocabulary> findByRxcui(String rxcui);

    /**
     * Lookup by name — case-insensitive, useful for display enrichment.
     */
    @Query("SELECT v FROM DrugbankVocabulary v WHERE LOWER(v.name) = LOWER(:name)")
    Optional<DrugbankVocabulary> findByNameIgnoreCase(@Param("name") String name);

    /**
     * Batch fetch by list of RxCUIs — used to enrich a full prescription.
     */
    @Query("SELECT v FROM DrugbankVocabulary v WHERE v.rxcui IN :rxcuis")
    List<DrugbankVocabulary> findAllByRxcuiIn(@Param("rxcuis") List<String> rxcuis);
}
