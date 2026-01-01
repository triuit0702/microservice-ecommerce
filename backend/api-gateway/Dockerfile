FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY target/api-gateway.jar api-gateway.jar

EXPOSE 9191

ENTRYPOINT ["java", "-jar", "api-gateway.jar"]