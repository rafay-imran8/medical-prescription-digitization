# Prescription Digitization Flutter App

A comprehensive Flutter mobile application for digitizing medical prescriptions using AI-powered OCR, drug normalization, and interaction detection.

## 🎯 Features

- **📸 Image Upload**: Capture prescriptions via camera or select from gallery
- **🤖 AI Analysis**: 4-phase pipeline for prescription processing
  - Phase 1: OCR extraction using YOLO + TrOCR
  - Phase 2: LLM-based text cleaning and normalization
  - Phase 3: RxNorm drug code mapping
  - Phase 4: Drug interaction and disease warning checks
- **⚠️ Smart Warnings**: 
  - Drug-drug interaction detection
  - Drug-disease contraindication warnings
  - Duplicate active ingredient alerts
- **📊 Detailed Results**: Comprehensive prescription analysis with severity levels
- **🎨 Professional UI**: Medical-grade interface with clear visualization

## 🏗️ Architecture

### Service Layer
- **API Service**: RESTful communication with FastAPI backend
- **Image Picker Service**: Camera and gallery integration
- **State Management**: Riverpod for predictable state management

### Data Models
- Complete JSON serialization with type safety
- Structured response models matching backend API
- Comprehensive error handling

### UI/UX
- Material 3 design system
- Responsive layouts for all screen sizes
- Dark/Light theme support ready
- Accessibility-first approach

## 🚀 Prerequisites

