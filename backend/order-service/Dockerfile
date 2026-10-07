FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY target/order-service.jar order-service.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "order-service.jar"]