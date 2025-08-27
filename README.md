# Business Monitoring

A Spring Boot microservice application for processing customer subscription data, generating business reports, and managing notification workflows through Kafka messaging.

## Architecture Overview

### Functional Description
The Business Monitoring application provides a comprehensive solution for:

- **CSV Data Processing**: Upload and validate customer subscription CSV files
- **Business Intelligence**: Generate summary reports with key metrics
- **Automated Notifications**:
    - Alert for customers with multiple expired services
    - Identify upselling opportunities for long-term subscribers
- **Email Integration**: Send automated upselling notifications via email
- **Security**: OAuth2/JWT authentication through Keycloak integration

### Technical Architecture

```
                             
                       ┌─────────────────┐
                       │   Keycloak      │
                       │ Authentication  │
                       └─────────────────┘
                                ^
                                │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend/     │    │  Business       │    │   PostgreSQL    │
│   API Client    │<──>│  Monitoring     │<──>│   Database      │
│                 │    │   Service       │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                              v
                       ┌─────────────────┐
                       │     Kafka       │
                       │   Messaging     │
                       └─────────────────┘
                              │
                              v
                       ┌─────────────────┐
                       │  Email Service  │
                       │   (SMTP)        │
                       └─────────────────┘

```

**Components:**
- **Spring Boot 3.5.4** with Java 21
- **PostgreSQL 17** for data persistence
- **Apache Kafka** for asynchronous messaging
- **Keycloak 26.3.3** for OAuth2/OIDC authentication
- **Flyway** for database migrations
- **Docker Compose** for containerized deployment

## Quick Start

### 1. Environment Setup

Clone the repository and navigate to the project directory:

```bash
git clone <repository-url>
cd BusinessMonitoring
```

### 2. Create .env file
#### Environment Variables

The application uses environment variables for configuration. Default values are provided in `.env` file. Following an example `.env` configuration:

```env
# Database
DB_NAME=business_monitoring
DB_USER=userBM
DB_PWD=userBM123
DB_HOST=database
DB_PORT=5432
DB_JDBC_BATCH_SIZE=20

# Server
SERVER_PORT=8090
SERVER_PORT_MANAGEMENT=8091

# Kafka & Zookeeper
KAFKA_BROKER_ID=1
KAFKA_PORT=9092
KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092
KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092
KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT
KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT
KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1

ZOOKEEPER_SERVER_ID=1
ZOOKEEPER_CLIENT_PORT=2181
ZOOKEEPER_PORT=2181
ZOOKEEPER_TICK_TIME=2000
ZOOKEEPER_SERVERS=zookeeper:2888:3888

# Kafka Producer/Consumer
EXPIRED_SERVICES_KAFKA_CLIENT_ID=expired-services-producer
UPSELLING_SERVICE_KAFKA_CLIENT_ID=upselling-service-producer
KAFKA_PRODUCER_RETRIES=3
KAFKA_PRODUCER_ACKS=all
KAFKA_PRODUCER_BACKOFF_INTERVAL=1000
KAFKA_PRODUCER_MAX_ATTEMPTS=3
EVENT_TOPIC_EXPIRED_SERVICES=expired-services-topic
EVENT_TOPIC_EMAIL_UPSELLING_SERVICE=email-upselling-service-topic

# Business Logic
EXPIRED_SERVICES_LIMIT=5
YEARS_SUBSCRIPTION_LIMIT=3

# Mail Configuration (disabled by default)
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password
MAIL_FROM=your-email@gmail.com
MAIL_TO=recipient@example.com
MAIL_ENABLE=false
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
MANAGEMENT_HEALTH_MAIL_ENABLED=false

# Async Configuration
SPRING_TASK_EXECUTION_POOL_CORE_SIZE=5
SPRING_TASK_EXECUTION_POOL_MAX_SIZE=10
SPRING_TASK_EXECUTION_POOL_QUEUE_CAPACITY=25
SPRING_TASK_EXECUTION_THREAD_NAME_PREFIX=notification-

# Retry Configuration
RETRY_MAX_ATTEMPTS=3
RETRY_INITIAL_DELAY=1000
RETRY_MULTIPLIER=2.0
RETRY_MAX_DELAY=5000

# Keycloak 
KEYCLOAK_INTERNAL_AUTH_SERVER_URL=http://keycloak:8080
KEYCLOAK_EXTERNAL_AUTH_SERVER_URL=http://localhost:8080
KEYCLOAK_REALM=business-monitoring
KEYCLOAK_PORT=8080
```
### 3. Start the Complete Environment
Build the project by running:
```bash
mvn wrapper:wrapper

./mvnw clean install
```

Then start all services using Docker Compose:
```bash
docker-compose up -d
```

This command will start:
- PostgreSQL database  
- Zookeeper 
- Kafka 
- Keycloak
- Business Monitoring API (with a management actuator on a different port)

### 4. Configure Keycloak Authentication

Wait for all services to be healthy, then run the setup script:

```bash
chmod +x setup-keycloak.sh
./setup-keycloak.sh
```

This creates:
- Realm: `business-monitoring`
- Client: `business-monitoring-client`
- User: `reportuser` / `password123`
- Role: `REPORT_USER`

_It is just a sample file to quickly set up Keycloak. You can customize as needed as well as you can use the Keycloak admin console._

### 5. Verify Services

Check service health:
```bash
# Application health
docker-compose ps 
```

## API Endpoints

### Base URL: `http://{host}:{server_port}/api/v1/report`

#### 1. Upload CSV File
- **Endpoint**: `POST /upload-csv`
- **Content-Type**: `multipart/form-data`
- **Authentication**: Required (`REPORT_USER` role)
- **Parameters**: `file` (multipart file)

#### 2. Get Summary Report
- **Endpoint**: `GET /summary`
- **Content-Type**: `application/json`
- **Authentication**: Required (`REPORT_USER` role)

### API Documentation
Interactive API documentation is available at:
- **Swagger UI**: http://{host}:{server_port}/openapi/ui/swagger-ui.html
- **OpenAPI JSON**: http://{host}:{server_port}/openapi/v1/api-docs

_By using some rest client (such as postman/insomnia) you have to first authenticate by selecting Oauth2_
## CSV File Format

The application accepts CSV files with the following structure:

### Required Headers (exact order):
1. `customer_id` - Customer identifier (string)
2. `service_type` - Type of service (string)
3. `activation_date` - Service activation date (YYYY-MM-DD)
4. `expiration_date` - Service expiration date (YYYY-MM-DD)
5. `amount` - Service amount (decimal)
6. `status` - Service status (ACTIVE|EXPIRED|PENDING_RENEWAL)

### Sample CSV:
```csv
customer_id,service_type,activation_date,expiration_date,amount,status
CUST001,Hosting,2020-01-15,2024-12-31,299.99,ACTIVE
CUST002,SPID,2021-03-20,2023-03-19,99.99,EXPIRED
CUST001,PEC,2019-05-10,2024-05-09,199.99,ACTIVE
CUST003,email,2018-01-01,2025-01-01,999.99,PENDING_RENEWAL
```

### File Validation:
- **Format**: CSV or TXT files only
- **Size**: Must not be empty
- **Headers**: Must match exactly (case-insensitive)
- **Data Types**: Proper date format (YYYY-MM-DD) and decimal numbers
- **Status Values**: Must be one of ACTIVE, EXPIRED, PENDING_RENEWAL


### Reset Environment

```bash
# Stop all services
docker-compose down

# Remove volumes 
docker-compose down -v

# Restart fresh
docker-compose up -d
./setup-keycloak.sh
```
