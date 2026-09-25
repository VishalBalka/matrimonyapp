# Phase 04 Handoff

## Scope
Spring Boot backend foundation only.

Included:
- Spring Boot project
- Docker PostgreSQL configuration
- Flyway
- user schema
- password credential schema
- session schema
- health endpoint
- Spring context test
- Phase 04 static verifier

Not included:
- registration HTTP flow
- login HTTP flow
- logout
- rate limiting
- Android networking
- OTP
- social login
- password reset
- MFA/2FA

## Database decision
The build specification calls for Docker PostgreSQL. Phase 04 therefore uses Docker PostgreSQL.

Neon remains available for development, but no switch to Neon is made here.

## Local database
Database name: matrimony
User: matrimony
Password: <DB_PASSWORD>

The password above is a local development value only. Do not replace it with a real credential or commit production secrets.

## Verification
Required commands:

    docker compose up -d postgres
    cd backend
    ..\android\gradlew.bat test
    cd ..
    .\scripts\verify-phase-04.ps1
    cd backend
    ..\android\gradlew.bat bootRun

Health endpoint:

    GET http://localhost:8080/api/health

Do not record a PASS until actual command output has been observed.

## Phase 04 commit
pending

