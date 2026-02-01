FROM eclipse-temurin:17-jre-alpine

ENV SERVER_PORT=8447
ENV JAVA_TOOL_OPTIONS="-javaagent:/opt/app/opentelemetry/opentelemetry-javaagent.jar"
EXPOSE ${SERVER_PORT}

WORKDIR /opt/app/aai-resources

# To have this reproducible we are using an explicit version, effectively
# this should always be the latest version available
# https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases
ARG AGENT_VERSION=2.24.0

RUN mkdir -p /opt/app/opentelemetry && \
    wget -O /opt/app/opentelemetry/opentelemetry-javaagent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${AGENT_VERSION}/opentelemetry-javaagent.jar && \
    chown -R nobody:nobody /opt/app

USER nobody

COPY --chown=nobody:nobody /maven/aai-resources/ .

ENTRYPOINT ["/bin/sh", "/opt/app/aai-resources/docker-entrypoint.sh"]
