# Warehouse Dispatch & Gate Pass Management Backend

Spring Boot 3 backend for a warehouse dispatch and gate pass management system, designed to scale from a single warehouse to multi-warehouse operations.

## Stack

- Java 21
- Spring Boot 3
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL + Flyway
- MapStruct
- MinIO
- Docker / Docker Compose
- Swagger / OpenAPI

## Implemented Foundation

- JWT authentication and role-based authorization
- Core master data APIs: warehouses, users, vendors, drivers, vehicles
- Gate pass request creation, approval/rejection, QR generation, exit confirmation
- Notification persistence with placeholder push gateway
- PostgreSQL schema, indexes, sequences, views, and seed data
- MinIO-based attachment upload endpoint
- Audit log persistence hooks for key workflow actions

## Default Local Users

If `app.bootstrap.enabled=true`, the app seeds one warehouse and these users on first startup:

- `admin / Admin@123`
- `manager / Manager@123`
- `employee / Employee@123`
- `security / Security@123`

## Run Locally With Docker

```bash
docker compose up --build
```

Useful endpoints:

- API base: `http://localhost:8081/api/v1`
- Swagger UI: `http://localhost:8081/swagger-ui/index.html`
- Health: `http://localhost:8081/actuator/health`
- PostgreSQL host port: `localhost:15432`
- MinIO Console: `http://localhost:9001`

## Local Maven Run

1. Start PostgreSQL and MinIO.
2. Export environment variables as needed.
3. Run:

```bash
mvn spring-boot:run
```

## API Areas

- `/api/v1/auth`
- `/api/v1/users`
- `/api/v1/warehouses`
- `/api/v1/vendors`
- `/api/v1/drivers`
- `/api/v1/vehicles`
- `/api/v1/gate-pass-requests`
- `/api/v1/approvals`
- `/api/v1/security`
- `/api/v1/notifications`
- `/api/v1/reports`

## Next Backend Steps

1. Replace the placeholder notification gateway with Firebase FCM integration.
2. Add presigned download URLs for MinIO attachments.
3. Expand report aggregation and warehouse-level access rules.
4. Add integration tests with PostgreSQL Testcontainers.
