package com.prescription.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "drug_disease", schema = "drugbank_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrugDisease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "drugbank_id")
    private String drugbankId;

    /**
     * RxCUI — join key used by the pipeline lookup.
     */
    @Column(name = "rxcui")
    private String rxcui;

    @Column(name = "drug_name")
    private String drugName;

    /**
     * Raw <indication> paragraph from DrugBank XML.
     */
    @Column(name = "indication_text", columnDefinition = "TEXT")
    private String indicationText;

    /**
     * MeSH category terms array from <categories> in DrugBank XML.
     */
    @Column(name = "mesh_terms", columnDefinition = "TEXT[]")
    private String[] meshTerms;
}
