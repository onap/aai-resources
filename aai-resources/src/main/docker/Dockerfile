FROM aaionap/aai-common:@aai.docker.version@-@aai.base.image@

RUN  mkdir -p /opt/aaihome/aaiadmin /opt/aai/logroot/AAI-RES

VOLUME /opt/aai/logroot/AAI-RES
VOLUME /tmp
VOLUME /opt/tools

HEALTHCHECK --interval=40s --timeout=10s --retries=3 CMD nc -z -v localhost ${SERVER_PORT:-8447} || exit 1

# Add the proper files into the docker image from your build
WORKDIR /opt/app/aai-resources
COPY /maven/aai-resources/ .

ENV AAI_BUILD_VERSION @aai.docker.version@
# Expose the ports for outside linux to use
# 8447 is the important one to be used
EXPOSE 8447
ENTRYPOINT ["/bin/bash", "/opt/app/aai-resources/docker-entrypoint.sh"]