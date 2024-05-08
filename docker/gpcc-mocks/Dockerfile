FROM gradle:8.7-jdk21 AS build

COPY --chown=gradle:gradle gpcc-mocks /home/gradle/gpcc-mocks

WORKDIR /home/gradle/gpcc-mocks

RUN gradle classes

FROM build AS package

RUN gradle --build-cache bootJar

FROM eclipse-temurin:21-jre-jammy

EXPOSE 8081

RUN mkdir /app

COPY --from=package /home/gradle/gpcc-mocks/build/libs/gpcc-mocks.jar /app/gpcc-mocks.jar

USER 65534

ENTRYPOINT ["java", "-jar", "/app/gpcc-mocks.jar"]
