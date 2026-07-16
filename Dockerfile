
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Cài đặt libc6-compat và gcompat để cung cấp libstdc++.so.6 cho OpenCV
RUN apk add --no-cache libc6-compat gcompat libstdc++

# Chạy file JAR đã build với JDK 21 JRE
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Render sẽ tự cấp cổng qua biến môi trường PORT, Java cần lắng nghe cổng này
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
