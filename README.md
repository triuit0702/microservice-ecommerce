# Microservice E-commerce

## Introduction

This project is a full-stack application consisting of a client application, an admin page, and a backend built using a microservices architecture.


## Architecture overview

```
                 ┌────────────────────┐
                 │  service-registry  │  Eureka :8761 (all services register here)
                 └─────────▲──────────┘
                           │ discovery (lb://SERVICE-NAME)
                           │
React clients ──HTTP──► api-gateway :9191 ──┬──► identity-service :9898  (MySQL user_db)
(:3000 admin,           AuthenticationFilter │
 :5173 client)          RoleBasedAccessFilter├──► product-service  :8084  (MySQL product_db + Redis cart)
                                             │
                                             └──► order-service    :9195  (MySQL order_db)

                 order-service ◄────── Kafka :9193 ──────► product-service
```

| Component        | Responsibility                                                                                       |
| ---------------- | ---------------------------------------------------------------------------------------------------- |
| service-registry | Eureka discovery server                                                                              |
| api-gateway      | Single entry point: routing, JWT check, role check, CORS                                             |
| identity-service | Users, register/login (password and Google), JWT cookie, admin user management, audit log, dashboard |
| product-service  | Products, variants, categories, image upload (Cloudinary), cart (Redis), stock reservation           |
| order-service    | Place orders, outbox, receive stock reservation results                                              |
| common-lib       | Shared DTOs, events, enums, exceptions                                                               |

### Place order flow (Saga with Kafka)

Orders and stock live in two services, each with its own database, so one order is split into small steps.
The services talk to each other through Kafka events, and a failed step is undone (the order is cancelled,
the stock is given back). This prevents selling more than the stock.

```
Client ──POST /api/v1/order──► order-service
                                  │ 1. quick check with product-service: products exist,
                                  │    enough stock, same prices (stock is not reduced yet)
                                  │ 2. save the order (PENDING) and the message to send
                                  │    in the same DB transaction (the "outbox")
                                  ▼
Client ◄── 3. 201 Created (order PENDING)

order-service (background job, every 1s)
   │ 4. send the saved message to Kafka
   ▼
[Kafka topic: inventory.command]
   ▼
product-service
   │ 5. reduce the stock → reply RESERVED (or CANCEL if out of stock)
   ▼
[Kafka topic: inventory.result]
   ▼
order-service
     6. RESERVED → order CONFIRMED,  CANCEL → order CANCELLED

Client polls GET /api/v1/order/{id} until the order is no longer PENDING.

If nothing happens for 5 minutes:
order-service (background job, every 60s)
   │ 7. order still PENDING → CANCELLED, send "release stock"
   ▼
[Kafka topic: inventory.command]
   ▼
product-service
     8. give the stock back
```

`RESERVED` / `CANCEL` are the replies from product-service; `CONFIRMED` / `CANCELLED` are the order statuses.

**Key points**
- **Idempotent checkout (safe to repeat):** each checkout sends an `Idempotency-Key`; a double click or a retry
  returns the same order instead of creating a second one.
- **Server prices win:** client prices are only compared; a difference returns `409 PRICE_CHANGED`.
- **Outbox:** the order and its message are saved together, so a message is never lost even if Kafka is down.
- **Duplicate-safe consumer:** product-service remembers the orders it has handled, so a repeated message never
  reduces the stock twice.

Details (error codes, topics, dead-letter topics, Kafka down, full sequence): [ORDER_FLOW_DETAILS.md](backend/ORDER_FLOW_DETAILS.md)


## Tech stack
- Frontend: React + Vite.
- Backend: Java 17, Spring Boot, Spring Cloud (Eureka, Gateway, OpenFeign).
- Database: MySQL.
- Messaging: Apache Kafka.
- Caching: Redis.
- Image storage: Cloudinary.
- Monitoring: Prometheus, mysqld-exporter, Grafana.
- Infrastructure: Docker, Docker Compose.

