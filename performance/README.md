# hive-player-service performance tests

The performance tests are built using jmeter
They can be run on command line or as docker image or as a k8s job running the docker image.
The results can be collected from the pod to be viewed and analysed on the jmeter desktop application.





## Running jmeter performance test directly

```
jmeter -n -t hive_player_performance.jmx \
-JhivePlayerHost=localhost \
-JhivePlayerPort=9001 \
-JhivePlayerPathPrefix="/hive/s2s/platform/player/v1" \
-JhivePlayerApiKey="key" \
-JusersNumber=2 \
-JplaysPerUser=20 \
-JrampUpSeconds=2 \
-l results.jtl
```

## Building a docker image of the performance test

```
docker build -f Dockerfile -t gsiio/hive-player-server-perf-test:1.0.0 .
```

## Running the performance test docker image
```
docker run \
--env HIVE_PLAYER_HOST=127.0.0.1 \
--network="host" \
--name="hive-player-perf-test" \
-it gsiio/hive-player-perf-test:1.0.0
```

## Retrieving results from docker container 
```
docker cp 'hive-player-perf-test:/opt/performance_test/test_results/results.jtl' .
```

## Stopping performance test in docker container
```
touch terminate && docker cp terminate 'hive-player-perf-test:/opt/performance_test/terminate'
```

## Creating and starting k8s performance job
```
kubectl apply -f performance_job.yaml
```

## Retrieving results from k8s pod
```
kubectl cp {hive-player-performance-test-pod-name}:/opt/performance_test/test_results/results.jtl results.jtl
```
