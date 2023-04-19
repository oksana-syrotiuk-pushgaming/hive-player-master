#!/bin/sh
cd /opt/performance_test || exit
jmeter -n -t hive_player_performance.jmx \
-JhivePlayerHost="${HIVE_PLAYER_HOST:-hive-player-service-v1.default.svc.cluster.local}" \
-JhivePlayerPort="${HIVE_PLAYER_PORT:-9001}" \
-JhivePlayerPathPrefix="${HIVE_PLAYER_API_PATH:-/hive/s2s/platform/player/v1}" \
-JusersNumber="${USERS_NUMBER:-100}" \
-JplaysPerUser="${PLAYS_PER_USER:-20}" \
-JrampUpSeconds="${RAMPUP_SEC:-60}" \
-l /opt/performance_test/test_results/results.jtl

echo "Test complete, waiting for termination marker file. File path: '/opt/performance_test/terminate'"
while [ ! -f /opt/performance_test/terminate ]
do
  sleep 1
done
echo "Done."

