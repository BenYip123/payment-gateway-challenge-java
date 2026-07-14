FROM gradle:8-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle bootJar -x test

FROM eclipse-temurin:17-jre
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
