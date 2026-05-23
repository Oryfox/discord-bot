FROM maven:3-amazoncorretto-25-debian AS maven
WORKDIR /build
COPY . .
RUN mvn clean package

FROM amazoncorretto:25-alpine
ARG COMMIT
RUN echo ${COMMIT} >> version
COPY --from=maven /build/target/*.jar /api.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/api.jar"]