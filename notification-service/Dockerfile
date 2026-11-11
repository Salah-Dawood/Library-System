# Build from the project root:  docker build -f notification-service/Dockerfile -t booknest/notification-service .

# ---- stage 1: build the jar with Maven and JDK 17 ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# the poms first, so the downloaded libraries are cached until a pom changes
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
COPY user-service/pom.xml user-service/
COPY catalog-service/pom.xml catalog-service/
COPY lending-service/pom.xml lending-service/
COPY notification-service/pom.xml notification-service/
RUN mvn -q -B -pl notification-service -am dependency:go-offline

# then the code of this service only; the tests run in the CI pipeline, not here
COPY notification-service/src notification-service/src
RUN mvn -q -B -pl notification-service -am package -DskipTests

# ---- stage 2: run the jar on a small Java 17 runtime ----
FROM eclipse-temurin:17-jre
WORKDIR /app

# run as a normal user, not root
RUN useradd --system --create-home booknest && chown booknest /app
USER booknest

COPY --from=build /build/notification-service/target/notification-service-*.jar app.jar

EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
