# Faculty Leave Management System — runtime image.
# "Build once, deploy many": the image wraps the WAR that already passed the unit + Selenium tests.
#   mvn -B package -DskipTests && docker build -t flms:1.1.0 .
FROM eclipse-temurin:17-jre-alpine

ARG VERSION=dev
LABEL org.opencontainers.image.title="flms" \
      org.opencontainers.image.description="Faculty Leave Management System" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.authors="Nidhi Bhawari"

# Run as an unprivileged user; /app/data holds the H2 database file
RUN addgroup -S flms && adduser -S -G flms flms \
 && mkdir -p /app/data && chown -R flms:flms /app
WORKDIR /app
COPY --chown=flms:flms target/flms.war /app/flms.war
USER flms

ENV APP_ENV=docker \
    PORT=8080 \
    DB_URL=jdbc:h2:file:/app/data/flms \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
VOLUME ["/app/data"]

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/flms.war"]
