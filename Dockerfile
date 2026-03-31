# Крок 1: Збірка
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Крок 2: Запуск
FROM eclipse-temurin:17-jre-focal
WORKDIR /app
# Використовуємо знахідку: копіюємо все, що закінчується на -shaded.jar
COPY --from=build /app/target/*-shaded.jar /app/app.jar
EXPOSE 8080
# Вказуємо повний шлях до файлу
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
