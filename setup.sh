#!/bin/bash

echo "🚀 Prescription Management System - Backend Setup"
echo "=================================================="
echo ""

# Check Java version
echo "Checking Java version..."
java -version 2>&1 | grep -q "version \"17" || java -version 2>&1 | grep -q "version \"21"
if [ $? -eq 0 ]; then
    echo "✅ Java 17+ found"
else
    echo "❌ Java 17+ required. Please install Java 17 or later."
    exit 1
fi

# Check Maven
echo "Checking Maven..."
command -v mvn >/dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ Maven found"
else
    echo "❌ Maven not found. Please install Maven 3.8+."
    exit 1
fi

# Check PostgreSQL connection
echo "Checking PostgreSQL..."
command -v psql >/dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ PostgreSQL client found"
else
    echo "⚠️  PostgreSQL client not found. Make sure PostgreSQL is installed."
fi

# Create image storage directory
echo ""
echo "Creating image storage directory..."
if [ "$(uname)" == "Darwin" ] || [ "$(uname)" == "Linux" ]; then
    sudo mkdir -p /var/prescription-images
    sudo chmod 777 /var/prescription-images
    echo "✅ Created /var/prescription-images"
else
    mkdir -p C:/prescription-images 2>/dev/null || echo "⚠️  Please manually create C:\\prescription-images"
fi

# Check application.properties
echo ""
echo "Checking configuration..."
if grep -q "your_password_here" src/main/resources/application.properties; then
    echo "⚠️  WARNING: Please update application.properties with:"
    echo "   - Database password"
    echo "   - JWT secret"
    echo "   - SendGrid API key"
else
    echo "✅ Configuration looks updated"
fi

echo ""
echo "=================================================="
echo "Setup checklist:"
echo "1. ✅ Java 17+ installed"
echo "2. ✅ Maven installed"
echo "3. ⚠️  PostgreSQL database created and tables setup"
echo "4. ⚠️  application.properties configured"
echo "5. ✅ Image storage directory created"
echo "6. ⚠️  FastAPI service running on port 8000"
echo ""
echo "Next steps:"
echo "  1. Update src/main/resources/application.properties"
echo "  2. Run database setup: python create_database_schema.py"
echo "  3. Build project: mvn clean install"
echo "  4. Run application: mvn spring-boot:run"
echo ""
echo "Server will start on http://localhost:8080"
echo "=================================================="