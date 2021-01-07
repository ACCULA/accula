FROM gradle:jdk15 AS build
WORKDIR /app
COPY . /app
RUN gradle bootJar --no-daemon

FROM openjdk:15-alpine
RUN apk update && apk upgrade && \
    apk add --no-cache git
COPY --from=build /app/api/build/libs/*.jar accula.jar
ENTRYPOINT ["java","--enable-preview","-jar","/accula.jar"]
