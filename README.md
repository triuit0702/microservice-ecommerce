## Introduction

This project is a full-stack application consisting of a client application, an admin page, and a backend built using a microservices architecture.

## Architecture


## Tech stack
- Frontend : React js
- Backend: java, spring boot
- Database: Mysql
- Messaging: Apache Kafka.
- Caching: Redis.
- Infrastructure: Docker, Docker compose.

## Project structure
TODO: Cấu trúc thư mục và vai trò từng module

## Prerequisites
- Java JDK 17
- Inteliji IDEA
- Docker

## Run Project

### Backend

cd /env
docker-compose -f docker-compose.yml up -d


Start each microservice manually from IntelliJ IDEA in the following:

1. `service-registry`
2. `api-gateway`
3. `identity-service`
4. `product-service`
5. `order-service`

For each service, select the appropriate Run Configuration and click **Run**.

Wait until the service has started successfully before running the next service.


### Frontend

1.  admin 
Open a terminal :

cd /frontend/admin
npm install
npm start

2.  client
Open another termina

cd /frontend/client
npm install
npm run dev

## API Documentation