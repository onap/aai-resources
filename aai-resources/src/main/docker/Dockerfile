FROM @aai.docker.namespace@/aai-common-@aai.base.image@:@aai.base.image.version@

USER root
RUN  mkdir -p /opt/aaihome/aaiadmin /opt/aai/logroot/AAI-RES /opt/bulkprocess_load
RUN chown -R aaiadmin:aaiadmin /opt/aaihome /opt/aai/logroot/AAI-RES /opt/bulkprocess_load


VOLUME /tmp
VOLUME /opt/tools

HEALTHCHECK --interval=40s --timeout=10s --retries=3 CMD nc -z -v localhost ${SERVER_PORT:-8447} || exit 1



ENV AAI_BUILD_VERSION @aai.docker.version@
# Expose the ports for outside linux to use
# 8447 is the important one to be used
EXPOSE 8447

# Add the proper files into the docker image from your build
WORKDIR /opt/app/aai-resources

RUN chown -R aaiadmin:aaiadmin /opt/app/aai-resources /etc/profile.d /opt/aai/logroot/AAI-RES /opt/bulkprocess_load

COPY --chown=aaiadmin:aaiadmin /maven/aai-resources/ .

USER aaiadmin

ENTRYPOINT ["/bin/bash", "/opt/app/aai-resources/docker-entrypoint.sh"]
