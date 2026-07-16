
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Sử dụng apt-get (trình quản lý gói của Debian/Ubuntu) thay vì apk
RUN apt-get update && apt-get install -y --no-install-recommends \
    libstdc++6 \
    && rm -rf /var/lib/apt/lists/*

# Chạy file JAR đã build với JDK 21 JRE
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Render sẽ tự cấp cổng qua biến môi trường PORT, Java cần lắng nghe cổng này
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
