FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY src src
COPY keycloak keycloak
# The build produces several jars (boot jar, -plain.jar, -stubs.jar from Spring
# Cloud Contract); COPY ... *.jar app.jar fails with more than one match, so
# pick the executable boot jar explicitly (the only one without a classifier).
RUN chmod +x gradlew && ./gradlew build -x test --no-daemon --parallel \
    && cp build/libs/$(ls build/libs | grep '\.jar$' | grep -vE -- '-plain|-stubs' | head -1) app.jar

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
COPY --from=builder /app/app.jar app.jar
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

