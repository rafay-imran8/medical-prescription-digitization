# HealthAtlas

**AI-powered medical prescription digitization and drug-safety platform**

HealthAtlas turns handwritten and printed medical prescriptions into structured, verified digital records — automatically extracting drug names and dosages, normalizing them against standard drug nomenclature, and checking for dangerous drug-drug interactions before they reach a patient or pharmacist.

Built as a Final Year Project (FYP), GIKI, 2026.

---

## Overview

Prescription errors caused by illegible handwriting and manual transcription are a well-documented patient safety risk. HealthAtlas addresses this with a multi-stage AI pipeline that reads a prescription image, cleans and structures the extracted text, maps it to standardized drug codes, and flags clinically significant interactions — all surfaced through role-based dashboards for doctors, patients, analysts, and admins.

## Key Features

- **Prescription digitization** — upload a prescription image and get back structured, editable digital data
- **Field detection** — locates drug name, dosage, and frequency regions on the prescription image
- **Handwriting/text OCR** — extracts raw text from detected fields
- **LLM-based cleanup** — corrects and structures noisy OCR output into consistent drug/dosage entries
- **Drug normalization** — maps extracted drug names to standard RxNorm codes, with local brand-name mapping for regional drug names
- **Drug-drug interaction checking** — cross-references normalized drugs against a DrugBank-backed interaction database and flags risky combinations
- **Role-based access** — separate views and permissions for Doctor, Patient, Analyst, and Admin roles
- **Analytics dashboards** — disease and prescription trend monitoring
- **Secure by design** — JWT authentication, bcrypt password hashing, and time-limited data access permissions enforced at the API layer

## Pipeline Architecture

```
Prescription Image
      │
      ▼
1. Field Detection        (YOLO11)
      │
      ▼
2. OCR / Text Extraction  (TrOCR)
      │
      ▼
3. LLM Cleanup            (Flan-T5 XL)
      │
      ▼
4. Drug Normalization     (RxNorm + local brand mapping)
      │
      ▼
5. Interaction Checking    (DrugBank-based interaction lookup)
      │
      ▼
Structured, Verified Prescription Record
```

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React |
| Backend (core API) | Spring Boot |
| ML Microservice | FastAPI (Python) |
| Database | PostgreSQL (hosted on AWS RDS) |
| Auth | JWT, bcrypt |
| Containerization | Docker |
| ML Models | YOLO11, TrOCR, Flan-T5 XL |
| Drug Data | RxNorm, DrugBank |

## System Architecture

HealthAtlas is a multi-tier system:

- **React frontend** — patient/doctor/analyst/admin interfaces
- **Spring Boot backend** — REST APIs, authentication, authorization, business logic, following a service/repository/controller layering
- **FastAPI ML microservice** — hosts the field detection → OCR → LLM cleanup → normalization → interaction-checking pipeline
- **PostgreSQL on AWS RDS** — persistent storage for users, prescriptions, and drug interaction records
- All services are containerized with Docker for orchestration and deployment

## Getting Started

### Prerequisites

- Docker & Docker Compose
- Java 17+ and Maven (if running the backend outside Docker)
- Python 3.10+ (if running the ML service outside Docker)
- Node.js 18+ (if running the frontend outside Docker)
- PostgreSQL instance (local or AWS RDS)

### Setup

```bash
# Clone the repository
git clone <repo-url>
cd healthatlas

# Configure environment variables (see below)
cp .env.example .env

# Start all services with Docker Compose
docker-compose up --build
```

The frontend, backend, and ML microservice will each start in their own container. Update `.env` with your database credentials and any model paths before starting.

### Environment Variables

| Variable | Description |
|---|---|
| `DB_URL` | PostgreSQL connection string (AWS RDS endpoint or local) |
| `DB_USERNAME` / `DB_PASSWORD` | Database credentials |
| `JWT_SECRET` | Secret key used to sign JWTs |
| `ML_SERVICE_URL` | URL of the FastAPI ML microservice |
| `DRUGBANK_API_KEY` | Credentials for DrugBank interaction lookups (if applicable) |

*(Adjust variable names to match your actual `.env` file if they differ.)*

## API Overview

The Spring Boot backend exposes REST APIs across four role-based domains:

- **Doctor** — create and manage prescriptions for patients
- **Patient** — view their own prescription history and flagged interactions
- **Analyst** — access aggregated disease/prescription trend data
- **Admin** — manage users, roles, and system-level data

Access is enforced with JWT-based authentication and role-based authorization at the API layer, with time-limited permissions on patient data.

## Engineering Notes

A few non-obvious things tuned during development, worth mentioning if extending the project:

- Fixed N+1 query patterns in the JPA/Hibernate layer for prescription/patient lookups
- Tuned HikariCP connection pool settings for the Spring Boot ↔ PostgreSQL connection
- Optimized the JWT filter chain to reduce per-request overhead
- Flan-T5 XL is lazy-loaded in the ML microservice to avoid startup crashes under systemd
- Frontend was rebuilt with a dark-mode-first UI

## Project Structure

```
healthatlas/
├── backend/          # Spring Boot REST API
├── ml-service/       # FastAPI ML microservice (detection, OCR, LLM, normalization, interactions)
├── frontend/         # React application
├── docker-compose.yml
└── README.md
```


## Author

**Muhammad Murtaza** — B.S. Computer Science, GIKI
[GitHub](https://github.com/murtazakhan7) · [Portfolio](https://murtazakhan7.github.io/Portfolio) · [LinkedIn](https://linkedin.com/in/muhammad-murtaza-6444792a9)

