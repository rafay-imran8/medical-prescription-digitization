# 🚀 COMPLETE SYSTEM RUN GUIDE
## All Commands Ready to Copy-Paste

---

## 📋 Prerequisites Check

```bash
# Verify Python
python --version  # Should be 3.8+

# Verify Java
java -version  # Should be 11+

# Verify PostgreSQL
psql --version  # Should be 12+

# Verify Flutter
flutter --version  # Should be 3.0+

# Verify Git
git --version
```

---

## 🗄️ STEP 1: DATABASE SETUP (10 minutes)

### 1a: Navigate to DB Branch
```bash
cd c:\Users\rafay\OneDrive\Desktop\flutter-fyp\medical-prescription-digitization

git checkout fyp-db
```

### 1b: Create Environment File
```bash
# Windows (PowerShell)
@"
DB_HOST=localhost
DB_PORT=5432
DB_NAME=prescriptions_db
DB_USER=postgres
DB_PASSWORD=password123
"@ | Out-File -Encoding UTF8 .env

# Mac/Linux
cat > .env << EOF
DB_HOST=localhost
DB_PORT=5432
DB_NAME=prescriptions_db
DB_USER=postgres
DB_PASSWORD=password123
EOF
```

### 1c: Create Database
```bash
# Make sure PostgreSQL is running
psql -U postgres -c "CREATE DATABASE prescriptions_db;"

# Or use pgAdmin GUI
```

### 1d: Initialize Schema
```bash
# Install dependencies
pip install -r requirements.txt

# Run database setup
python init_db.py
```

### 1e: Verify
```bash
psql -U postgres -d prescriptions_db -c "\dt app_schema.*"

# Should show 11 tables
```

---

## 🤖 STEP 2: FASTAPI SERVICE (30 minutes)

### 2a: Navigate to FastAPI Branch
```bash
cd c:\Users\rafay\OneDrive\Desktop\flutter-fyp\medical-prescription-digitization

git checkout fyp-fast-api
```

### 2b: Create Environment File
```bash
# Windows (PowerShell)
@"
DB_HOST=localhost
DB_PORT=5432
DB_NAME=prescriptions_db
DB_USER=postgres
DB_PASSWORD=password123
CUDA_VISIBLE_DEVICES=0
"@ | Out-File -Encoding UTF8 .env

# Mac/Linux
cat > .env << EOF
DB_HOST=localhost
DB_PORT=5432
DB_NAME=prescriptions_db
DB_USER=postgres
DB_PASSWORD=password123
CUDA_VISIBLE_DEVICES=0
EOF
```

### 2c: Create Virtual Environment
```bash
# Windows
python -m venv venv
venv\Scripts\activate

# Mac/Linux
python3 -m venv venv
source venv/bin/activate
```

### 2d: Install Dependencies
```bash
pip install --upgrade pip

pip install -r requirements.txt

# This may take 10-15 minutes
```

### 2e: Cache Models (First Time Only)
```bash
# Download all AI models (OPTIONAL - done on first run anyway)
python -c "
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM
import torch
print('Downloading models...')
tokenizer = AutoTokenizer.from_pretrained('google/flan-t5-xl')
model = AutoModelForSeq2SeqLM.from_pretrained('google/flan-t5-xl')
print('✅ Models cached successfully')
"
```

### 2f: Start FastAPI
```bash
# Important: Keep virtual environment activated!

python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

### 2g: Verify in Another Terminal
```bash
# Windows
curl http://localhost:8000/health

# Mac/Linux
curl -X GET http://localhost:8000/health

# Expected:
# {"status":"healthy","database":"connected"}
```

**Leave this terminal running! Open a new one for next step.**

---

## ☕ STEP 3: SPRING BOOT SERVICE (20 minutes)

### 3a: Navigate to Spring Boot Branch
```bash
cd c:\Users\rafay\OneDrive\Desktop\flutter-fyp\medical-prescription-digitization

