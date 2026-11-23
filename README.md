# E-Commerce

> 🚧 **This project is under active development.** Features, APIs and structure may change at any time, and parts of it are incomplete or not yet production-ready.

A microservices-based education and e-commerce backend built with Spring Boot and Spring Cloud.

Courses: [Course service setup, roles and lesson APIs](course-service/README.md).

Step-by-step learning: [Reliable registration with RabbitMQ and an outbox](docs/registration-flow.md).

Sales business logic: [Product catalog rules and API](docs/product-catalog.md).

Stock and checkout: [Inventory and order business flow](docs/sales-flow.md).

Deployment: [External access through Gateway only](docs/gateway-access.md).

Frontend: [Next.js + TypeScript + Tailwind CSS](frontend/README.md). Run `npm run dev`
inside `frontend` and open `http://localhost:3000`.

## Services

| Service             | Port | Description                                              |
|---------------------|------|----------------------------------------------------------|
| `discovery-service` | 8761 | Eureka service registry                                  |
| `auth-service`      | 8085 | Registration, login, JWT access and refresh tokens       |
| `user-service`      | 8081 | User profiles and parent–student relationships            |
| `user-api`          | –    | Shared registration event contract                       |
| `gateway-service`   | 8080 | Public API entry point                                 |
| `product-service`   | 8083 | Product catalog, MongoDB                               |
| `inventory-service` | 8084 | Stock and reservations                                 |
| `order-service`     | 8086 | Orders and checkout                                    |
| `course-service`    | 8087 | Admin syllabus, direct enrollment and student points   |

## Tech Stack

- Java 21
- Spring Boot 3.5.x, Spring Cloud (Eureka, OpenFeign)
- Spring Security with JWT
- Spring Data JPA, PostgreSQL
- Gradle (multi-module)

## Data ownership

| Service | Database | Persistent data |
|---|---|---|
| Auth | PostgreSQL `auth_db` | Accounts, roles, invitations, refresh tokens and the registration outbox |
| User | PostgreSQL `user_db` | Profiles and family links |
| Course | PostgreSQL `course_db` | Courses, enrollments, syllabus, reports and points |
| Inventory | PostgreSQL `inventory_db` | Stock, receipts and reservations |
| Order | PostgreSQL `order_db` | Customer orders, item price snapshots and payment references |
| Product | MongoDB `product_db` | Products and categories |

Each PostgreSQL service owns its Flyway migrations and accesses its tables through Spring Data JPA repositories. Controllers validate HTTP input and delegate to services. Order calls Catalog and Inventory outside database transactions. Gateway and Discovery run without application databases.

For independent PostgreSQL logins, set `<SERVICE>_DB_USERNAME` and `<SERVICE>_DB_PASSWORD` for `AUTH`, `USER`, `COURSE`, `INVENTORY` or `ORDER`. Each pair defaults to the existing `DB_USERNAME` and `DB_PASSWORD`. For an initialized volume, provision the matching PostgreSQL role and grants before changing the application's credentials.

## Getting Started

### Prerequisites

- JDK 21
- Docker (for PostgreSQL)

### 1. Configure the environment

Configure the root `.env`. See [course environment additions](course-service/.env.example)
for the LMS keys. Set the JWT secret and service keys, and keep
database/RabbitMQ credentials consistent with existing volumes. Compose runs the
whole application in a private network; only Gateway's port 8080 is published.

### 2. Set environment variables

| Variable             | Used by                      | Description                                  |
|----------------------|------------------------------|----------------------------------------------|
| `DB_USERNAME`        | `auth-service`, `user-service` | Database username                          |
| `DB_PASSWORD`        | `auth-service`, `user-service` | Database password                          |
| `SECRET_KEY`         | `auth-service`               | JWT signing key (at least 32 characters)     |
| `ACCESS_EXPIRATION`  | `auth-service`               | Access token lifetime in milliseconds        |
| `REFRESH_EXPIRATION` | `auth-service`               | Refresh token lifetime in milliseconds       |

### 3. Run the services through Docker

```bash
docker compose config --quiet
docker compose up -d --build
```

Send requests to `http://localhost:8080/api/v1/...`. Stop any copies previously
started through the IDE; container isolation does not close host Java processes.
See [gateway access](docs/gateway-access.md) for internal networking and configuration.

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
- [x] API gateway
- [x] Product, inventory and order services
- [x] Course, admin-managed syllabus, direct enrollment and student point APIs
- [ ] Teacher and student frontend screens
- [ ] Tests

## License

Not specified yet.
