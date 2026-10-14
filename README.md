# E-Commerce

> 🚧 **This project is under active development.** Features, APIs and structure may change at any time, and parts of it are incomplete or not yet production-ready.

A microservices-based e-commerce backend built with Spring Boot and Spring Cloud.

## Services

| Service             | Port | Description                                              |
|---------------------|------|----------------------------------------------------------|
| `discovery-service` | 8761 | Eureka service registry                                  |
| `auth-service`      | 8085 | Registration, login, JWT access and refresh tokens       |
| `user-service`      | 8081 | User profiles (internal API called by `auth-service`)    |
| `user-api`          | –    | Shared library: Feign client and DTOs for `user-service` |

## Tech Stack

- Java 21
- Spring Boot 3.5.x, Spring Cloud (Eureka, OpenFeign)
- Spring Security with JWT
- Spring Data JPA, PostgreSQL
- Gradle (multi-module)

## Getting Started

### Prerequisites

- JDK 21
- Docker (for PostgreSQL)

### 1. Start the databases

```bash
docker compose up -d
```

This starts two PostgreSQL instances: `auth_db` on port `5432` and `user_db` on port `5433`.

### 2. Set environment variables

| Variable             | Used by                      | Description                                  |
|----------------------|------------------------------|----------------------------------------------|
| `DB_USERNAME`        | `auth-service`, `user-service` | Database username                          |
| `DB_PASSWORD`        | `auth-service`, `user-service` | Database password                          |
| `SECRET_KEY`         | `auth-service`               | JWT signing key (at least 32 characters)     |
| `ACCESS_EXPIRATION`  | `auth-service`               | Access token lifetime in milliseconds        |
| `REFRESH_EXPIRATION` | `auth-service`               | Refresh token lifetime in milliseconds       |

### 3. Run the services

Start them in this order:

1. `discovery-service`
2. `user-service`
3. `auth-service`

## API (current)

| Method | Endpoint                | Description        |
|--------|-------------------------|--------------------|
| POST   | `/api/v1/auth/register` | Register a new user |
| POST   | `/api/v1/auth/login`    | Log in, returns access and refresh tokens |

## Status

- [x] Service discovery
- [x] User registration and login
- [ ] Refresh token and logout endpoints
- [ ] Input validation and proper error responses
- [ ] API gateway
- [ ] Product, cart and order services
- [ ] Tests

## License

Not specified yet.