git checkout fyp-backend-spring-boot
```

### 3b: Create Configuration File
```bash
# Windows (PowerShell)
@"
spring.application.name=prescription-management
spring.datasource.url=jdbc:postgresql://localhost:5432/prescriptions_db
spring.datasource.username=postgres
spring.datasource.password=password123
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQL10Dialect
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true
server.port=8080
management.endpoints.web.exposure.include=health,metrics
fastapi.service.url=http://localhost:8000
fastapi.service.analyze-prescription=/analyze-prescription
logging.level.root=INFO
logging.level.com.prescription=DEBUG
"@ | Out-File -Encoding UTF8 src\main\resources\application.properties

# Mac/Linux
cat > src/main/resources/application.properties << EOF
spring.application.name=prescription-management
spring.datasource.url=jdbc:postgresql://localhost:5432/prescriptions_db
spring.datasource.username=postgres
spring.datasource.password=password123
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQL10Dialect
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true
server.port=8080
management.endpoints.web.exposure.include=health,metrics
fastapi.service.url=http://localhost:8000
fastapi.service.analyze-prescription=/analyze-prescription
logging.level.root=INFO
logging.level.com.prescription=DEBUG
EOF
```

### 3c: Build
```bash
mvn clean package

# First build will download dependencies (~200MB)
# This takes 10-15 minutes
```

### 3d: Start Spring Boot
```bash
# Option 1: Using Maven
mvn spring-boot:run

# Option 2: Using JAR (faster if already built)
java -jar target/prescription-management-1.0.0.jar

# If version differs, use:
# java -jar target/prescription-management-*.jar
```

### 3e: Verify in Another Terminal
```bash
# Windows
curl http://localhost:8080/api/health

# Mac/Linux
curl -X GET http://localhost:8080/api/health

# Expected:
# {"status":"UP","database":"connected"}
```

**Leave this terminal running! Open a new one for Flutter.**

---

## 📱 STEP 4: FLUTTER APP (5 minutes)

### 4a: Navigate to Flutter App
```bash
cd c:\Users\rafay\OneDrive\Desktop\flutter-fyp\prescription_app
```

### 4b: Get Dependencies
```bash
flutter pub get
```

### 4c: Generate JSON Serialization
```bash
flutter pub run build_runner build

# Or watch mode for auto-rebuild:
# flutter pub run build_runner watch
```

### 4d: List Available Devices
```bash
flutter devices
```

### 4e: Run the App
```bash
# Run on default device
flutter run

# Run on specific device
flutter run -d <device_id>

# Run in release mode (better performance)
flutter run --release

# Run with debugging
flutter run -v
```

---

## 🧪 STEP 5: TESTING (Everything should work now!)

### 5a: Verify All Services Running

```bash
# In a new terminal, test all endpoints:

# 1. FastAPI health
curl http://localhost:8000/health

# 2. Spring Boot health
curl http://localhost:8080/api/health

# 3. PostgreSQL
psql -U postgres -d prescriptions_db -c "SELECT 1"

# All should respond successfully
```

### 5b: Manual API Test
```bash
# Upload a prescription image to FastAPI directly
curl -X POST "http://localhost:8000/analyze-prescription" \
  -F "image=@C:\path\to\prescription.jpg"

# Or test health endpoint
curl http://localhost:8000/health
```

### 5c: Test Full Flow in Flutter App
```
1. Open Flutter app
2. Click "Take Photo" or "Choose from Gallery"
3. Select/capture a prescription image
4. Wait 30-60 seconds for processing
5. Review results screen
6. Check for warnings
```

---

## 🔍 TERMINAL LAYOUT (Recommended)

Run these in separate terminals:

```
Terminal 1: PostgreSQL
  └─ psql -U postgres -d prescriptions_db
     Or just verify it's running: psql -U postgres -c "SELECT 1"

Terminal 2: FastAPI
  └─ cd medical-prescription-digitization
  └─ git checkout fyp-fast-api
  └─ source venv/bin/activate (or venv\Scripts\activate)
  └─ python -m uvicorn main:app --host 0.0.0.0 --port 8000

