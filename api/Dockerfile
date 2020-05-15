FROM gradle:jdk14 AS build
WORKDIR /api
COPY . /api
RUN gradle build -x test -x web:build --no-daemon

FROM openjdk:14-alpine
COPY --from=build /api/build/libs/*.jar accula.jar
ENTRYPOINT ["java","-jar","/accula.jar"]
