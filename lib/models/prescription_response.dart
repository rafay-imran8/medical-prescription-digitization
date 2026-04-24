import 'package:json_annotation/json_annotation.dart';

part 'prescription_response.g.dart';

/// Main API Response Model from FastAPI
@JsonSerializable()
class PrescriptionApiResponse {
  @JsonKey(name: 'request_id')
  final String requestId;

  @JsonKey(name: 'processing_time_sec')
  final double processingTimeSec;

  final PrescriptionResult result;

  PrescriptionApiResponse({
    required this.requestId,
    required this.processingTimeSec,
    required this.result,
  });

  factory PrescriptionApiResponse.fromJson(Map<String, dynamic> json) =>
      _$PrescriptionApiResponseFromJson(json);

  Map<String, dynamic> toJson() => _$PrescriptionApiResponseToJson(this);
}

/// Main Result object containing all prescription data
@JsonSerializable()
class PrescriptionResult {
  @JsonKey(name: 'patient_info')
  final Map<String, dynamic>? patientInfo;

  final Map<String, dynamic>? vitals;

  @JsonKey(name: 'clinical_info')
  final Map<String, dynamic>? clinicalInfo;

  final List<MedicineResult> prescription;

  @JsonKey(name: 'drug_interactions')
  final List<DrugInteractionResult> drugInteractions;

  @JsonKey(name: 'drug_disease_warnings')
  final List<DrugDiseaseWarning> drugDiseaseWarnings;

  @JsonKey(name: 'duplicate_ingredient_warnings')
  final List<DuplicateIngredientWarning> duplicateIngredientWarnings;

  @JsonKey(name: 'high_severity_count')
  final int highSeverityCount;

  @JsonKey(name: 'moderate_severity_count')
  final int moderateSeverityCount;

  final Map<String, dynamic>? audit;

  PrescriptionResult({
    required this.patientInfo,
    required this.vitals,
    required this.clinicalInfo,
    required this.prescription,
    required this.drugInteractions,
    required this.drugDiseaseWarnings,
    required this.duplicateIngredientWarnings,
    required this.highSeverityCount,
    required this.moderateSeverityCount,
    this.audit,
  });

  factory PrescriptionResult.fromJson(Map<String, dynamic> json) =>
      _$PrescriptionResultFromJson(json);

  Map<String, dynamic> toJson() => _$PrescriptionResultToJson(this);
}

/// Individual Medicine from prescription
@JsonSerializable()
class MedicineResult {
  @JsonKey(name: 'Medicine_Name')
  final String? medicineName;

  @JsonKey(name: 'Medicine_Type')
  final String? medicineType;

  @JsonKey(name: 'Dosage')
  final String? dosage;

  @JsonKey(name: 'Frequency')
  final String? frequency;

  @JsonKey(name: 'Duration_to_take_med')
  final String? duration;

  @JsonKey(name: 'Quantity')
  final String? quantity;

  final String? rxcui;

  @JsonKey(name: 'normalized_name')
  final String? normalizedName;

  @JsonKey(name: 'normalization_confidence')
  final double? normalizationConfidence;

  @JsonKey(name: 'normalization_status')
  final String? normalizationStatus;

  @JsonKey(name: 'normalization_method')
  final String? normalizationMethod;

  MedicineResult({
    required this.medicineName,
    this.medicineType,
    this.dosage,
    this.frequency,
    this.duration,
    this.quantity,
    this.rxcui,
    this.normalizedName,
    this.normalizationConfidence,
    this.normalizationStatus,
    this.normalizationMethod,
  });

  factory MedicineResult.fromJson(Map<String, dynamic> json) =>
      _$MedicineResultFromJson(json);

  Map<String, dynamic> toJson() => _$MedicineResultToJson(this);
}

/// Drug-Drug Interaction Warning
@JsonSerializable()
class DrugInteractionResult {
  @JsonKey(name: 'drug1_name')
  final String? drug1Name;

  @JsonKey(name: 'drug1_rxcui')
  final String? drug1Rxcui;

  @JsonKey(name: 'drug2_name')
  final String? drug2Name;

  @JsonKey(name: 'drug2_rxcui')
  final String? drug2Rxcui;

  final String severity; // HIGH | MODERATE | LOW | UNKNOWN

  final String? description;

  final String? source; // DRUGBANK

  DrugInteractionResult({
    required this.drug1Name,
    required this.drug1Rxcui,
    required this.drug2Name,
    required this.drug2Rxcui,
    required this.severity,
    this.description,
    this.source,
  });

  /// Returns color based on severity level
  String getSeverityColor() {
    switch (severity.toUpperCase()) {
      case 'HIGH':
        return '#d32f2f'; // Red
      case 'MODERATE':
        return '#f57c00'; // Orange
      case 'LOW':
        return '#fbc02d'; // Yellow
      default:
        return '#757575'; // Gray
    }
  }

  factory DrugInteractionResult.fromJson(Map<String, dynamic> json) =>
      _$DrugInteractionResultFromJson(json);

  Map<String, dynamic> toJson() => _$DrugInteractionResultToJson(this);
}

/// Drug-Disease Warning
@JsonSerializable()
class DrugDiseaseWarning {
  @JsonKey(name: 'drugName')
  final String? drugName;

  final String? rxcui;

  @JsonKey(name: 'indicationText')
  final String? indicationText;

  @JsonKey(name: 'meshTerms')
  final List<String>? meshTerms;

  DrugDiseaseWarning({
    required this.drugName,
    this.rxcui,
    this.indicationText,
    this.meshTerms,
  });

  factory DrugDiseaseWarning.fromJson(Map<String, dynamic> json) =>
      _$DrugDiseaseWarningFromJson(json);

  Map<String, dynamic> toJson() => _$DrugDiseaseWarningToJson(this);
}

/// Duplicate Active Ingredient Warning
@JsonSerializable()
class DuplicateIngredientWarning {
  final String? rxcui;

  final List<String> medicines;

  final String message;

  DuplicateIngredientWarning({
    this.rxcui,
    required this.medicines,
    required this.message,
  });

  factory DuplicateIngredientWarning.fromJson(Map<String, dynamic> json) =>
      _$DuplicateIngredientWarningFromJson(json);

  Map<String, dynamic> toJson() => _$DuplicateIngredientWarningToJson(this);
}

/// Local storage model for prescription history
@JsonSerializable()
class PrescriptionRecord {
  final String id;
  final String? imagePath;
  final DateTime uploadedAt;
  final PrescriptionApiResponse apiResponse;
  final String status; // processing, completed, failed

  PrescriptionRecord({
    required this.id,
    this.imagePath,
    required this.uploadedAt,
    required this.apiResponse,
    required this.status,
  });

  factory PrescriptionRecord.fromJson(Map<String, dynamic> json) =>
      _$PrescriptionRecordFromJson(json);

  Map<String, dynamic> toJson() => _$PrescriptionRecordToJson(this);
}
