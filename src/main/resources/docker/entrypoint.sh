#!/bin/sh
echo "Starting Hive Player Service"
exec java ${JAVA_OPTS} -Dspring.cloud.bootstrap.name=bootstrap.${DEPLOY_TYPE} -jar ${ARTIFACT}-${VERSION}.jar
