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
 :5173 client)          RoleBasedAccessFilter├──► product-service  :8084  (MySQL + Redis cart)
                                             │
                                             └──► order-service    :9195  (MySQL order_db)

                 order-service ◄────── Kafka :9193 ──────► product-service
                                           │
                                           └──────────────► email-service :8086 (currently not run)
										   
```

| Component         | Responsibility |
|------------------|-----------------|
| service-registry | Eureka discovery server |
| api-gateway      | Single entry point: routing, JWT check, role check, CORS |
| identity-service | Users, register/login (password and Google), JWT cookie, admin user management, audit log, dashboard |
| product-service  | Products, variants, categories, image upload (Cloudinary), cart (Redis), stock reservation |
| order-service    | Place orders, outbox, receive stock reservation results |
| common-lib       | Shared DTOs, events, enums, exceptions  |

### Place order flow (Saga with Kafka)

The order process is a **choreography saga** between order-service and product-service. It uses two topics, one per direction.

| Topic (config key) | Name | Producer → Consumer | Payload |
|---|---|---|---|
| `spring.kafka.create-order-topic.name` | `create_order_topic` | order-service → product-service | `OrderMessageEvent` (orderId, items, userId, email…) |
| `spring.kafka.update-order-topic.name` | `update_order_topic` | product-service → order-service | `OrderMessageEvent` (orderId, status = `RESERVED` / `CANCEL`) |

Topic names are always read from config, never hard-coded.

```
Client ──POST /api/v1/order──► Gateway (Auth) ──► order-service / OrderController
                                                     │ 1. Feign → identity-service /auth/me (forward cookie)
                                                     │ 2. Feign → product-service: validate stock and price
                                                     │ 3. ONE DB transaction:
                                                     │      - save Order (status = PENDING)
                                                     │      - save OutboxEvent (status = 0 / NEW)
                                                     ▼
◄── 201 Created (order PENDING) ────────────────────┘

OutboxPublisherJob (every 1s, batch 500)
   │ 4. read NEW outbox events → send to Kafka and wait for broker ACK → mark as SENT
   ▼
create_order_topic
   ▼
product-service / OrderConsumer
   │ 5. duplicate check (processed_orders)
   │ 6. trừ stock bằng UPDATE có điều kiện
   │ 7. send RESERVED or CANCEL → mark result as published
   ▼
update_order_topic
   ▼
order-service / InventoryConsumer
     8. if order is PENDING: RESERVED → CONFIRMED, CANCEL → CANCELLED
```

## Tech stack
- Frontend : React js
- Backend: java, spring boot
- Database: Mysql
- Messaging: Apache Kafka.
- Caching: Redis.
- Infrastructure: Docker, Docker compose.

## Project structure
```
microservice-ecommerce-clone/
├── frontend/
│   ├── client/                 # React + Vite (trang mua hàng)
│   │   
│   └── admin/                  # React + Vite (trang quản trị)
└── backend/
    ├── env/docker-compose-dev.yml     # MySQL 3307, Redis 6319, Kafka 9193, Kafka UI 8080
    ├── common-lib/            # DTO, event, enum, exception dùng chung
    ├── service-registry/      # :8761 Eureka
    ├── api-gateway/           # :9191
    ├── identity-service/      # :9898
    ├── product-service/       # :8084
    ├── order-service/         # :9195
```

## Prerequisites
- Java JDK 17
- Inteliji IDEA
- Docker

## Run Project

### Backend

cd backend/env  
``
docker-compose -f docker-compose.yml up -d  ``


Start each microservice manually from IntelliJ IDEA in the following:

1. `service-registry`
2. `api-gateway`
3. `identity-service`
4. `product-service`
5. `order-service`

For each service, select the appropriate Run Configuration and click **Run**.

Wait until the service has started successfully before running the next service.

Check all service run successfully at:  http://localhost:8761/

![alt text](image.png)


### Frontend

#### admin  
Open a terminal and navigate to the admin directory:

``
cd /frontend/admin  
``

If this is your first time running the application: ``npm install``  
``
npm start
``  
Open browser and go to: http://localhost:3000  
You can login with account:  
- Username: admin
- Password: 123456

#### client    
Open another terminal and navigate to the client directory:  
``
cd /frontend/client``  
If this is your first time running the application: ``npm install``  
``
npm run dev``  

##  TODO
- The client page doesn't have a login feature yet, so for now , use the admin page to create users for clients.