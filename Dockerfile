# Крок 1: Збірка (Build)
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# Крок 2: Запуск (Run)
# Використовуємо актуальний образ Eclipse Temurin замість старого openjdk
FROM eclipse-temurin:17-jre-focal
COPY --from=build /target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
