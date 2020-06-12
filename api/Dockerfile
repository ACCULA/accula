FROM gradle:jdk14 AS build
WORKDIR /app
COPY . /app
RUN gradle build -x test -x web:build --no-daemon

FROM openjdk:14-alpine
COPY --from=build /app/api/build/libs/*.jar accula.jar
ENTRYPOINT ["java","--enable-preview","-jar","/accula.jar"]
