# E-commerce Backend Microservices Project  🚀

## Introduction 🌟

This project is a backend system for an e-commerce platform, built using a microservices architecture. 

## Microservices Architecture 🛠️

The project is divided into several interconnected microservices, each handling specific business functions. Below is an overview of each service:

### 1. API Gateway 🌐

- **Description**: The single entry point for all client requests, routing them to the appropriate microservices.
- **Technology**: Spring Cloud Gateway
- **Features**: Security handling, routing, load balancing, and rate limiting.

### 2. Service Registry 📜

Eureka giống như là danh bạ của microservice, giúp các service đăng ký và tìm thấy nhau mà không biết IP hay port của nhau.

Khi các service khởi động thì nó sẽ đăng ký với Eureka Server, và khi một service muốn gọi service khác thì nó sẽ hỏi Eureka Server để lấy thông tin về service đó.


- **Description**: Supports dynamic discovery and registration of microservices within the ecosystem.
- **Technology**: Eureka Server (Spring Cloud Netflix)


### 3. Identity Service 🧑‍💻

- **Description**: spring security and manage user.
- **Technology**: Spring Boot, MySQL, Redis.
- **Features**: show list , create and update user, login, logout.
    - User management (list, create and update users).
    - Authentication (login and logout).
    - Authorization

### 4. Product Service 🛒

- **Description**: Manages the product catalog.
- **Technology**: Spring Boot, MySQL, Redis, kafka.
- **Features**:
    - Product management.
    - Category management.
    - Shopping cart management.

### 5. Order Service 🧾

- **Description**: order product.
- **Technology**: Spring Boot, MySQL, kafka.
- **Features**: order creation.


## Technologies Used 🔧

- **Spring Boot**: The main framework for developing microservices.
- **Apache Kafka**: Supports inter-service communication via asynchronous messaging.
- **OpenFeign**: Simplifies HTTP calls between microservices with declarative REST clients.
- **MySQL**: Relational database management system for storing service-specific data.
- **Redis**: Temporary data storage and caching for high performance.
- **Spring Cloud**: Provides tools for managing a distributed system (Eureka, Gateway, etc.).
- **Docker**: Containerizes each microservice to ensure consistent and isolated environments.
- **Docker Compose**: Coordinates multi-container Docker applications, managing service dependencies and networking.


## Run Project


### Start Infrastructure Services
cd /env
docker-compose -f docker-compose.yml up -d


### Run microservices
Start each microservice manually from IntelliJ IDEA in the following:

1. `service-registry`
2. `api-gateway`
3. `identity-service`
4. `product-service`
5. `order-service`

For each service, select the appropriate Run Configuration and click **Run**.

Wait until the service has started successfully before running the next service.
