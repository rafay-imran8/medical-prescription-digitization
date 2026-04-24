# 🚀 Quick Start Guide - Complete System Setup

This guide will help you set up and run the entire medical prescription digitization system in the correct order.

## 📋 Prerequisites Checklist

- [ ] Python 3.8+ installed
- [ ] Java 11+ and Maven installed
- [ ] PostgreSQL 12+ installed and running
- [ ] Flutter SDK 3.0+ installed
- [ ] Git installed
- [ ] 8GB+ RAM recommended
- [ ] GPU (CUDA-capable) recommended for FastAPI

## ⏱️ Estimated Setup Time
- **Total: 1-2 hours** (first time)
- Database: 10 minutes
- FastAPI: 20-30 minutes (model download)
- Spring Boot: 15-20 minutes
- Flutter App: 10-15 minutes

---

## 🔧 Step-by-Step Setup

### STEP 1: Database Setup (PostgreSQL)

```bash
# Navigate to db branch
cd medical-prescription-digitization
git checkout fyp-db

# Create environment file
cat > .env << EOF
DB_HOST=localhost
DB_PORT=5432
DB_NAME=prescriptions_db
DB_USER=postgres
DB_PASSWORD=your_secure_password
EOF

# Install Python dependencies
pip install -r requirements.txt

# Initialize database
python init_db.py

# Verify tables created
psql -U postgres -d prescriptions_db -c "\dt app_schema.*"

# Expected output: Should show 10+ tables
```

**Expected Output:**
```
           List of relations
 Schema |        Name        | Type  | Owner
--------+--------------------+-------+--------
 app_schema | access_tokens      | table | postgres
 app_schema | doctors            | table | postgres
 app_schema | drug_interactions  | table | postgres
 app_schema | llm_corrections    | table | postgres
 app_schema | patient_doctor_access | table | postgres
 app_schema | patients           | table | postgres
 app_schema | prescription_images | table | postgres
 app_schema | prescription_medicines | table | postgres
 app_schema | prescriptions      | table | postgres
 app_schema | processing_logs    | table | postgres
 app_schema | users              | table | postgres
```

---

### STEP 2: FastAPI Service (Port 8000)

```bash
# Navigate to FastAPI branch
cd medical-prescription-digitization
git checkout fyp-fast-api

# Create environment file
cat > .env << EOF
DB_HOST=localhost
DB_PORT=5432
DB_NAME=prescriptions_db
DB_USER=postgres
DB_PASSWORD=your_secure_password
CUDA_VISIBLE_DEVICES=0
EOF

# Create virtual environment (IMPORTANT)
python -m venv venv

# Activate virtual environment
# Windows:
venv\Scripts\activate
# Mac/Linux:
source venv/bin/activate

# Upgrade pip
pip install --upgrade pip

# Install dependencies
pip install -r requirements.txt

# Download/cache models (first time only, ~10-15 GB)
python -c "
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM
import torch

print('Downloading models...')
tokenizer = AutoTokenizer.from_pretrained('google/flan-t5-xl')
model = AutoModelForSeq2SeqLM.from_pretrained('google/flan-t5-xl')
print('✅ Models cached')
"

# Start FastAPI service
python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload

# In another terminal, verify it's running:
# Windows:
curl http://localhost:8000/health
# Mac/Linux:
curl -X GET http://localhost:8000/health

# Expected response:
# {"status":"healthy","database":"connected"}
```

**Logs should show:**
```
✅ Database connection pool initialized
🔧 Loading models on cuda/cpu...
✅ All AI models loaded successfully
INFO:     Application startup complete [uvicorn]
```

**Troubleshooting FastAPI:**
- **"CUDA out of memory"**: Reduce batch size or use CPU: `export CUDA_VISIBLE_DEVICES=-1`
- **"Model download fails"**: Check internet connection and HuggingFace access
- **"Port already in use"**: Use different port: `--port 8001`

---

### STEP 3: Spring Boot Backend (Port 8080)

