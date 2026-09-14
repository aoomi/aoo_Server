FROM eclipse-temurin:25-jre

ARG APP_UID=10001
ARG APP_GID=10001
RUN groupadd --gid "${APP_GID}" aoo && useradd --uid "${APP_UID}" --gid aoo --no-create-home --shell /usr/sbin/nologin aoo

WORKDIR /app
COPY --chown=aoo:aoo runtime/ /app/

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError" \
    AOO_CONFIG_DIR=/etc/aoo \
    AOO_SECRET_DIR=/run/secrets
USER 10001:10001
ENTRYPOINT ["/app/bin/entrypoint.sh"]