Terminal 3: Spring Boot
  └─ cd medical-prescription-digitization
  └─ git checkout fyp-backend-spring-boot
  └─ mvn spring-boot:run

Terminal 4: Flutter
  └─ cd prescription_app
  └─ flutter run

Terminal 5: Testing/Monitoring
  └─ curl http://localhost:8000/health
  └─ curl http://localhost:8080/api/health
  └─ flutter logs
```

---

## 🐛 QUICK TROUBLESHOOTING

### Issue: "Port 8000 already in use"
```bash
# Kill the process
# Windows
netstat -ano | findstr :8000
taskkill /PID <PID> /F

# Mac/Linux
lsof -i :8000
kill -9 <PID>

# Or use different port
python -m uvicorn main:app --port 8001
```

### Issue: "Database connection refused"
```bash
# Check PostgreSQL is running
psql -U postgres -c "SELECT 1"

# Windows: Check service
sc query postgresql-x64-15

# Restart if needed
```

### Issue: "CUDA out of memory"
```bash
# Use CPU instead
set CUDA_VISIBLE_DEVICES=-1
# or
export CUDA_VISIBLE_DEVICES=-1

# Then restart FastAPI
```

### Issue: "Flutter can't connect to FastAPI"
```dart
// Edit lib/models/api_models.dart
// For Android emulator:
static const String fastApiBaseUrl = 'http://10.0.2.2:8000';

// For physical device (use your IP):
static const String fastApiBaseUrl = 'http://192.168.1.100:8000';

// Find your IP:
// Windows: ipconfig
// Mac/Linux: ifconfig
```

### Issue: "Image permission denied on iOS"
```
Delete app and reinstall
Permission dialog will appear
Grant permission
```

---

## 📊 EXPECTED OUTPUTS

### FastAPI Startup
```
✅ Database connection pool initialized
🔧 Loading models on cuda/cpu...
✅ All AI models loaded successfully
INFO:     Application startup complete [uvicorn]
```

### Spring Boot Startup
```
2024-04-24 10:00:00 INFO Starting PrescriptionManagementApplication
2024-04-24 10:00:05 INFO Started PrescriptionManagementApplication
```

### Flutter App Startup
```
Launching lib/main.dart on Pixel 5
✓ Built build/app/outputs/flutter-apk/app-debug.apk
✓ Installed build/app/outputs/flutter-apk/app.apk
```

### Successful Upload
```
🚀 [REQUEST] POST http://10.0.2.2:8000/analyze-prescription
📊 Upload Progress: 100%
✅ [RESPONSE] 200
✅ Upload successful
```

---

## 🎯 QUICK CHECKLIST

Before starting:
- [ ] Python 3.8+ installed
- [ ] Java 11+ installed
- [ ] PostgreSQL running
- [ ] Flutter SDK installed
- [ ] Git installed
- [ ] All repos cloned

During setup:
- [ ] Database created and initialized
- [ ] FastAPI health check working
- [ ] Spring Boot health check working
- [ ] Flutter app builds without errors

After setup:
- [ ] All 4 services running in separate terminals
- [ ] Can upload test image from Flutter
- [ ] Results display correctly
- [ ] No errors in logs

---

## 📚 Need More Help?

- **Full setup**: See QUICK_START.md
- **Architecture**: See ARCHITECTURE.md
- **Flutter app**: See README.md in prescription_app/
- **Troubleshooting**: Check QUICK_START.md section "🆘 Troubleshooting"

---

## 🎓 QUICK LEARNING PATH

1. First time? → Read QUICK_START.md
2. Understand system? → Read ARCHITECTURE.md
3. Set up everything? → Use THIS FILE
4. Stuck? → Check QUICK_START.md troubleshooting

---

## 💾 Save These Commands

Bookmark this file and the following:
- QUICK_START.md - Detailed setup guide
- ARCHITECTURE.md - System overview
- README.md - Flutter app docs

---

**Ready? Start with Terminal 1 and work your way down! 🚀**

*Questions? Check the documentation files or the troubleshooting section above.*