## Project structure
```
microservice-ecommerce-clone/
├── frontend/
│   ├── client/                 # React + Vite (shop page)
│   └── admin/                  # React + Vite (admin dashboard)
└── backend/
    ├── docker-compose.yml     # Option 1: full stack (infra + all services)
    ├── Dockerfile             # Shared multi-stage build for every service
    ├── env/docker-compose-dev.yml     # Option 2: infra only (MySQL 3307, Redis 6319, Kafka 9193, Kafka UI 8080, Prometheus 9090, Grafana 3001)
    ├── common-lib/            # Shared DTOs, events, enums, exceptions
    ├── service-registry/      # :8761 Eureka
    ├── api-gateway/           # :9191
    ├── identity-service/      # :9898
    ├── product-service/       # :8084
    └── order-service/         # :9195
```

## Prerequisites

| Option                          | Needs                                  |
| ------------------------------- | -------------------------------------- |
| 1. Docker Compose (one command) | Docker (~3 GB free RAM), Node.js 20+   |
| 2. Command line (development)   | Java JDK 17, Docker, Node.js 20+       |

Maven is **not** needed: Option 1 builds inside Docker, Option 2 uses the Maven wrapper (`mvnw`) in each module.

Notes for the commands below:
- **macOS / Linux**: if `./mvnw` says "permission denied", run `chmod +x backend/*/mvnw`.
- **Windows PowerShell**: use `.\mvnw.cmd` instead of `./mvnw`.
- All paths are relative to the repository root.

## Infrastructure

Started by `backend/docker-compose.yml` (Option 1) or `backend/env/docker-compose-dev.yml` (Option 2):

| Component | Address               | Notes                                                                                                |
| --------- | --------------------- | ---------------------------------------------------------------------------------------------------- |
| MySQL     | `localhost:3307`      | user `root` / `123456`. `user_db`, `product_db`, `order_db` are created by `env/mysql/init/init.sql` |
| Redis     | `localhost:6319`      | Cart storage                                                                                         |
| Kafka     | `localhost:9193`      | Order saga events                                                                                    |
| Kafka UI  | http://localhost:8080 | View topics and messages                                                                             |
| Prometheus | http://localhost:9090 | Metrics of the services (`/actuator/prometheus`) and MySQL (via `mysqld-exporter`)                   |
| Grafana | http://localhost:3001 | user `admin` / `admin` (`GRAFANA_ADMIN_PASSWORD`, first start only). Dashboards are in folder **Ecommerce** |

Monitoring check: every target on http://localhost:9090/targets is **UP** (a service shows DOWN while it is not running),
and the query `mysql_up` returns `1`. `mysqld-exporter` needs no setup: it logs in to MySQL with the dev root account.
If you change the MySQL root password, set `MYSQLD_EXPORTER_PASSWORD` to the same value.
Grafana dashboards are JSON files in `backend/env/grafana/dashboards` (Services Overview, JVM, MySQL), loaded automatically.

## Run Project

### Option 1: Docker Compose (one command)

Builds and starts MySQL, Redis, Kafka, Kafka UI, Prometheus, mysqld-exporter, Grafana and the 5 backend services:

```bash
cd backend
docker compose up --build -d
```

- The first build takes about 5 minutes (Maven downloads dependencies); later builds are cached.
- The services need about 1 more minute to start and register in Eureka.
- Total memory use is about 3 GB.

Check:
- http://localhost:8761 lists API-GATEWAY, IDENTITY-SERVICE, PRODUCT-SERVICE and ORDER-SERVICE.
- http://localhost:9191/actuator/health returns `{"status":"UP"}`.

Useful commands (run in `backend/`):

```bash
docker compose ps                        # status of all containers
docker compose logs -f order-service     # follow the logs of one service
docker compose up --build -d order-service   # rebuild one service after a code change
docker compose down                      # stop everything (data is kept)
# data lives in backend/env/data/ (shared with Option 2); delete that folder to reset the database
```

