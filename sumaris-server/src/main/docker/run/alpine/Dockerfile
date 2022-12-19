FROM eclipse-temurin:17-jdk-alpine
ARG WAR_FILE=app.war
ARG CONFIG_FILES=config/*.properties
RUN apk update && apk install libsodium-dev
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
WORKDIR /app
COPY app.sh app.sh
RUN chmod +x app.sh
COPY ${WAR_FILE} app.war
COPY ${CONFIG_FILES} config/
ENV PORT=8080 \
    TZ=UTC \
    PROFILES=default
EXPOSE ${PORT}
ENTRYPOINT ["app.sh"]