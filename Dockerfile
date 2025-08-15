# Use an official Java runtime as a parent image
FROM openjdk:21-jdk-slim

WORKDIR /opt/app

ARG JAR_FILE=target/*.jar

EXPOSE ${SERVER_PORT}

COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java","-jar","app.jar"]