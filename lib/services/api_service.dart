import 'package:dio/dio.dart';
import 'package:logger/logger.dart';
import '../models/api_models.dart';
import '../models/prescription_response.dart';

/// Service for handling API communications with FastAPI backend
class PrescriptionApiService {
  static final PrescriptionApiService _instance =
      PrescriptionApiService._internal();
  late Dio _dio;
  final Logger _logger = Logger();

  factory PrescriptionApiService() {
    return _instance;
  }

  PrescriptionApiService._internal() {
    _initializeDio();
  }

  /// Initialize Dio HTTP client with configuration
  void _initializeDio() {
    _dio = Dio(
      BaseOptions(
        baseUrl: ApiConfig.fastApiBaseUrl,
        connectTimeout: Duration(seconds: ApiConfig.requestTimeoutSeconds),
        receiveTimeout: Duration(seconds: ApiConfig.uploadTimeoutSeconds),
        contentType: 'multipart/form-data',
        validateStatus: (status) => status != null && status < 500,
      ),
    );

    // Add request interceptor for logging
    _dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          _logger.i('🚀 [REQUEST] ${options.method} ${options.uri}');
          return handler.next(options);
        },
        onResponse: (response, handler) {
          _logger.i(
              '✅ [RESPONSE] ${response.statusCode} - ${response.requestOptions.uri}');
          return handler.next(response);
        },
        onError: (error, handler) {
          _logger.e('❌ [ERROR] ${error.message}');
          return handler.next(error);
        },
      ),
    );
  }

  /// Upload prescription image to FastAPI for analysis
  /// 
  /// Parameters:
  /// - [imagePath]: Path to the image file
  /// - [onProgress]: Callback for upload progress
  /// 
  /// Returns: [PrescriptionApiResponse] with analysis results
  /// 
  /// Throws: [ApiException] on failure
  Future<PrescriptionApiResponse> uploadPrescriptionImage({
    required String imagePath,
    Function(UploadProgress)? onProgress,
  }) async {
    try {
      _logger.d('📤 Uploading prescription from: $imagePath');

      // Create form data with image
      final formData = FormData.fromMap({
        'image': await MultipartFile.fromFile(imagePath),
      });

      // Upload with progress tracking
      final response = await _dio.post(
        ApiConfig.fastApiAnalyzePrescriptionEndpoint,
        data: formData,
        onSendProgress: (sent, total) {
          final progress = UploadProgress(sentBytes: sent, totalBytes: total);
          _logger.d('📊 Upload Progress: ${(progress.progress * 100).toStringAsFixed(1)}%');
          onProgress?.call(progress);
        },
      );

      // Handle response
      if (response.statusCode == 200) {
        _logger.i('✅ Upload successful');
        return PrescriptionApiResponse.fromJson(response.data);
      } else {
        throw ApiException(
          message: 'Upload failed: ${response.data['detail'] ?? 'Unknown error'}',
          statusCode: response.statusCode,
        );
      }
    } on DioException catch (e) {
      _logger.e('❌ Dio Error: ${e.message}');
      throw ApiException(
        message: _parseDioError(e),
        statusCode: e.response?.statusCode,
        originalError: e,
      );
    } catch (e) {
      _logger.e('❌ Unexpected Error: $e');
      throw ApiException(
        message: 'Unexpected error: $e',
        originalError: e,
      );
    }
  }

  /// Check health status of FastAPI service
  Future<bool> checkServiceHealth() async {
    try {
      final response = await _dio.get('/health');
      return response.statusCode == 200;
    } catch (e) {
      _logger.e('❌ Health check failed: $e');
      return false;
    }
  }

  /// Parse Dio errors into user-friendly messages
  String _parseDioError(DioException error) {
    switch (error.type) {
      case DioExceptionType.connectionTimeout:
        return 'Connection timeout. Please check your internet connection.';
      case DioExceptionType.sendTimeout:
        return 'Request timeout. Server took too long to respond.';
      case DioExceptionType.receiveTimeout:
        return 'Response timeout. Server is taking too long to process.';
      case DioExceptionType.badResponse:
        return 'Server error: ${error.response?.statusCode}';
      case DioExceptionType.cancel:
        return 'Request was cancelled.';
      case DioExceptionType.unknown:
        return 'Network error. Please check your connection.';
      default:
        return 'An error occurred: ${error.message}';
    }
  }

  /// Cancel ongoing requests
  void cancelRequests() {
    // Dio 5.x: Use close() or CancelToken for request cancellation
    // For now, we'll just mark as cancelled
    // Future: Implement proper request cancellation with CancelToken
  }

  /// Dispose resources
  void dispose() {
    _dio.close();
  }
}
