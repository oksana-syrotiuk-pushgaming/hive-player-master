FROM azul/zulu-openjdk-alpine:11

LABEL org.opencontainers.image.source=https://github.com/gsi-io/hive-player-service

ARG ARTIFACT
ARG VERSION

ENV ARTIFACT ${ARTIFACT}
ENV VERSION ${VERSION}

ENV CONSUL_HOST consul-svc.default.svc.cluster.local

ENV DEPLOY_TYPE k8s

RUN mkdir -p /opt/hive/player/service/config

WORKDIR /opt/hive/player/service
ADD target/${ARTIFACT}-${VERSION}.jar /opt/hive/player/service/${ARTIFACT}-${VERSION}.jar
COPY src/main/resources/docker/bootstrap.k8s.properties config/
COPY src/main/resources/docker/bootstrap.ecs.properties config/

COPY src/main/resources/docker/entrypoint.sh /opt/hive/player/service
RUN chmod +x ./entrypoint.sh

ENTRYPOINT ./entrypoint.sh