```bash
# Navigate to Spring Boot branch
cd medical-prescription-digitization
git checkout fyp-backend-spring-boot

# Create application.properties
cat > src/main/resources/application.properties << EOF
spring.application.name=prescription-management
spring.datasource.url=jdbc:postgresql://localhost:5432/prescriptions_db
spring.datasource.username=postgres
spring.datasource.password=your_secure_password
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.database-platform=org.hibernate.dialect.PostgreSQL10Dialect
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

server.port=8080
management.endpoints.web.exposure.include=health,metrics

# FastAPI integration
fastapi.service.url=http://localhost:8000
fastapi.service.analyze-prescription=/analyze-prescription

# Logging
logging.level.root=INFO
logging.level.com.prescription=DEBUG
EOF

# Build Spring Boot application
mvn clean package

# Run Spring Boot service
java -jar target/prescription-management-1.0.0.jar

# Or using Maven
mvn spring-boot:run

# In another terminal, verify:
curl http://localhost:8080/api/health

# Expected response:
# {"status":"UP","database":"connected"}
```

**Note**: Spring Boot waits for FastAPI and Database to be running!

---

### STEP 4: Flutter Application

```bash
# Navigate to Flutter app
cd prescription_app

# Get dependencies
flutter pub get

# Generate JSON serialization code
flutter pub run build_runner build

# List connected devices
flutter devices

# Run on emulator/device
flutter run

# Or run in release mode (better performance)
flutter run --release
```

**First Run Setup:**
```
✅ Generated lib/models/prescription_response.g.dart
✅ Generated lib/providers/prescription_provider.g.dart
✅ Analyzed 42 source files
✅ Built successfully
```

---

## 🧪 End-to-End Testing

### Terminal Setup (3+ terminals recommended)

**Terminal 1: PostgreSQL**
```bash
# PostgreSQL usually runs as a service
# Verify it's running:
psql -U postgres -c "SELECT 1" && echo "✅ PostgreSQL running"
```

**Terminal 2: FastAPI**
```bash
cd medical-prescription-digitization
git checkout fyp-fast-api
source venv/bin/activate  # Mac/Linux
# or venv\Scripts\activate on Windows
python -m uvicorn main:app --host 0.0.0.0 --port 8000
```

**Terminal 3: Spring Boot**
```bash
cd medical-prescription-digitization
git checkout fyp-backend-spring-boot
mvn spring-boot:run
```

**Terminal 4: Flutter**
```bash
cd prescription_app
flutter run
```

### Test Flow

1. **Verify All Services Running:**
   ```bash
   # Test each endpoint
   curl http://localhost:8000/health
   curl http://localhost:8080/api/health
   psql -U postgres -c "SELECT 1"
   ```

2. **Upload Prescription Image:**
   - Open Flutter app
   - Click "Take Photo" or "Choose from Gallery"
   - Select a prescription image
   - Wait 30-60 seconds for processing

3. **Expected Results:**
   - ✅ Medicine list extracted
   - ✅ RxNorm normalization applied
   - ✅ Drug interactions shown (if multiple drugs)
   - ✅ Disease warnings displayed
   - ✅ Severity levels assigned

---

## 🔍 API Testing with cURL

### Test FastAPI Directly

```bash
# Upload prescription image
curl -X POST "http://localhost:8000/analyze-prescription" \
  -H "accept: application/json" \
  -F "image=@/path/to/prescription.jpg"

# Check health
curl http://localhost:8000/health

# Check root endpoint
curl http://localhost:8000/
```

### Test Spring Boot

```bash
# Health check
curl http://localhost:8080/api/health

# Get patient profile (requires authentication)
curl -H "Authorization: Bearer <JWT_TOKEN>" \
  http://localhost:8080/api/patient/profile

# Upload through Spring Boot (requires auth)
curl -X POST "http://localhost:8080/api/patient/prescriptions/upload" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -F "image=@/path/to/prescription.jpg"
```

---

## 📊 Database Verification

```bash
# Connect to database
psql -U postgres -d prescriptions_db

# View all tables
\dt app_schema.*

# Check prescriptions table
SELECT COUNT(*) FROM app_schema.prescriptions;

# Check users
SELECT * FROM app_schema.users;

# Check drug interactions
SELECT * FROM app_schema.drug_interactions LIMIT 5;

# Exit
\q
```

