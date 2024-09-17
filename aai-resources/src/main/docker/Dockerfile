FROM eclipse-temurin:11-jre-alpine
USER nobody

ENV SERVER_PORT=8447
EXPOSE ${SERVER_PORT}

# Add the proper files into the docker image from your build
WORKDIR /opt/app/aai-resources

COPY --chown=nobody:nobody /maven/aai-resources/ .

ENTRYPOINT ["/bin/sh", "/opt/app/aai-resources/docker-entrypoint.sh"]
