# Bước 1: Build source code bằng Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Bước 2: Chạy file JAR đã build với JDK siêu nhẹ (JRE)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Render sẽ tự cấp cổng qua biến môi trường PORT, Java cần lắng nghe cổng này
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]