---

## ⚙️ Configuration Reference

### FastAPI (.env)
```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=prescriptions_db
DB_USER=postgres
DB_PASSWORD=secure_password
CUDA_VISIBLE_DEVICES=0
MODEL_CACHE_DIR=./models
```

### Spring Boot (application.properties)
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/prescriptions_db
spring.datasource.username=postgres
spring.datasource.password=secure_password
fastapi.service.url=http://localhost:8000
server.port=8080
```

### Flutter (lib/models/api_models.dart)
```dart
class ApiConfig {
  // Local machine
  static const String fastApiBaseUrl = 'http://localhost:8000';
  
  // Android emulator
  // static const String fastApiBaseUrl = 'http://10.0.2.2:8000';
  
  // Physical device (update IP)
  // static const String fastApiBaseUrl = 'http://192.168.1.100:8000';
}
```

---

## 🆘 Troubleshooting

### FastAPI Issues

**Problem**: "No module named transformers"
```bash
pip install transformers torch torchvision
```

**Problem**: "CUDA out of memory"
```bash
export CUDA_VISIBLE_DEVICES=-1  # Use CPU instead
```

**Problem**: Port 8000 already in use
```bash
# Kill process on port 8000 (Windows)
netstat -ano | findstr :8000
taskkill /PID <PID> /F

# Or use different port
python -m uvicorn main:app --port 8001
```

### Spring Boot Issues

**Problem**: "Connection refused to database"
```bash
# Ensure PostgreSQL is running
pg_isready -h localhost -p 5432

# Or check service status (Windows)
sc query postgresql-x64-15
```

**Problem**: "FastAPI service unavailable"
```bash
# Ensure FastAPI is running
curl http://localhost:8000/health

# Check firewall if accessing from different machine
```

### Flutter Issues

**Problem**: "Connection refused" from Flutter app
```dart
// In lib/models/api_models.dart
// Try with 10.0.2.2 for Android emulator
static const String fastApiBaseUrl = 'http://10.0.2.2:8000';

// For Windows Subsystem for Linux:
// Use host.docker.internal instead
```

**Problem**: Image permission denied
```bash
# iOS: Check Info.plist permissions (done in setup)
# Android: Grant in app settings > Permissions
```

---

## 📈 Performance Monitoring

### Monitor FastAPI
```bash
# Check logs in real-time
tail -f fastapi.log

# Monitor GPU usage (if available)
nvidia-smi watch -n 1
```

### Monitor Spring Boot
```bash
# View logs
tail -f logs/spring.log

# Check memory usage
jps -l  # List Java processes
```

### Monitor Database
```bash
# Connection count
psql -U postgres -d prescriptions_db -c "SELECT count(*) FROM pg_stat_activity;"

# Query performance
psql -U postgres -d prescriptions_db -c "SELECT * FROM pg_stat_statements ORDER BY total_time DESC LIMIT 5;"
```

---

## 🎯 Success Checklist

After setup, verify:
- [ ] PostgreSQL running and accessible
- [ ] FastAPI service responds to /health endpoint
- [ ] Spring Boot service started successfully
- [ ] Flutter app connects without errors
- [ ] Can upload test prescription image
- [ ] Results display correctly
- [ ] No errors in any service logs

---

## 📞 Getting Help

1. **Check Logs**: Review error messages in each service
2. **Verify Ports**: Ensure no port conflicts
3. **Network**: Check connectivity between services
4. **Database**: Verify credentials and schema creation
5. **Dependencies**: Ensure all requirements installed

---

## 🚀 Production Deployment Considerations

- Use HTTPS instead of HTTP
- Set up proper authentication (JWT)
- Use environment variables for secrets
- Configure database backups
- Set up monitoring and alerting
- Use Docker for consistency
- Set up CI/CD pipeline
- Implement rate limiting
- Add comprehensive logging

---

**Happy Prescribing! 🏥💊**
