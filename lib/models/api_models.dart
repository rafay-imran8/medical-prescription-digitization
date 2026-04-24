/// Enum for API service types
enum ApiServiceType {
  fastapi, // Direct FastAPI endpoint (port 8000)
  springboot, // Through Spring Boot (port 8080, requires auth)
}

/// Configuration for API connections
class ApiConfig {
  // FastAPI Configuration
  static const String fastApiBaseUrl = 'http://10.1.142.55:8000'; // Remote GPU Server
  static const String fastApiAnalyzePrescriptionEndpoint =
      '/analyze-prescription';

  // Spring Boot Configuration
  static const String springBootBaseUrl = 'http://10.1.142.55:8080'; // Remote GPU Server
  static const String springBootUploadEndpoint = '/api/patient/prescriptions/upload';

  // For local testing, use:
  // static const String fastApiBaseUrl = 'http://10.0.2.2:8000'; // Android emulator
  // static const String springBootBaseUrl = 'http://10.0.2.2:8080'; // Android emulator

  // Request Timeouts
  static const int requestTimeoutSeconds = 60;
  static const int uploadTimeoutSeconds = 300; // 5 minutes for large files
}

/// API Error Model
class ApiException implements Exception {
  final String message;
  final int? statusCode;
  final dynamic originalError;

  ApiException({
    required this.message,
    this.statusCode,
    this.originalError,
  });

  @override
  String toString() => 'ApiException: $message (Status: $statusCode)';
}

/// Upload Progress Model
class UploadProgress {
  final int sentBytes;
  final int totalBytes;

  double get progress => totalBytes > 0 ? sentBytes / totalBytes : 0;

  UploadProgress({
    required this.sentBytes,
    required this.totalBytes,
  });
}
