FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY target/product-service.jar product-service.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "product-service.jar"]