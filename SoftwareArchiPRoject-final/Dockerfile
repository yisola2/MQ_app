# Use a Maven image to build the app
FROM maven:3.8.4-openjdk-17 AS build

WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Use an OpenJDK image to run the app
FROM openjdk:17

WORKDIR /app

# Copy the built JAR from the previous step
COPY --from=build /app/target/*.jar app.jar

# Expose port (Spring Boot default is 8080)
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]