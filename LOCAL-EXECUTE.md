# 로컬 실행 헬퍼 

```shell
java -DPEKKO_HOSTNAME=127.0.0.1 \
     -DPEKKO_PORT=17355 \
     -Dserver.port=8080 \
     -jar build/libs/contraband.jar
```

```shell
java -DPEKKO_HOSTNAME=127.0.0.1 \
     -DPEKKO_PORT=17356 \
     -Dserver.port=8081 \
     -jar build/libs/contraband.jar
```