### System Requirements
- Flutter SDK 3.0+ ([Install Flutter](https://flutter.dev/docs/get-started/install))
- Dart SDK 3.0+
- Android SDK 21+ or iOS 11+
- An IDE: Android Studio, VS Code, or IntelliJ

### Backend Services (Must be running)
- **FastAPI Service** (Port 8000)
  - GPU support required for AI models
  - YOLO, TrOCR, LLM, RxNorm, DrugBank data
- **Spring Boot API** (Port 8080) - Optional for authenticated uploads
- **PostgreSQL Database** (for Spring Boot)

## 📦 Installation & Setup

### 1. Clone Repository

```bash
cd prescription_app
flutter pub get
```

### 2. Configure Backend Connection

Edit `lib/models/api_models.dart`:

```dart
class ApiConfig {
  // For Android Emulator (default)
  static const String fastApiBaseUrl = 'http://10.0.2.2:8000';
  
  // For Physical Device - replace with your actual IP
  // static const String fastApiBaseUrl = 'http://192.168.1.100:8000';
}
```

**Finding Your Machine IP:**
- **Windows**: `ipconfig` → Look for "IPv4 Address"
- **Mac/Linux**: `ifconfig` → Look for "inet" address
- **Important**: Use the same network for device/emulator

### 3. Install Dependencies

```bash
flutter pub get
```

### 4. Generate JSON Serialization Code

```bash
flutter pub run build_runner build
# or for watch mode (auto-rebuild on file changes)
flutter pub run build_runner watch
```

### 5. Configure Platform Permissions

#### Android
Permissions are already configured in `android/app/src/main/AndroidManifest.xml`

#### iOS
Edit `ios/Runner/Info.plist`:

```xml
<dict>
  <key>NSCameraUsageDescription</key>
  <string>We need camera access to capture prescription images</string>
  
  <key>NSPhotoLibraryUsageDescription</key>
  <string>We need permission to access your photos</string>
  
  <key>NSPhotoLibraryAddOnlyUsageDescription</key>
  <string>We need permission to save images</string>
</dict>
```

### 6. Run the App

```bash
# Run on connected device/emulator
flutter run

# Run on specific device
flutter devices  # List available devices
flutter run -d <device_id>

# Run with debugging
flutter run -v

# Run in release mode (better performance)
flutter run --release
```

## 🔧 Backend Setup

### FastAPI Service

```bash
# Activate Python virtual environment
cd ../medical-prescription-digitization
git checkout fyp-fast-api

# Install dependencies
pip install -r requirements.txt

# Set environment variables
export DB_HOST=localhost
export DB_NAME=prescriptions_db
export DB_USER=postgres
export DB_PASSWORD=your_password

# Run FastAPI service
python -m uvicorn main:app --host 0.0.0.0 --port 8000

# Verify: http://localhost:8000/docs (Swagger UI)
```

### Spring Boot Service (Optional)

```bash
git checkout fyp-backend-spring-boot

# Build
mvn clean package

# Run (requires database and FastAPI running)
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080"

# Verify: http://localhost:8080/api/health
```

### PostgreSQL Database

```bash
# Run database setup
git checkout fyp-db

# Execute initialization
python init_db.py

# Verify tables
psql -U postgres -d prescriptions_db -c "\dt app_schema.*"
```

## 🧪 Testing the Complete Pipeline

### End-to-End Test Flow

1. **Start All Services**:
   ```bash
   # Terminal 1: FastAPI
   cd medical-prescription-digitization && git checkout fyp-fast-api
   python -m uvicorn main:app --host 0.0.0.0 --port 8000
   
   # Terminal 2: Spring Boot (optional)
   cd medical-prescription-digitization && git checkout fyp-backend-spring-boot
   mvn spring-boot:run
   
   # Terminal 3: Flutter App
   cd prescription_app
   flutter run
   ```

2. **Test Upload**:
   - Click "Take Photo" or "Choose from Gallery"
   - Select a prescription image
   - Wait for processing (usually 30-60 seconds)
   - View analysis results

3. **Expected Results**:
   - Medicine list with normalization data
   - Drug interaction warnings (if multiple drugs)
   - Disease contraindication alerts
   - Duplicate ingredient warnings (if any)

### Manual API Testing

```bash
# Test FastAPI directly
curl -X POST "http://localhost:8000/analyze-prescription" \
  -F "image=@/path/to/prescription.jpg"

# Health check
curl http://localhost:8000/health
```

## 📱 App Screens

### Home Screen
- **Purpose**: Image selection interface
- **Features**:
  - Camera capture button
  - Gallery selection button
  - How-it-works guide
  - Tips for best results

### Results Screen
- **Purpose**: Display prescription analysis
- **Sections**:
  - Summary statistics (medicines, interactions, severity counts)
  - Medicine list with details (dosage, frequency, etc.)
  - Drug interaction warnings (expandable, severity-coded)
  - Disease warnings
  - Duplicate ingredient alerts
  - Processing information & disclaimers

## 🏥 Data Models Explained

### PrescriptionApiResponse
```
├── request_id: Unique request identifier
├── processing_time_sec: Total processing duration
└── result: PrescriptionResult
    ├── patient_info: Patient demographics
    ├── vitals: Blood pressure, temperature, weight
    ├── clinical_info: Diagnosis, history
    ├── prescription: List<MedicineResult>
    ├── drug_interactions: List<DrugInteractionResult>
    ├── drug_disease_warnings: List<DrugDiseaseWarning>
    ├── duplicate_ingredient_warnings: List<DuplicateIngredientWarning>
    ├── high_severity_count: Count of HIGH severity issues
    └── moderate_severity_count: Count of MODERATE severity issues
```

### MedicineResult
```
├── medicineName: Original extracted name
├── normalizedName: RxNorm-mapped standard name
├── rxcui: RxNorm Concept Unique Identifier
├── dosage: Recommended dosage
├── frequency: How often to take
├── duration: How long to take
├── quantity: Amount prescribed
├── normalizationConfidence: 0-1 confidence score
├── normalizationMethod: exact_match|supplement_map|no_match|safety_blocked
└── normalizationStatus: completed|failed
```

### DrugInteractionResult
```
├── drug1Name / drug1Rxcui: First medication
├── drug2Name / drug2Rxcui: Second medication
├── severity: HIGH|MODERATE|LOW|UNKNOWN
├── description: Clinical interaction details
└── source: DRUGBANK
```

## 🔒 Security Considerations

- **HTTPS**: Configure HTTPS in production (update API URLs)
- **Data Privacy**: Prescription images stored locally only
- **Authentication**: Use Spring Boot endpoint with JWT for multi-user setup
- **Network**: Use VPN for sensitive medical data transmission
- **No Database**: Flutter app is read-only, all data from backend

## 🐛 Troubleshooting

### Common Issues

**Issue**: "Connection refused" when connecting to FastAPI
```
Solution:
1. Verify FastAPI is running: http://localhost:8000/health
2. Update API base URL for your network setup
3. Check firewall settings
4. Use adb reverse for Android: adb reverse tcp:8000 tcp:8000
```

**Issue**: Camera permission denied
```
Solution:
1. Grant permissions in Android Settings
2. iOS: Delete app and reinstall (permission dialog will appear)
3. Check AndroidManifest.xml / Info.plist configuration
```

**Issue**: "Failed to deserialize JSON"
```
Solution:
1. Verify FastAPI response format matches models
2. Check for null fields in response
3. Run: flutter pub run build_runner build
4. Check app logs: flutter logs
```

**Issue**: Image upload timeout
```
Solution:
1. Check image file size (should be < 50MB)
2. Increase timeout in ApiConfig
3. Verify network connection
4. Try on different network
```

## 📊 Performance Tips

1. **Image Optimization**: 
   - Capture clear, well-lit images
   - Avoid excessive rotation/cropping
   - File size < 10MB recommended

2. **Network Optimization**:
   - Use WiFi instead of cellular when possible
   - Ensure stable 4G+ connection

3. **Battery**:
   - App uses GPU on backend, minimal phone battery
   - Camera and network are main battery consumers

## 📚 Project Structure

```
prescription_app/
├── lib/
│   ├── main.dart                 # App entry point
│   ├── models/
│   │   ├── prescription_response.dart  # API response models
│   │   └── api_models.dart            # Configuration & utilities
│   ├── services/
│   │   ├── api_service.dart      # HTTP client for FastAPI
│   │   └── image_picker_service.dart  # Camera & gallery
│   ├── providers/
│   │   └── prescription_provider.dart  # Riverpod state management
│   ├── screens/
│   │   ├── home_screen.dart      # Upload interface
│   │   └── results_screen.dart   # Analysis results
│   ├── widgets/
│   │   └── prescription_widgets.dart   # Reusable UI components
│   └── utils/
│       └── constants.dart        # Colors, text styles, strings
├── android/                      # Android platform code
├── ios/                          # iOS platform code
├── pubspec.yaml                  # Dependencies
└── analysis_options.yaml         # Linting rules
```

## 🔄 Development Workflow

### Adding New Features

1. **Define Data Model**:
   ```dart
   // lib/models/prescription_response.dart
   @JsonSerializable()
   class NewModel { ... }
   ```

2. **Create Service**:
   ```dart
   // lib/services/new_service.dart
   class NewService { ... }
   ```

3. **Add Provider**:
   ```dart
   // lib/providers/prescription_provider.dart
   final newProvider = Provider((ref) => NewService());
   ```

4. **Build UI**:
   ```dart
   // lib/screens/new_screen.dart
   class NewScreen extends ConsumerWidget { ... }
   ```

5. **Generate JSON**:
   ```bash
   flutter pub run build_runner build
   ```

## 📖 API Documentation

### FastAPI Endpoints

#### POST /analyze-prescription
**Request**: Multipart form data with `image` file
**Response**: PrescriptionApiResponse (see Data Models)
**Timeout**: 300 seconds

#### GET /health
**Response**: `{ "status": "healthy", "database": "connected" }`

### Request/Response Format

All requests use `multipart/form-data` for image uploads.
All responses are JSON with proper error handling.

## 🤝 Contributing

When contributing to this project:

1. Follow Dart style guide
2. Use provider for state management
3. Add widget documentation comments
4. Test on both Android and iOS
5. Run `flutter analyze` before committing
6. Ensure `flutter pub run build_runner build` succeeds

## 📄 License

This project is part of the Medical Prescription Digitization FYP system.

## 📞 Support

For issues or questions:
1. Check troubleshooting section
2. Review application logs: `flutter logs`
3. Verify backend services are running
4. Check network connectivity
5. Consult team documentation

## 🎓 Educational Use

This application is designed for educational purposes and demonstrates:
- Modern Flutter architecture
- State management with Riverpod
- RESTful API integration
- Image processing workflows
- Medical data handling
- Error recovery strategies

---

**Happy Prescribing! 💊**
