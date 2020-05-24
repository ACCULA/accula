FROM gradle:jdk14 AS build
WORKDIR /app
COPY . /app
RUN gradle build -x test -x web:build --no-daemon

FROM openjdk:14-alpine
COPY --from=build /app/code/build/libs/*.jar code.jar
ENTRYPOINT ["java","-jar","/code.jar"]
