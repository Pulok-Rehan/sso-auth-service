FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copy jar
COPY target/sso-auth-service-0.0.1-SNAPSHOT.jar app.jar

# Create directory for JWT keys
RUN mkdir -p /app/jwt-keys

# Expose application port
EXPOSE 9090

# Run Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]
