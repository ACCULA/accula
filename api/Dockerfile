FROM gradle:jdk16 AS build
ARG GPR_USERNAME
ARG GPR_TOKEN
ENV GPR_USERNAME=$GPR_USERNAME GPR_TOKEN=$GPR_TOKEN
WORKDIR /app
COPY . /app
RUN gradle bootJar --no-daemon

FROM openjdk:16-alpine
RUN apk update && apk upgrade && \
    apk add --no-cache git
COPY --from=build /app/api/build/libs/*.jar accula.jar
ENTRYPOINT ["java","--add-opens","java.base/java.util=ALL-UNNAMED","-jar","/accula.jar"]
