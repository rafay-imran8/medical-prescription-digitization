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

    @Autowired
    private DrugInteractionRepository interactionRepository;

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

    @Transactional
    public PrescriptionResponse uploadAndProcessPrescription(Long patientId, MultipartFile image) throws IOException {
        // Get patient
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        // Save image to storage
        String storedFilename = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
        Path imagePath = Paths.get(imageStoragePath, storedFilename);
        Files.createDirectories(imagePath.getParent());
        Files.write(imagePath, image.getBytes());

        // Create prescription record
        Prescription prescription = new Prescription();
        prescription.setPatient(patient);
        prescription.setPrescriptionType(Prescription.PrescriptionType.SCANNED);
        prescription.setProcessingStatus("PROCESSING");
        prescription.setImagePath(imagePath.toString());
        prescription = prescriptionRepository.save(prescription);

        // Save image metadata
        PrescriptionImage prescriptionImage = new PrescriptionImage();
        prescriptionImage.setPrescription(prescription);
        prescriptionImage.setOriginalFilename(image.getOriginalFilename());
        prescriptionImage.setStoredFilename(storedFilename);
        prescriptionImage.setFilePath(imagePath.toString());
        prescriptionImage.setFileSizeBytes(image.getSize());
        prescriptionImage.setMimeType(image.getContentType());
        imageRepository.save(prescriptionImage);

        try {
            // Call FastAPI for AI processing
            System.out.println("Calling FastAPI for image: " + imagePath);
            JsonNode aiResult = callFastApiForProcessing(imagePath.toFile());
            System.out.println("FastAPI response received: " + aiResult);

            // Check if result exists
            if (!aiResult.has("result")) {
                throw new RuntimeException("Invalid response from AI service: missing 'result' field");
            }

            JsonNode resultNode = aiResult.get("result");

            // Update prescription with patient info
            updatePrescriptionFromAIResult(prescription, resultNode);

            // Save medicines
            saveMedicinesFromAIResult(prescription, resultNode);

            // Save drug interactions
            saveDrugInteractionsFromAIResult(prescription, resultNode);

            // Update processing status
            prescription.setProcessingStatus("COMPLETED");
            prescription = prescriptionRepository.save(prescription);

            System.out.println("Prescription processing completed successfully: " + prescription.getPrescriptionId());

            // Build and return response
            return buildPrescriptionResponse(prescription);

        } catch (Exception e) {
            System.err.println("Error processing prescription: " + e.getMessage());
            e.printStackTrace();

            prescription.setProcessingStatus("FAILED");
            prescriptionRepository.save(prescription);

            throw new RuntimeException("Failed to process prescription: " + e.getMessage(), e);
        }
    }

    private JsonNode callFastApiForProcessing(File imageFile) throws IOException {
        String url = fastApiUrl + "/analyze-prescription";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", new FileSystemResource(imageFile));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("FastAPI returned error: " + response.getStatusCode());
        }

        return objectMapper.readTree(response.getBody());
    }

    private void updatePrescriptionFromAIResult(Prescription prescription, JsonNode resultNode) {
        try {
            // Patient info
            if (resultNode.has("patient_info") && !resultNode.get("patient_info").isNull()) {
                JsonNode patientInfo = resultNode.get("patient_info");

                // Parse date safely
                if (patientInfo.has("Date")) {
                    String dateStr = patientInfo.get("Date").asText();
                    if (dateStr != null && !dateStr.isEmpty()) {
                        try {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("[dd-MM-yyyy][yyyy-MM-dd][MM/dd/yyyy][dd/MM/yyyy]");
                            prescription.setPrescriptionDate(LocalDate.parse(dateStr, formatter));
                        } catch (Exception e) {
                            System.err.println("Failed to parse date: " + dateStr);
                        }
                    }
                }
            }

            // Vitals
            if (resultNode.has("vitals") && !resultNode.get("vitals").isNull()) {
                JsonNode vitals = resultNode.get("vitals");
                prescription.setWeight(vitals.path("Weight").asText(null));
                prescription.setTemperature(vitals.path("Temperature").asText(null));
                prescription.setBloodPressure(vitals.path("Blood_Pressure").asText(null));
            }

            // Clinical info
            if (resultNode.has("clinical_info") && !resultNode.get("clinical_info").isNull()) {
                JsonNode clinicalInfo = resultNode.get("clinical_info");
                prescription.setDiagnosis(clinicalInfo.path("Diagnosis").asText(null));
                prescription.setPatientHistory(clinicalInfo.path("Patient_History").asText(null));
            }

            // Store full LLM-corrected JSON
            prescription.setLlmCorrectedJson(objectMapper.writeValueAsString(resultNode));

            prescriptionRepository.save(prescription);
            System.out.println("Prescription updated with AI data");

        } catch (Exception e) {
            System.err.println("Error updating prescription from AI result: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveMedicinesFromAIResult(Prescription prescription, JsonNode resultNode) {
        try {
            if (!resultNode.has("prescription") || resultNode.get("prescription").isNull()) {
                System.out.println("No medicines found in AI result");
                return;
            }

            JsonNode prescriptionArray = resultNode.get("prescription");
            if (!prescriptionArray.isArray()) {
                System.out.println("Prescription field is not an array");
                return;
            }

            int count = 0;
            for (JsonNode medicineNode : prescriptionArray) {
                PrescriptionMedicine medicine = new PrescriptionMedicine();
                medicine.setPrescription(prescription);

                // Required field
                String medicineName = medicineNode.path("Medicine_Name").asText(null);
                if (medicineName == null || medicineName.isEmpty()) {
                    System.out.println("Skipping medicine with no name");
                    continue;
                }
                medicine.setMedicineName(medicineName);

                // Optional fields
                medicine.setMedicineType(medicineNode.path("Medicine_Type").asText(null));
                medicine.setDosage(medicineNode.path("Dosage").asText(null));
                medicine.setFrequency(medicineNode.path("Frequency").asText(null));
                medicine.setDuration(medicineNode.path("Duration_to_take_med").asText(null));
                medicine.setQuantity(medicineNode.path("Quantity").asText(null));

                // RxNorm normalization data
                medicine.setRxcui(medicineNode.path("rxcui").asText(null));
                medicine.setNormalizedName(medicineNode.path("normalized_name").asText(null));

                // ✅ NEW: Store normalization method
                String method = medicineNode.path("normalization_method").asText(null);
                medicine.setNormalizationMethod(method);

                // Normalization status
                String normalizationStatus = medicineNode.path("normalization_status").asText(null);
                medicine.setNormalizationStatus(normalizationStatus != null ? normalizationStatus.toUpperCase() : "COMPLETED");


                // Confidence
                if (medicineNode.has("normalization_confidence") && !medicineNode.get("normalization_confidence").isNull()) {
                    medicine.setNormalizationConfidence(
                            BigDecimal.valueOf(medicineNode.path("normalization_confidence").asDouble())
                    );
                }

                // ✅ NEW: Store RxNorm alternatives as JSON
                if (medicineNode.has("alternatives") && medicineNode.get("alternatives").isArray()) {
                    medicine.setRxnormAlternatives(medicineNode.get("alternatives").toString());
                }

                medicineRepository.save(medicine);
                count++;
            }

            System.out.println("Saved " + count + " medicines");

        } catch (Exception e) {
            System.err.println("Error saving medicines: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveDrugInteractionsFromAIResult(Prescription prescription, JsonNode resultNode) {
        try {
            if (!resultNode.has("drug_interactions") || resultNode.get("drug_interactions").isNull()) {
                System.out.println("No drug interactions found");
                return;
            }

            JsonNode interactions = resultNode.get("drug_interactions");
            if (!interactions.isArray()) {
                System.out.println("Drug interactions field is not an array");
                return;
            }

            int count = 0;
            for (JsonNode interactionNode : interactions) {
                DrugInteraction interaction = new DrugInteraction();
                interaction.setPrescription(prescription);

                // Drug 1
                if (interactionNode.has("drug1") && !interactionNode.get("drug1").isNull()) {
                    JsonNode drug1 = interactionNode.get("drug1");
                    interaction.setDrug1Rxcui(drug1.path("rxcui").asText(null));
                    interaction.setDrug1Name(drug1.path("name").asText(null));
                }

                // Drug 2
                if (interactionNode.has("drug2") && !interactionNode.get("drug2").isNull()) {
                    JsonNode drug2 = interactionNode.get("drug2");
                    interaction.setDrug2Rxcui(drug2.path("rxcui").asText(null));
                    interaction.setDrug2Name(drug2.path("name").asText(null));
                }

                interaction.setSeverity(interactionNode.path("severity").asText(null));
                interaction.setDescription(interactionNode.path("description").asText(null));

                interactionRepository.save(interaction);
                count++;
            }

            System.out.println("Saved " + count + " drug interactions");

        } catch (Exception e) {
            System.err.println("Error saving drug interactions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getPatientPrescriptions(Long patientId) {
        List<Prescription> prescriptions = prescriptionRepository.findByPatient_PatientIdWithDetails(patientId);
        return prescriptions.stream()
                .map(this::buildPrescriptionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getDoctorPatientPrescriptions(Long doctorId, Long patientId) {
        // Check access
        if (!patientService.hasAccessToPatient(doctorId, patientId)) {
            throw new RuntimeException("Doctor does not have access to this patient's records");
        }

        List<Prescription> prescriptions = prescriptionRepository.findByPatient_PatientIdWithDetails(patientId);
        return prescriptions.stream()
                .map(this::buildPrescriptionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PrescriptionResponse getPrescriptionById(Long prescriptionId, Long requestingUserId, String role) {
        Prescription prescription = prescriptionRepository.findByIdWithDetails(prescriptionId)
                .orElseThrow(() -> new RuntimeException("Prescription not found"));

        // Access control
        if ("PATIENT".equals(role)) {
            Patient patient = patientService.getPatientByUserId(requestingUserId);
            if (!prescription.getPatient().getPatientId().equals(patient.getPatientId())) {
                throw new RuntimeException("Access denied");
            }
        } else if ("DOCTOR".equals(role)) {
            Doctor doctor = doctorRepository.findByUser_UserId(requestingUserId)
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));
            if (!patientService.hasAccessToPatient(doctor.getDoctorId(), prescription.getPatient().getPatientId())) {
                throw new RuntimeException("Access denied");
            }
        }

        return buildPrescriptionResponse(prescription);
    }

    private PrescriptionResponse buildPrescriptionResponse(Prescription prescription) {
        PrescriptionResponse response = new PrescriptionResponse();
        response.setPrescriptionId(prescription.getPrescriptionId());
        response.setPrescriptionType(prescription.getPrescriptionType().name());
        response.setPrescriptionDate(prescription.getPrescriptionDate());
        response.setDiagnosis(prescription.getDiagnosis());
        response.setPatientHistory(prescription.getPatientHistory());
        response.setWeight(prescription.getWeight());
        response.setTemperature(prescription.getTemperature());
        response.setBloodPressure(prescription.getBloodPressure());
        response.setProcessingStatus(prescription.getProcessingStatus());
        response.setCreatedAt(prescription.getCreatedAt());
        response.setImagePath(prescription.getImagePath());

        // Patient info
        Patient patient = prescription.getPatient();
        if (patient != null) {
            response.setPatientInfo(new PrescriptionResponse.PatientInfo(
                    patient.getPatientId(),
                    patient.getPatientUniqueId(),
                    patient.getPatientName(),
                    patient.getAge(),
                    patient.getGender()
            ));
        }

        // Doctor info
        if (prescription.getDoctor() != null) {
            Doctor doctor = prescription.getDoctor();
            if (doctor.getUser() != null) {
                response.setDoctorInfo(new PrescriptionResponse.DoctorInfo(
                        doctor.getDoctorId(),
                        doctor.getDoctorUniqueId(),
                        doctor.getUser().getFullName(),
                        doctor.getSpecialization()
                ));
            }
        }

        // ✅ UPDATED: Medicines with full RxNorm details
        List<PrescriptionMedicine> medicines = medicineRepository.findByPrescription_PrescriptionId(prescription.getPrescriptionId());
        if (medicines != null && !medicines.isEmpty()) {
            response.setMedicines(medicines.stream()
                    .map(med -> {
                        PrescriptionResponse.MedicineInfo info = new PrescriptionResponse.MedicineInfo(
                                med.getMedicineId(),
                                med.getMedicineName(),
                                med.getMedicineType(),
                                med.getDosage(),
                                med.getFrequency(),
                                med.getDuration(),
                                med.getQuantity(),
                                med.getRxcui(),
                                med.getNormalizedName(),
                                med.getNormalizationStatus(),
                                med.getNormalizationConfidence() != null ?
                                        med.getNormalizationConfidence().doubleValue() : null,
                                med.getNormalizationMethod(),
                                parseRxNormAlternatives(med.getRxnormAlternatives())
                        );
                        return info;
                    })
                    .collect(Collectors.toList()));
        } else {
            response.setMedicines(new ArrayList<>());
        }

        // In buildPrescriptionResponse, before return response:
        List<DrugInteraction> interactions = interactionRepository
                .findByPrescription_PrescriptionId(prescription.getPrescriptionId());
        if (interactions != null && !interactions.isEmpty()) {
            response.setInteractions(interactions.stream()
                    .map(i -> new PrescriptionResponse.InteractionInfo(
                            i.getDrug1Name(),
                            i.getDrug2Name(),
                            i.getSeverity(),
                            i.getDescription()
                    ))
                    .collect(Collectors.toList()));
        }

        // ... rest of the method
        return response;
    }

    /**
     * Parse RxNorm alternatives from JSON string
     */
    private List<PrescriptionResponse.RxNormAlternative> parseRxNormAlternatives(String alternativesJson) {
        if (alternativesJson == null || alternativesJson.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            JsonNode alternatives = objectMapper.readTree(alternativesJson);
            List<PrescriptionResponse.RxNormAlternative> result = new ArrayList<>();

            if (alternatives.isArray()) {
                for (JsonNode alt : alternatives) {
                    result.add(new PrescriptionResponse.RxNormAlternative(
                            alt.path("rxcui").asText(null),
                            alt.path("name").asText(null),
                            alt.path("tty").asText(null),
                            alt.has("similarity") ? alt.path("similarity").asDouble() : null
                    ));
                }
            }

            return result;
        } catch (Exception e) {
            System.err.println("Error parsing RxNorm alternatives: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Transactional
    public PrescriptionResponse createDigitalPrescription(Long doctorId, CreatePrescriptionRequest request) {
        try {
            // Verify doctor has access to patient
            if (!patientService.hasAccessToPatient(doctorId, request.getPatientId())) {
                throw new RuntimeException("You do not have access to this patient's records");
            }

            // Get doctor and patient
            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));
            Patient patient = patientRepository.findById(request.getPatientId())
                    .orElseThrow(() -> new RuntimeException("Patient not found"));

            // Create prescription
            Prescription prescription = new Prescription();
            prescription.setDoctor(doctor);
            prescription.setPatient(patient);
            prescription.setPrescriptionType(Prescription.PrescriptionType.DIGITAL);
            prescription.setPrescriptionDate(request.getPrescriptionDate() != null ?
                    request.getPrescriptionDate() : LocalDate.now());
            prescription.setDiagnosis(request.getDiagnosis());
            prescription.setPatientHistory(request.getPatientHistory());
            prescription.setWeight(request.getWeight());
            prescription.setTemperature(request.getTemperature());
            prescription.setBloodPressure(request.getBloodPressure());
            prescription.setProcessingStatus("COMPLETED");

            prescription = prescriptionRepository.save(prescription);

            // Save medicines
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
                    medicine.setNormalizationStatus("MANUAL"); // No RxNorm for digital prescriptions

                    medicineRepository.save(medicine);
                }
            }

            System.out.println("Digital prescription created successfully: " + prescription.getPrescriptionId());

            // Build and return response
            return buildPrescriptionResponse(prescription);

        } catch (Exception e) {
            System.err.println("Error creating digital prescription: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create prescription: " + e.getMessage(), e);
        }
    }

}