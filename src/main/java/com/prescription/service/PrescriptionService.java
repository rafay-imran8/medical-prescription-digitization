package com.prescription.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prescription.dto.PrescriptionResponse;
import com.prescription.dto.CreatePrescriptionRequest;
import com.prescription.entity.*;
import com.prescription.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PrescriptionService {

    @Value("${fastapi.url:http://localhost:8000}")
    private String fastApiUrl;

    @Value("${image.storage.path:/var/prescription-images}")
    private String imageStoragePath;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PrescriptionMedicineRepository medicineRepository;

    @Autowired
    private PrescriptionImageRepository imageRepository;

    /**
     * app_schema.drug_interactions — per-prescription rows saved at upload time.
     * Used by saveDrugInteractionsFromAIResult() and buildPrescriptionResponse().
     */
    @Autowired
    private DrugInteractionRepository drugInteractionRepository;

    @Autowired
    private DuplicateIngredientWarningRepository duplicateWarningRepository;

    /**
     * drugbank_schema.drug_interactions — DrugBank reference data.
     * Phase 4 runs in Python (FastAPI), so this repo is wired for future
     * Java-side lookups only. Not called in the current pipeline.
     */
    @Autowired
    private DrugbankInteractionRepository drugbankInteractionRepository;

    @Autowired
    private ProcessingLogRepository processingLogRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientService patientService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // =========================================================================
    // Upload + Process (scanned prescription)
    // =========================================================================

    @Transactional
    public PrescriptionResponse uploadAndProcessPrescription(Long patientId, MultipartFile image) throws IOException {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        String storedFilename = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
        Path imagePath = Paths.get(imageStoragePath, storedFilename);
        Files.createDirectories(imagePath.getParent());
        Files.write(imagePath, image.getBytes());

        Prescription prescription = new Prescription();
        prescription.setPatient(patient);
        prescription.setPrescriptionType(Prescription.PrescriptionType.SCANNED);
        prescription.setProcessingStatus("PROCESSING");
        prescription.setImagePath(imagePath.toString());
        prescription = prescriptionRepository.save(prescription);

        PrescriptionImage prescriptionImage = new PrescriptionImage();
        prescriptionImage.setPrescription(prescription);
        prescriptionImage.setOriginalFilename(image.getOriginalFilename());
        prescriptionImage.setStoredFilename(storedFilename);
        prescriptionImage.setFilePath(imagePath.toString());
        prescriptionImage.setFileSizeBytes(image.getSize());
        prescriptionImage.setMimeType(image.getContentType());
        imageRepository.save(prescriptionImage);

        try {
            System.out.println("Calling FastAPI for image: " + imagePath);
            JsonNode aiResult = callFastApiForProcessing(imagePath.toFile());
            System.out.println("FastAPI response received: " + aiResult);

            if (!aiResult.has("result")) {
                throw new RuntimeException("Invalid response from AI service: missing 'result' field");
            }

            JsonNode resultNode = aiResult.get("result");

            updatePrescriptionFromAIResult(prescription, resultNode);
            saveMedicinesFromAIResult(prescription, resultNode);
            saveDrugInteractionsFromAIResult(prescription, resultNode);
            saveDuplicateWarningsFromAIResult(prescription, resultNode);

            prescription.setProcessingStatus("COMPLETED");
            prescription = prescriptionRepository.save(prescription);

            System.out.println("Prescription processing completed: " + prescription.getPrescriptionId());
            return buildPrescriptionResponse(prescription, resultNode);

        } catch (Exception e) {
            System.err.println("Error processing prescription: " + e.getMessage());
            e.printStackTrace();
            prescription.setProcessingStatus("FAILED");
            prescriptionRepository.save(prescription);
            throw new RuntimeException("Failed to process prescription: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // FastAPI call
    // =========================================================================

    private JsonNode callFastApiForProcessing(File imageFile) throws IOException {
        String url = fastApiUrl + "/analyze-prescription";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", new FileSystemResource(imageFile));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, requestEntity, String.class
        );

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("FastAPI returned error: " + response.getStatusCode());
        }

        return objectMapper.readTree(response.getBody());
    }

    // =========================================================================
    // Update prescription fields from AI result
    // =========================================================================

    private void updatePrescriptionFromAIResult(Prescription prescription, JsonNode resultNode) {
        try {
            if (resultNode.has("patient_info") && !resultNode.get("patient_info").isNull()) {
                JsonNode patientInfo = resultNode.get("patient_info");
                if (patientInfo.has("Date")) {
                    String dateStr = patientInfo.get("Date").asText();
                    if (dateStr != null && !dateStr.isEmpty()) {
                        try {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                                    "[dd-MM-yyyy][yyyy-MM-dd][MM/dd/yyyy][dd/MM/yyyy]");
                            prescription.setPrescriptionDate(LocalDate.parse(dateStr, formatter));
                        } catch (Exception e) {
                            System.err.println("Failed to parse date: " + dateStr);
                        }
                    }
                }
            }

            if (resultNode.has("vitals") && !resultNode.get("vitals").isNull()) {
                JsonNode vitals = resultNode.get("vitals");
                prescription.setWeight(vitals.path("Weight").asText(null));
                prescription.setTemperature(vitals.path("Temperature").asText(null));
                prescription.setBloodPressure(vitals.path("Blood_Pressure").asText(null));
            }

            if (resultNode.has("clinical_info") && !resultNode.get("clinical_info").isNull()) {
                JsonNode clinicalInfo = resultNode.get("clinical_info");
                prescription.setDiagnosis(clinicalInfo.path("Diagnosis").asText(null));
                prescription.setPatientHistory(clinicalInfo.path("Patient_History").asText(null));
            }

            prescription.setLlmCorrectedJson(objectMapper.writeValueAsString(resultNode));
            prescriptionRepository.save(prescription);

        } catch (Exception e) {
            System.err.println("Error updating prescription from AI result: " + e.getMessage());
        }
    }

    // =========================================================================
    // Save medicines
    // =========================================================================

    private void saveMedicinesFromAIResult(Prescription prescription, JsonNode resultNode) {
        try {
            if (!resultNode.has("prescription") || resultNode.get("prescription").isNull()) {
                System.out.println("No medicines found in AI result");
                return;
            }

            JsonNode prescriptionArray = resultNode.get("prescription");
            if (!prescriptionArray.isArray()) return;

            int count = 0;
            for (JsonNode medicineNode : prescriptionArray) {
                PrescriptionMedicine medicine = new PrescriptionMedicine();
                medicine.setPrescription(prescription);

                String medicineName = medicineNode.path("Medicine_Name").asText(null);
                if (medicineName == null || medicineName.isEmpty()) continue;

                medicine.setMedicineName(medicineName);
                medicine.setMedicineType(medicineNode.path("Medicine_Type").asText(null));
                medicine.setDosage(medicineNode.path("Dosage").asText(null));
                medicine.setFrequency(medicineNode.path("Frequency").asText(null));
                medicine.setDuration(medicineNode.path("Duration_to_take_med").asText(null));
                medicine.setQuantity(medicineNode.path("Quantity").asText(null));
                medicine.setRxcui(medicineNode.path("rxcui").asText(null));
                medicine.setNormalizedName(medicineNode.path("normalized_name").asText(null));
                medicine.setNormalizationMethod(medicineNode.path("normalization_method").asText(null));

                String normStatus = medicineNode.path("normalization_status").asText(null);
                medicine.setNormalizationStatus(normStatus != null ? normStatus.toUpperCase() : "COMPLETED");

                if (medicineNode.has("normalization_confidence") && !medicineNode.get("normalization_confidence").isNull()) {
                    medicine.setNormalizationConfidence(
                            BigDecimal.valueOf(medicineNode.path("normalization_confidence").asDouble())
                    );
                }

                if (medicineNode.has("alternatives") && medicineNode.get("alternatives").isArray()) {
                    medicine.setRxnormAlternatives(medicineNode.get("alternatives").toString());
                }

                medicineRepository.save(medicine);
                count++;
            }

            System.out.println("Saved " + count + " medicines");

        } catch (Exception e) {
            System.err.println("Error saving medicines: " + e.getMessage());
        }
    }

    // =========================================================================
    // Save drug interactions to app_schema.drug_interactions
    // Phase 4 output is flat: {drug1_name, drug1_rxcui, drug2_name, drug2_rxcui,
    //                          severity, description, source}
    // =========================================================================




    private void saveDuplicateWarningsFromAIResult(Prescription prescription, JsonNode resultNode) {
        try {
            if (!resultNode.has("duplicate_ingredient_warnings")
                    || !resultNode.get("duplicate_ingredient_warnings").isArray()) {
                System.out.println("No duplicate ingredient warnings found");
                return;
            }

            JsonNode warningsArray = resultNode.get("duplicate_ingredient_warnings");
            if (warningsArray.isEmpty()) return;

            int count = 0;
            for (JsonNode n : warningsArray) {
                List<String> names = new ArrayList<>();
                if (n.has("medicines") && n.get("medicines").isArray()) {
                    n.get("medicines").forEach(m -> names.add(m.asText()));
                }

                DuplicateIngredientWarningEntity entity = DuplicateIngredientWarningEntity.builder()
                        .prescription(prescription)
                        .rxcui(n.path("rxcui").asText(null))
                        .message(n.path("message").asText(null))
                        .medicineNames(String.join(",", names))
                        .build();

                duplicateWarningRepository.save(entity);
                count++;
            }
            System.out.println("Saved " + count + " duplicate ingredient warnings");

        } catch (Exception e) {
            System.err.println("Error saving duplicate warnings: " + e.getMessage());
        }
    }

    private void saveDrugInteractionsFromAIResult(Prescription prescription, JsonNode resultNode) {
        try {
            if (!resultNode.has("drug_interactions") || resultNode.get("drug_interactions").isNull()) {
                System.out.println("No drug interactions found");
                return;
            }

            JsonNode interactions = resultNode.get("drug_interactions");
            if (!interactions.isArray()) return;

            int count = 0;
            for (JsonNode interactionNode : interactions) {
                DrugInteraction interaction = new DrugInteraction();
                interaction.setPrescription(prescription);
                interaction.setDrug1Rxcui(interactionNode.path("drug1_rxcui").asText(null));
                interaction.setDrug1Name(interactionNode.path("drug1_name").asText(null));
                interaction.setDrug2Rxcui(interactionNode.path("drug2_rxcui").asText(null));
                interaction.setDrug2Name(interactionNode.path("drug2_name").asText(null));
                interaction.setSeverity(interactionNode.path("severity").asText(null));
                interaction.setDescription(interactionNode.path("description").asText(null));
                interaction.setSource(interactionNode.path("source").asText("DRUGBANK"));

                // Saves to app_schema.drug_interactions (DrugInteraction entity)
                drugInteractionRepository.save(interaction);
                count++;
            }

            System.out.println("Saved " + count + " drug interactions");

        } catch (Exception e) {
            System.err.println("Error saving drug interactions: " + e.getMessage());
        }
    }

    // =========================================================================
    // Read queries
    // =========================================================================

    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getPatientPrescriptions(Long patientId) {
        List<Prescription> prescriptions = prescriptionRepository.findByPatient_PatientIdWithDetails(patientId);
        return prescriptions.stream()
                .map(p -> buildPrescriptionResponse(p, null))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getDoctorPatientPrescriptions(Long doctorId, Long patientId) {
        if (!patientService.hasAccessToPatient(doctorId, patientId)) {
            throw new RuntimeException("Doctor does not have access to this patient's records");
        }
        List<Prescription> prescriptions = prescriptionRepository.findByPatient_PatientIdWithDetails(patientId);
        return prescriptions.stream()
                .map(p -> buildPrescriptionResponse(p, null))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PrescriptionResponse getPrescriptionById(Long prescriptionId, Long requestingUserId, String role) {
        Prescription prescription = prescriptionRepository.findByIdWithDetails(prescriptionId)
                .orElseThrow(() -> new RuntimeException("Prescription not found"));

        if ("PATIENT".equals(role)) {
            Patient patient = patientService.getPatientByUserId(requestingUserId);
            if (!prescription.getPatient().getPatientId().equals(patient.getPatientId())) {
                throw new RuntimeException("Access denied");
            }
        } else if ("DOCTOR".equals(role)) {
            Doctor doctor = doctorRepository.findByUser_UserId(requestingUserId)
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));
            if (!patientService.hasAccessToPatient(doctor.getDoctorId(),
                    prescription.getPatient().getPatientId())) {
                throw new RuntimeException("Access denied");
            }
        }

        return buildPrescriptionResponse(prescription, null);
    }




    // =========================================================================
    // Build response
    // =========================================================================

    private PrescriptionResponse buildPrescriptionResponse(Prescription prescription, JsonNode resultNode) {

        PrescriptionResponse response = PrescriptionResponse.builder()
                .prescriptionId(prescription.getPrescriptionId())
                .status(prescription.getProcessingStatus())
                .createdAt(prescription.getCreatedAt())
                .build();

        Patient patient = prescription.getPatient();
        if (patient != null) {
            response.setPatientName(patient.getPatientName());
        }

        if (prescription.getDoctor() != null && prescription.getDoctor().getUser() != null) {
            response.setDoctorName(prescription.getDoctor().getUser().getFullName());
        }

        response.setImageUrl(prescription.getImagePath());

        // ── Medicines ────────────────────────────────────────────────────────
        List<PrescriptionMedicine> medicines = medicineRepository
                .findByPrescription_PrescriptionId(prescription.getPrescriptionId());

        response.setMedicines(medicines != null && !medicines.isEmpty()
                ? medicines.stream()
                .map(med -> PrescriptionResponse.MedicineResult.builder()
                        .originalName(med.getMedicineName())
                        .normalizedName(med.getNormalizedName())
                        .rxcui(med.getRxcui())
                        .medicineType(med.getMedicineType())
                        .dosage(med.getDosage())
                        .frequency(med.getFrequency())
                        .duration(med.getDuration())
                        .quantity(med.getQuantity())
                        .confidence(med.getNormalizationConfidence() != null
                                ? med.getNormalizationConfidence().doubleValue() : 0.0)
                        .method(med.getNormalizationMethod())
                        .status(med.getNormalizationStatus())
                        .build())
                .collect(Collectors.toList())
                : new ArrayList<>());

        // ── Drug interactions + disease warnings ─────────────────────────────
        List<PrescriptionResponse.DrugInteractionResult> ddiResults     = new ArrayList<>();
        List<PrescriptionResponse.DrugDiseaseWarning>    diseaseWarnings = new ArrayList<>();
        List<PrescriptionResponse.DuplicateIngredientWarning> duplicateWarnings = new ArrayList<>();
        int highCount     = 0;
        int moderateCount = 0;

        // ── Duplicate ingredient warnings ────────────────────────────────────

        if (resultNode != null) {
            // Fresh pipeline run — read directly from FastAPI response JSON
            if (resultNode.has("drug_interactions") && resultNode.get("drug_interactions").isArray()) {
                for (JsonNode n : resultNode.get("drug_interactions")) {
                    String severity = n.path("severity").asText("UNKNOWN");
                    ddiResults.add(PrescriptionResponse.DrugInteractionResult.builder()
                            .drug1Name(n.path("drug1_name").asText(null))
                            .drug1Rxcui(n.path("drug1_rxcui").asText(null))
                            .drug2Name(n.path("drug2_name").asText(null))
                            .drug2Rxcui(n.path("drug2_rxcui").asText(null))
                            .severity(severity)
                            .description(n.path("description").asText(null))
                            .source(n.path("source").asText("DRUGBANK"))
                            .build());
                    if ("HIGH".equals(severity))     highCount++;
                    if ("MODERATE".equals(severity)) moderateCount++;
                }
            }

            if (resultNode.has("drug_disease_warnings") && resultNode.get("drug_disease_warnings").isArray()) {
                for (JsonNode n : resultNode.get("drug_disease_warnings")) {
                    List<String> meshTerms = new ArrayList<>();
                    if (n.has("mesh_terms") && n.get("mesh_terms").isArray()) {
                        n.get("mesh_terms").forEach(t -> meshTerms.add(t.asText()));
                    }
                    diseaseWarnings.add(PrescriptionResponse.DrugDiseaseWarning.builder()
                            .drugName(n.path("drug_name").asText(null))
                            .rxcui(n.path("rxcui").asText(null))
                            .indicationText(n.path("indication_text").asText(null))
                            .meshTerms(meshTerms)
                            .build());
                }
            }

            // ── NEW: duplicate ingredient warnings ───────────────────────────
            // ── NEW: duplicate ingredient warnings ───────────────────────────
            if (resultNode.has("duplicate_ingredient_warnings")
                    && resultNode.get("duplicate_ingredient_warnings").isArray()) {
                for (JsonNode n : resultNode.get("duplicate_ingredient_warnings")) {
                    List<String> duplicateMedicineNames = new ArrayList<>();
                    if (n.has("medicines") && n.get("medicines").isArray()) {
                        n.get("medicines").forEach(m -> duplicateMedicineNames.add(m.asText()));
                    }
                    duplicateWarnings.add(PrescriptionResponse.DuplicateIngredientWarning.builder()
                            .rxcui(n.path("rxcui").asText(null))
                            .medicines(duplicateMedicineNames)
                            .message(n.path("message").asText(null))
                            .build());
                }
            }
            // ─────────────────────────────────────────────────────────────────

        } else {
            // Historical fetch — saved rows from app_schema
            List<DrugInteraction> savedInteractions = drugInteractionRepository
                    .findByPrescription_PrescriptionId(prescription.getPrescriptionId());
            if (savedInteractions != null) {
                for (DrugInteraction i : savedInteractions) {
                    String severity = i.getSeverity() != null ? i.getSeverity() : "UNKNOWN";
                    ddiResults.add(PrescriptionResponse.DrugInteractionResult.builder()
                            .drug1Name(i.getDrug1Name())
                            .drug1Rxcui(i.getDrug1Rxcui())
                            .drug2Name(i.getDrug2Name())
                            .drug2Rxcui(i.getDrug2Rxcui())
                            .severity(severity)
                            .description(i.getDescription())
                            .source(i.getSource() != null ? i.getSource() : "DRUGBANK")
                            .build());
                    if ("HIGH".equals(severity))     highCount++;
                    if ("MODERATE".equals(severity)) moderateCount++;
                }
            }
            // duplicateWarnings stays empty ArrayList — not persisted, only fresh run
            // ── NEW: read persisted duplicate warnings ────────────────────────
            List<DuplicateIngredientWarningEntity> savedDuplicates = duplicateWarningRepository
                    .findByPrescription_PrescriptionId(prescription.getPrescriptionId());
            if (savedDuplicates != null) {
                for (DuplicateIngredientWarningEntity d : savedDuplicates) {
                    List<String> names = (d.getMedicineNames() != null && !d.getMedicineNames().isEmpty())
                            ? List.of(d.getMedicineNames().split(","))
                            : new ArrayList<>();
                    duplicateWarnings.add(PrescriptionResponse.DuplicateIngredientWarning.builder()
                            .rxcui(d.getRxcui())
                            .medicines(names)
                            .message(d.getMessage())
                            .build());
                }
            }
        }

        response.setDrugInteractions(ddiResults);
        response.setDrugDiseaseWarnings(diseaseWarnings);
        response.setDuplicateIngredientWarnings(duplicateWarnings);  // ← NEW
        response.setHighSeverityCount(highCount);
        response.setModerateSeverityCount(moderateCount);

        return response;
    }

    // =========================================================================
    // Create digital prescription (doctor-entered, no OCR)
    // =========================================================================

    @Transactional
    public PrescriptionResponse createDigitalPrescription(Long doctorId, CreatePrescriptionRequest request) {
        try {
            if (!patientService.hasAccessToPatient(doctorId, request.getPatientId())) {
                throw new RuntimeException("You do not have access to this patient's records");
            }

            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));
            Patient patient = patientRepository.findById(request.getPatientId())
                    .orElseThrow(() -> new RuntimeException("Patient not found"));

            Prescription prescription = new Prescription();
            prescription.setDoctor(doctor);
            prescription.setPatient(patient);
            prescription.setPrescriptionType(Prescription.PrescriptionType.DIGITAL);
            prescription.setPrescriptionDate(request.getPrescriptionDate() != null
                    ? request.getPrescriptionDate() : LocalDate.now());
            prescription.setDiagnosis(request.getDiagnosis());
            prescription.setPatientHistory(request.getPatientHistory());
            prescription.setWeight(request.getWeight());
            prescription.setTemperature(request.getTemperature());
            prescription.setBloodPressure(request.getBloodPressure());
            prescription.setProcessingStatus("COMPLETED");
            prescription = prescriptionRepository.save(prescription);

            if (request.getMedicines() != null && !request.getMedicines().isEmpty()) {
                for (CreatePrescriptionRequest.MedicineRequest medReq : request.getMedicines()) {
                    PrescriptionMedicine medicine = new PrescriptionMedicine();
                    medicine.setPrescription(prescription);
                    medicine.setMedicineName(medReq.getMedicineName());
                    medicine.setMedicineType(medReq.getMedicineType());
                    medicine.setDosage(medReq.getDosage());
                    medicine.setFrequency(medReq.getFrequency());
                    medicine.setDuration(medReq.getDuration());
                    medicine.setQuantity(medReq.getQuantity());
                    medicine.setNormalizationStatus("MANUAL");
                    medicineRepository.save(medicine);
                }
            }

            System.out.println("Digital prescription created: " + prescription.getPrescriptionId());
            return buildPrescriptionResponse(prescription, null);

        } catch (Exception e) {
            System.err.println("Error creating digital prescription: " + e.getMessage());
            throw new RuntimeException("Failed to create prescription: " + e.getMessage(), e);
        }
    }
}