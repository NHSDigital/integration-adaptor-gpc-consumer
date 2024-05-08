FROM gradle:8.7-jdk17 AS build

COPY --chown=gradle:gradle service /home/gradle/service

WORKDIR /home/gradle/service

RUN gradle classes

FROM build AS package

RUN gradle --build-cache bootJar

FROM eclipse-temurin:17-jre-jammy

EXPOSE 8080

RUN mkdir /app

COPY --from=package /home/gradle/service/build/libs/gpc-consumer.jar /app/gpc-consumer.jar

USER 65534

ENTRYPOINT ["java","-Dreactor.netty.http.server.accessLogEnabled=true", "-jar", "/app/gpc-consumer.jar"]
