# --- build ---
FROM gradle:9.7-jdk21 AS build
WORKDIR /app
COPY settings.gradle.kts build.gradle.kts ./
COPY src ./src
RUN gradle --no-daemon bootJar

# --- run ---
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
# Run as an unprivileged user so a container-escape/RCE bug isn't root.
RUN addgroup -S app && adduser -S app -G app && chown -R app:app /app
USER app
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
