import 'package:image_picker/image_picker.dart';
import 'package:logger/logger.dart';

/// Service for handling image selection from camera or gallery
class ImagePickerService {
  static final ImagePickerService _instance = ImagePickerService._internal();
  final ImagePicker _picker = ImagePicker();
  final Logger _logger = Logger();

  factory ImagePickerService() {
    return _instance;
  }

  ImagePickerService._internal();

  /// Pick image from device gallery
  /// 
  /// Returns: [XFile] with image path, or null if cancelled
  Future<XFile?> pickImageFromGallery() async {
    try {
      _logger.d('📱 Opening gallery');
      final image = await _picker.pickImage(
        source: ImageSource.gallery,
        imageQuality: 95, // Good quality for OCR
      );

      if (image != null) {
        _logger.i('✅ Image selected from gallery: ${image.name}');
      } else {
        _logger.d('❌ Gallery cancelled');
      }

      return image;
    } catch (e) {
      _logger.e('❌ Gallery error: $e');
      rethrow;
    }
  }

  /// Capture image using device camera
  /// 
  /// Returns: [XFile] with image path, or null if cancelled
  Future<XFile?> captureImageWithCamera() async {
    try {
      _logger.d('📷 Opening camera');
      final image = await _picker.pickImage(
        source: ImageSource.camera,
        imageQuality: 95, // Good quality for OCR
      );

      if (image != null) {
        _logger.i('✅ Image captured: ${image.name}');
      } else {
        _logger.d('❌ Camera cancelled');
      }

      return image;
    } catch (e) {
      _logger.e('❌ Camera error: $e');
      rethrow;
    }
  }

  /// Pick image with option to choose source (camera or gallery)
  /// 
  /// Parameters:
  /// - [useCamera]: If true, use camera; if false, use gallery
  /// 
  /// Returns: [XFile] with image path, or null if cancelled
  Future<XFile?> pickImage({required bool useCamera}) async {
    return useCamera ? captureImageWithCamera() : pickImageFromGallery();
  }
}
