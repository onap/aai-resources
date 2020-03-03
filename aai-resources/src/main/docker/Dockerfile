FROM @aai.docker.namespace@/aai-common-@aai.base.image@:@aai.base.image.version@

RUN  mkdir -p /opt/aaihome/aaiadmin /opt/aai/logroot/AAI-RES

#VOLUME /opt/aai/logroot/AAI-RES
VOLUME /tmp
VOLUME /opt/tools

HEALTHCHECK --interval=40s --timeout=10s --retries=3 CMD nc -z -v localhost ${SERVER_PORT:-8447} || exit 1

# Add the proper files into the docker image from your build

ENV AAI_BUILD_VERSION @aai.docker.version@
# Expose the ports for outside linux to use
# 8447 is the important one to be used
EXPOSE 8447

RUN groupadd aaiadmin -g 1000
RUN adduser -u 1000 -h /opt/aaihome/aaiadmin -S -D -G aaiadmin -s /bin/bash aaiadmin

WORKDIR /opt/app/aai-resources

RUN chown -R aaiadmin:aaiadmin /opt/app/aai-resources /etc/profile.d /opt/aai/logroot/AAI-RES

COPY --chown=aaiadmin:aaiadmin /maven/aai-resources/ .

USER aaiadmin

ENTRYPOINT ["/bin/bash", "/opt/app/aai-resources/docker-entrypoint.sh"]
