FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
ENV TZ=Asia/Kolkata
WORKDIR /app
RUN mkdir -p /app/logs
COPY --from=build /workspace/target/top-5g-pm-file-sync*.jar /app/app.jar
EXPOSE 8520
ENTRYPOINT ["sh","-c","java ${JAVA_OPTS} -jar /app/app.jar"]