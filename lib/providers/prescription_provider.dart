import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';
import '../models/prescription_response.dart';
import '../services/api_service.dart';
import '../services/image_picker_service.dart';

/// State class for prescription analysis
class PrescriptionState {
  final bool isLoading;
  final PrescriptionApiResponse? result;
  final String? error;
  final double uploadProgress;

  PrescriptionState({
    this.isLoading = false,
    this.result,
    this.error,
    this.uploadProgress = 0.0,
  });

  /// CopyWith method for creating modified copies
  PrescriptionState copyWith({
    bool? isLoading,
    PrescriptionApiResponse? result,
    String? error,
    double? uploadProgress,
  }) {
    return PrescriptionState(
      isLoading: isLoading ?? this.isLoading,
      result: result ?? this.result,
      error: error ?? this.error,
      uploadProgress: uploadProgress ?? this.uploadProgress,
    );
  }
}

/// Provider for prescription analysis state
final prescriptionProvider =
    StateNotifierProvider<PrescriptionNotifier, PrescriptionState>((ref) {
  return PrescriptionNotifier();
});

/// State notifier for managing prescription analysis state
class PrescriptionNotifier extends StateNotifier<PrescriptionState> {
  PrescriptionNotifier() : super(PrescriptionState());

  final _apiService = PrescriptionApiService();

  /// Upload and analyze prescription image
  Future<void> analyzePrescription(String imagePath) async {
    state = state.copyWith(isLoading: true, error: null, result: null);

    try {
      // Check service health first
      final isHealthy = await _apiService.checkServiceHealth();
      if (!isHealthy) {
        throw Exception('FastAPI service is not available');
      }

      // Upload image for analysis
      final response = await _apiService.uploadPrescriptionImage(
        imagePath: imagePath,
        onProgress: (progress) {
          state = state.copyWith(uploadProgress: progress.progress);
        },
      );

      state = state.copyWith(
        isLoading: false,
        result: response,
        uploadProgress: 1.0,
      );
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        error: e.toString(),
        uploadProgress: 0.0,
      );
    }
  }

  /// Reset state
  void reset() {
    state = PrescriptionState();
  }

  /// Clear error
  void clearError() {
    state = state.copyWith(error: null);
  }
}

/// Provider for selected image
final selectedImageProvider = StateProvider<XFile?>((ref) => null);

/// Provider for image picker service
final imagePickerProvider = Provider((ref) => ImagePickerService());

/// Provider for selected image file provider
final selectedImageFileProvider = FutureProvider<String?>((ref) async {
  final image = ref.watch(selectedImageProvider);
  return image?.path;
});

/// Provider to get prescription summary
final prescriptionSummaryProvider = Provider((ref) {
  final prescription = ref.watch(prescriptionProvider);
  final result = prescription.result?.result;

  if (result == null) return null;

  return {
    'totalMedicines': result.prescription.length,
    'totalInteractions': result.drugInteractions.length,
    'highSeverity': result.highSeverityCount,
    'moderateSeverity': result.moderateSeverityCount,
    'hasDuplicateWarnings': result.duplicateIngredientWarnings.isNotEmpty,
  };
});

/// Provider to check if prescription has warnings
final hasWarningsProvider = Provider((ref) {
  final prescription = ref.watch(prescriptionProvider);
  final result = prescription.result?.result;

  if (result == null) return false;

  return result.drugInteractions.isNotEmpty ||
      result.drugDiseaseWarnings.isNotEmpty ||
      result.duplicateIngredientWarnings.isNotEmpty;
});
