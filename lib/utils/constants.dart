import 'package:flutter/material.dart';

/// App color scheme
class AppColors {
  // Primary colors (Medical blue theme)
  static const Color primary = Color(0xFF0066CC);
  static const Color primaryLight = Color(0xFF4D94FF);
  static const Color primaryDark = Color(0xFF003D99);

  // Status colors
  static const Color success = Color(0xFF4CAF50);
  static const Color warning = Color(0xFFFFC107);
  static const Color error = Color(0xFFF44336);
  static const Color info = Color(0xFF2196F3);

  // Severity colors
  static const Color severityHigh = Color(0xFFD32F2F); // Red
  static const Color severityModerate = Color(0xFFF57C00); // Orange
  static const Color severityLow = Color(0xFFFBC02D); // Yellow

  // Neutral colors
  static const Color background = Color(0xFFFAFAFA);
  static const Color surface = Color(0xFFFFFFFF);
  static const Color surfaceVariant = Color(0xFFF5F5F5);
  static const Color outline = Color(0xFFDDDDDD);
  static const Color outlineVariant = Color(0xFFEEEEEE);

  // Text colors
  static const Color textPrimary = Color(0xFF212121);
  static const Color textSecondary = Color(0xFF666666);
  static const Color textTertiary = Color(0xFF999999);
  static const Color textDisabled = Color(0xFFBDBDBD);

  // Brand colors
  static const Color brandBg = Color(0xFFF0F7FF);
}

/// App typography
class AppTextStyles {
  static const TextStyle headline1 = TextStyle(
    fontSize: 32,
    fontWeight: FontWeight.bold,
    color: AppColors.textPrimary,
  );

  static const TextStyle headline2 = TextStyle(
    fontSize: 28,
    fontWeight: FontWeight.bold,
    color: AppColors.textPrimary,
  );

  static const TextStyle headline3 = TextStyle(
    fontSize: 24,
    fontWeight: FontWeight.bold,
    color: AppColors.textPrimary,
  );

  static const TextStyle headline4 = TextStyle(
    fontSize: 20,
    fontWeight: FontWeight.bold,
    color: AppColors.textPrimary,
  );

  static const TextStyle bodyLarge = TextStyle(
    fontSize: 16,
    fontWeight: FontWeight.normal,
    color: AppColors.textPrimary,
  );

  static const TextStyle bodyMedium = TextStyle(
    fontSize: 14,
    fontWeight: FontWeight.normal,
    color: AppColors.textPrimary,
  );

  static const TextStyle bodySmall = TextStyle(
    fontSize: 12,
    fontWeight: FontWeight.normal,
    color: AppColors.textSecondary,
  );

  static const TextStyle labelLarge = TextStyle(
    fontSize: 12,
    fontWeight: FontWeight.w600,
    color: AppColors.textPrimary,
  );

  static const TextStyle labelSmall = TextStyle(
    fontSize: 10,
    fontWeight: FontWeight.w600,
    color: AppColors.textSecondary,
  );
}

/// App spacing constants
class AppSpacing {
  static const double xs = 4.0;
  static const double sm = 8.0;
  static const double md = 16.0;
  static const double lg = 24.0;
  static const double xl = 32.0;
  static const double xxl = 48.0;
}

/// Border radius constants
class AppRadius {
  static const double sm = 4.0;
  static const double md = 8.0;
  static const double lg = 12.0;
  static const double xl = 16.0;
  static const double full = 999.0;
}

/// Shadow definitions
class AppShadows {
  static const BoxShadow small = BoxShadow(
    color: Color(0x0A000000),
    blurRadius: 2,
    offset: Offset(0, 1),
  );

  static const BoxShadow medium = BoxShadow(
    color: Color(0x14000000),
    blurRadius: 8,
    offset: Offset(0, 2),
  );

  static const BoxShadow large = BoxShadow(
    color: Color(0x1F000000),
    blurRadius: 16,
    offset: Offset(0, 8),
  );
}

/// String constants for UI
class AppStrings {
  // General
  static const String appName = 'Prescription Digitizer';
  static const String loading = 'Loading...';
  static const String cancel = 'Cancel';
  static const String retry = 'Retry';
  static const String back = 'Back';

  // Upload Screen
  static const String uploadTitle = 'Upload Prescription';
  static const String uploadSubtitle = 'Choose how to upload your prescription';
  static const String takePhoto = 'Take Photo';
  static const String chooseFromGallery = 'Choose from Gallery';
  static const String processingImage = 'Processing Image...';

  // Results Screen
  static const String resultsTitle = 'Analysis Results';
  static const String medicinesSection = 'Medicines';
  static const String warningsSection = 'Warnings';
  static const String interactionsSection = 'Drug Interactions';
  static const String diseaseWarningsSection = 'Disease Warnings';
  static const String duplicateWarningsSection = 'Duplicate Ingredients';

  // Severity Labels
  static const String severityHigh = 'HIGH SEVERITY';
  static const String severityModerate = 'MODERATE SEVERITY';
  static const String severityLow = 'LOW SEVERITY';

  // Error Messages
  static const String errorTitle = 'Error Occurred';
  static const String errorLoadingImage = 'Failed to load image';
  static const String errorUploadingImage = 'Failed to upload image';
  static const String errorProcessing = 'Error processing prescription';
  static const String errorServiceUnavailable =
      'Service is currently unavailable. Please try again later.';
  static const String errorNetwork = 'Network error. Please check your connection.';

  // Success Messages
  static const String successUpload = 'Prescription uploaded successfully';
}
