# syntax=docker/dockerfile:1
# Shared multi-stage build for every backend service.
# Build context is backend/ so common-lib (not on Maven Central) can be installed first.
# Usage: docker build --build-arg SERVICE=order-service -t order-service backend/

# ---- Stage 1: build the jar ----
FROM maven:3.9-eclipse-temurin-17 AS build
ARG SERVICE
WORKDIR /src

COPY common-lib/pom.xml common-lib/pom.xml
COPY common-lib/src common-lib/src
COPY ${SERVICE}/pom.xml ${SERVICE}/pom.xml
COPY ${SERVICE}/src ${SERVICE}/src

# The ~/.m2 cache is shared between builds, so dependencies are downloaded only once.
# common-lib is installed in the same step so the service always sees it.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -q -f common-lib/pom.xml install -DskipTests \
 && mvn -B -q -f ${SERVICE}/pom.xml package -DskipTests \
 && cp ${SERVICE}/target/${SERVICE}.jar /app.jar

# ---- Stage 2: small runtime image ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app.jar app.jar

# Keep each JVM small so the whole stack fits on a laptop; override in compose if needed.
ENV JAVA_OPTS="-Xms128m -Xmx384m"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