Then start the frontend (see [Frontend](#frontend)).

> Both options share the same MySQL, Redis and Kafka containers.
> To switch from Option 1 to Option 2, stop only the 5 services:
> `docker compose stop service-registry api-gateway identity-service product-service order-service`

### Option 2: Run services from the command line (development)

Faster for day-to-day coding: restart only the service you changed and read its logs directly.

#### 1. Start the infrastructure

```bash
cd backend/env
docker-compose -f docker-compose-dev.yml up -d
```

Check: `docker ps` shows MySQL, Redis and Kafka; Kafka UI is at http://localhost:8080.
The first start takes about a minute while MySQL runs `init.sql`.

#### 2. Install common-lib (first time, and after every change to it)

`common-lib` is not published to Maven Central, so the services cannot resolve it
until it is installed into your local Maven repository (`~/.m2`):

```bash
cd backend/common-lib
./mvnw clean install -DskipTests
```

Check: the output ends with `BUILD SUCCESS`.

#### 3. Start the backend services (one terminal each, in this order)

Each command keeps running. Wait for `Started ... in N seconds` before starting the next one.

```bash
cd backend/service-registry && ./mvnw spring-boot:run   # :8761
cd backend/api-gateway      && ./mvnw spring-boot:run   # :9191
cd backend/identity-service && ./mvnw spring-boot:run   # :9898
cd backend/product-service  && ./mvnw spring-boot:run   # :8084
cd backend/order-service    && ./mvnw spring-boot:run   # :9195
```

PowerShell example: `cd backend\service-registry; .\mvnw.cmd spring-boot:run`

Check:
- http://localhost:8761 lists API-GATEWAY, IDENTITY-SERVICE, PRODUCT-SERVICE and ORDER-SERVICE.
- http://localhost:9191/actuator/health returns `{"status":"UP"}`.

![Eureka dashboard](image.png)

`email-service` is not part of the default run for now (both options).

#### 4. Stop

Press Ctrl+C in each service terminal, then:

```bash
cd backend
docker compose -f env/docker-compose-dev.yml stop
```

### Frontend

Same for both options (two more terminals):

```bash
cd frontend/admin  && npm install && npm start     # http://localhost:3000
cd frontend/client && npm install && npm run dev   # http://localhost:5173
```

`npm install` is only needed the first time.
Admin login: username `admin`, password `123456`.

## Configuration (optional)

The app starts without these values; only **product image upload** and **Google login** need them.

**Option 1 (Docker Compose)**: create `backend/.env` (gitignored) and run `docker compose up -d` again:

```bash
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
GOOGLE_CLIENT_SECRET=...
```

**Option 2 (command line)**: set them in the terminal **before** starting the service:

| Service          | bash / zsh                          | PowerShell                            | Purpose              |
| ---------------- | ----------------------------------- | ------------------------------------- | -------------------- |
| product-service  | `export CLOUDINARY_API_KEY=...`     | `$env:CLOUDINARY_API_KEY = "..."`     | Product image upload |
| product-service  | `export CLOUDINARY_API_SECRET=...`  | `$env:CLOUDINARY_API_SECRET = "..."`  | Product image upload |
| identity-service | `export GOOGLE_CLIENT_SECRET=...`   | `$env:GOOGLE_CLIENT_SECRET = "..."`   | Google login         |

For Option 2, instead of environment variables you can use local config files (gitignored):
- **identity-service**: copy `backend/identity-service/src/main/resources/application-local.properties.example`
  to `application-local.properties` and fill in the secret.
- **product-service**: copy `backend/product-service/src/main/resources/application-local.yml.example`
  to `application-local.yml` and fill in the secrets.
- **frontend/client**: copy `frontend/client/.env.example` to `.env.local` and set `VITE_GOOGLE_CLIENT_ID`
  (same value as `GOOGLE_CLIENT_ID` of identity-service).

Never commit real secrets to the repository.
