FROM gradle:8.0.2-jdk19-alpine AS build
ARG GPR_USERNAME
ARG GPR_TOKEN
ENV GPR_USERNAME=$GPR_USERNAME GPR_TOKEN=$GPR_TOKEN
WORKDIR /app
COPY . /app
RUN gradle bootJar --no-daemon

FROM bellsoft/liberica-openjdk-alpine:19.0.2
RUN apk update && apk upgrade && \
    apk add --no-cache git
COPY --from=build /app/api/build/libs/*.jar accula.jar
ENTRYPOINT ["java","--enable-preview","-jar","/accula.jar"]
