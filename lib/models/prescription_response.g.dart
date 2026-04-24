// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'prescription_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

PrescriptionApiResponse _$PrescriptionApiResponseFromJson(
        Map<String, dynamic> json) =>
    PrescriptionApiResponse(
      requestId: json['request_id'] as String,
      processingTimeSec: (json['processing_time_sec'] as num).toDouble(),
      result:
          PrescriptionResult.fromJson(json['result'] as Map<String, dynamic>),
    );

Map<String, dynamic> _$PrescriptionApiResponseToJson(
        PrescriptionApiResponse instance) =>
    <String, dynamic>{
      'request_id': instance.requestId,
      'processing_time_sec': instance.processingTimeSec,
      'result': instance.result,
    };

PrescriptionResult _$PrescriptionResultFromJson(Map<String, dynamic> json) =>
    PrescriptionResult(
      patientInfo: json['patient_info'] as Map<String, dynamic>?,
      vitals: json['vitals'] as Map<String, dynamic>?,
      clinicalInfo: json['clinical_info'] as Map<String, dynamic>?,
      prescription: (json['prescription'] as List<dynamic>)
          .map((e) => MedicineResult.fromJson(e as Map<String, dynamic>))
          .toList(),
      drugInteractions: (json['drug_interactions'] as List<dynamic>)
          .map((e) => DrugInteractionResult.fromJson(e as Map<String, dynamic>))
          .toList(),
      drugDiseaseWarnings: (json['drug_disease_warnings'] as List<dynamic>)
          .map((e) => DrugDiseaseWarning.fromJson(e as Map<String, dynamic>))
          .toList(),
      duplicateIngredientWarnings: (json['duplicate_ingredient_warnings']
              as List<dynamic>)
          .map((e) =>
              DuplicateIngredientWarning.fromJson(e as Map<String, dynamic>))
          .toList(),
      highSeverityCount: (json['high_severity_count'] as num).toInt(),
      moderateSeverityCount: (json['moderate_severity_count'] as num).toInt(),
      audit: json['audit'] as Map<String, dynamic>?,
    );

Map<String, dynamic> _$PrescriptionResultToJson(PrescriptionResult instance) =>
    <String, dynamic>{
      'patient_info': instance.patientInfo,
      'vitals': instance.vitals,
      'clinical_info': instance.clinicalInfo,
      'prescription': instance.prescription,
      'drug_interactions': instance.drugInteractions,
      'drug_disease_warnings': instance.drugDiseaseWarnings,
      'duplicate_ingredient_warnings': instance.duplicateIngredientWarnings,
      'high_severity_count': instance.highSeverityCount,
      'moderate_severity_count': instance.moderateSeverityCount,
      'audit': instance.audit,
    };

MedicineResult _$MedicineResultFromJson(Map<String, dynamic> json) =>
    MedicineResult(
      medicineName: json['Medicine_Name'] as String?,
      medicineType: json['Medicine_Type'] as String?,
      dosage: json['Dosage'] as String?,
      frequency: json['Frequency'] as String?,
      duration: json['Duration_to_take_med'] as String?,
      quantity: json['Quantity'] as String?,
      rxcui: json['rxcui'] as String?,
      normalizedName: json['normalized_name'] as String?,
      normalizationConfidence:
          (json['normalization_confidence'] as num?)?.toDouble(),
      normalizationStatus: json['normalization_status'] as String?,
      normalizationMethod: json['normalization_method'] as String?,
    );

Map<String, dynamic> _$MedicineResultToJson(MedicineResult instance) =>
    <String, dynamic>{
      'Medicine_Name': instance.medicineName,
      'Medicine_Type': instance.medicineType,
      'Dosage': instance.dosage,
      'Frequency': instance.frequency,
      'Duration_to_take_med': instance.duration,
      'Quantity': instance.quantity,
      'rxcui': instance.rxcui,
      'normalized_name': instance.normalizedName,
      'normalization_confidence': instance.normalizationConfidence,
      'normalization_status': instance.normalizationStatus,
      'normalization_method': instance.normalizationMethod,
    };

DrugInteractionResult _$DrugInteractionResultFromJson(
        Map<String, dynamic> json) =>
    DrugInteractionResult(
      drug1Name: json['drug1_name'] as String?,
      drug1Rxcui: json['drug1_rxcui'] as String?,
      drug2Name: json['drug2_name'] as String?,
      drug2Rxcui: json['drug2_rxcui'] as String?,
      severity: json['severity'] as String,
      description: json['description'] as String?,
      source: json['source'] as String?,
    );

Map<String, dynamic> _$DrugInteractionResultToJson(
        DrugInteractionResult instance) =>
    <String, dynamic>{
      'drug1_name': instance.drug1Name,
      'drug1_rxcui': instance.drug1Rxcui,
      'drug2_name': instance.drug2Name,
      'drug2_rxcui': instance.drug2Rxcui,
      'severity': instance.severity,
      'description': instance.description,
      'source': instance.source,
    };

DrugDiseaseWarning _$DrugDiseaseWarningFromJson(Map<String, dynamic> json) =>
    DrugDiseaseWarning(
      drugName: json['drugName'] as String?,
      rxcui: json['rxcui'] as String?,
      indicationText: json['indicationText'] as String?,
      meshTerms: (json['meshTerms'] as List<dynamic>?)
          ?.map((e) => e as String)
          .toList(),
    );

Map<String, dynamic> _$DrugDiseaseWarningToJson(DrugDiseaseWarning instance) =>
    <String, dynamic>{
      'drugName': instance.drugName,
      'rxcui': instance.rxcui,
      'indicationText': instance.indicationText,
      'meshTerms': instance.meshTerms,
    };

DuplicateIngredientWarning _$DuplicateIngredientWarningFromJson(
        Map<String, dynamic> json) =>
    DuplicateIngredientWarning(
      rxcui: json['rxcui'] as String?,
      medicines:
          (json['medicines'] as List<dynamic>).map((e) => e as String).toList(),
      message: json['message'] as String,
    );

Map<String, dynamic> _$DuplicateIngredientWarningToJson(
        DuplicateIngredientWarning instance) =>
    <String, dynamic>{
      'rxcui': instance.rxcui,
      'medicines': instance.medicines,
      'message': instance.message,
    };

PrescriptionRecord _$PrescriptionRecordFromJson(Map<String, dynamic> json) =>
    PrescriptionRecord(
      id: json['id'] as String,
      imagePath: json['imagePath'] as String?,
      uploadedAt: DateTime.parse(json['uploadedAt'] as String),
      apiResponse: PrescriptionApiResponse.fromJson(
          json['apiResponse'] as Map<String, dynamic>),
      status: json['status'] as String,
    );

Map<String, dynamic> _$PrescriptionRecordToJson(PrescriptionRecord instance) =>
    <String, dynamic>{
      'id': instance.id,
      'imagePath': instance.imagePath,
      'uploadedAt': instance.uploadedAt.toIso8601String(),
      'apiResponse': instance.apiResponse,
      'status': instance.status,
    };
