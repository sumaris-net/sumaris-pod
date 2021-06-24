FROM openjdk:8-jdk-alpine
ARG WAR_FILE=app.war
ARG CONFIG_FILES=config/*.properties
RUN apt-get update && apt-get -y install libsodium-dev
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
RUN mkdir /app
COPY app.sh /app/app.sh
RUN chmod +x /app/app.sh
COPY ${WAR_FILE} /app/app.war
COPY ${CONFIG_FILES} /app/config/
ENV PORT=8080 \
    TZ=UTC \
    PROFILES=default
WORKDIR /app
EXPOSE ${PORT}
ENTRYPOINT ["app.sh"]