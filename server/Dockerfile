# Stage 1: Build the application
# FROM gradle:7.6.4-jdk21-alpine AS build
FROM gradle:jdk21-corretto-al2023 AS build

# Set the working directory
WORKDIR /app

# Copy the build.gradle file and any other necessary files
COPY build.gradle .
COPY src ./src

# Build the application
RUN gradle shadowJar --no-daemon

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-alpine

# Set the working directory
WORKDIR /app

# Copy the built JAR file from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose the port the application runs on (if applicable)
EXPOSE 6379

# Define the entry point to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
