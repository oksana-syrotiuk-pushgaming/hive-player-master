FROM justb4/jmeter:5.1.1

RUN mkdir -p /opt/performance_test/test_results

WORKDIR /opt/performance_test/

COPY hive_player_performance.jmx .
COPY entrypoint.sh .
RUN chmod +x ./entrypoint.sh
ENTRYPOINT ./entrypoint.sh