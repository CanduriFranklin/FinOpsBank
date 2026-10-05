FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd -r finopsgroup && useradd -r -g finopsgroup finopsuser
USER finopsuser

# Copia el JAR generado localmente por Gradle en Java 25
COPY app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]