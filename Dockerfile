FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

FROM eclipse-temurin:17-jre
RUN apt-get update && apt-get install -y --no-install-recommends libgomp1 libgfortran5 && rm -rf /var/lib/apt/lists/*
WORKDIR /app
ENV DJL_CACHE_DIR=/app/.djl ENGINE_CACHE_DIR=/app/.djl
COPY --from=build /app/target/posture-coach-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-Xmx400m","-jar","app.jar"]
