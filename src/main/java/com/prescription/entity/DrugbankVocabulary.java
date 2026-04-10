package com.prescription.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "drug_vocabulary", schema = "drugbank_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrugbankVocabulary {

    @Id
    @Column(name = "drugbank_id")
    private String drugbankId;

    @Column(name = "name", nullable = false)
    private String name;

    /**
     * RxCUI from DrugBank external-identifiers.
     * Join key to rxnorm_schema.rxnconso and all drugbank_schema interaction tables.
     */
    @Column(name = "rxcui")
    private String rxcui;

    @Column(name = "drug_type")
    private String drugType;

    @Column(name = "state")
    private String state;

    @Column(name = "synonyms", columnDefinition = "TEXT[]")
    private String[] synonyms;

    @Column(name = "groups", columnDefinition = "TEXT[]")
    private String[] groups;
}
