FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN sed -i 's/\r$//' gradlew
RUN chmod +x gradlew
RUN ./gradlew --no-daemon :discovery-service:bootJar :gateway-service:bootJar :auth-service:bootJar :user-service:bootJar :product-service:bootJar :inventory-service:bootJar :order-service:bootJar

FROM eclipse-temurin:21-jre
ARG SERVICE
WORKDIR /app
COPY --from=build /workspace/${SERVICE}/build/libs/*-SNAPSHOT.jar /app/app.jar
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
