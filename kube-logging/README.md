# Kubernetes logging
## Pod log tailing
#### kubectl log 명령으로 로그를 출력한다. 로그 옵션으로는 아래와 같다.
- -c, --container <container name>: 해당 컨테이너의 로그를 출력한다. 단독으로는 사용하지 않고, 특정 pod 안에 하나의 컨테이너가 아니고,
사이드카처럼 두개 이상의 컨테이너가 떠있을 때, 해당 팟중 원하는 컨테이너의 로그만 출력할때 사용한다.
- -f, --follow: tail하는 옵션이다.
- --tail=n: 최근 n번째 line부터 tail한다.
### deployment로 관리되는 pod들 로그 출력
```shell script
> kubectl log -f --tail=100 deployment/deploymentName
```

### 특정 팟 로그 출력
```shell script
> kubectl log -f --tail=100 <pod name>
```

### pod 내 특정 컨테이너 로그만 출력(주로 사이드카가 적용되 두개 이상 컨테이너가 하나의 팟으로 떠있을 경우, 앱 로그만 출력하기 위해 사용)
```shell script
> kubectl log -f --tail=100 <pod name> -c <container name>
```

## 클러스터 레벨 로깅
쿠버네티스에서는 생명 주기(컨테이너, 파드, 노드의 생명주기)와 분리된 스토리지를 구축하는 아키텍쳐를 클러스터 레벨 로깅이라고 한다.

### 1. 컨테이너 로그
컨테이너의 로그 수집은 컨테이너 런타임(도커)이 담당한다. 앱 컨테이너가 stdout과 stderr이라는 표준 스트림으로 로그를 출력한다면 컨테이너 런타임은
표준 스트림 2개를 특정 로그 드라이버로 리다이렉트하도록 설정되어 있다. 도커는 다양한 로그 드라이버를 지원하며 기본으로 json-file을 사용한다.

 ```shell script
 > docker inspect <container id>
 ```

```
"LogPath": "/var/lib/docker/containers/../xxx-json.log",
...
"HostConfig": {
    "LogConfig": {
        "Type": "json-file",
        "Config": {
            "max-file": "5",
            "max-size": "1m"
        }
    }
...
}
```
LogPath는 컨테이너 관련 메타 데이터들을 저장한 심볼릭링크이다. LogConfig는 로그 설정이다. 타입은 json-file, 파일 최대 개수는 5개, 용량은 1메가로 설정되어있다.

### 컨테이너 로그를 보는 방법
```shell script
> docker ps
CONTAINER ID        IMAGE                    COMMAND                  
f37d8ab3bde4        1223yys/springboot-web   "java -jar /app/app.…"
> docker run -it --rm -v /var/lib/docker/containers:/json-log alpine ash
> cd json-log
f37d8ab3bde432a3aa8ffc47a75249da07cc77820575c7faa27ea826b6b78ee8
> cd f37d8ab3bde432a3aa8ffc47a75249da07cc77820575c7faa27ea826b6b78ee8
> ls
checkpoints                   f37d8ab3bde432a3aa8ffc47a75249da07cc77820575c7faa27ea826b6b78ee8-json.log
config.v2.json                hostconfig.json
```
위 명령을 실행하면, 컨테이너 ID로 여러 디렉토리가 존재하고 그 디렉토리 안에 표준 출력으로 출력했던 로그들이 파일 형태로 저장되어 있다.

## Fluentd
참조: [Fluentd daemonset sample github address](https://github.com/fluent/fluentd-kubernetes-daemonset)
### Fluentd + Kafka + ELK log pipeline 구성
참조: [kubernetes kafka install](https://github.com/TheOpenCloudEngine/uEngine-cloud-k8s/wiki/Kafka-on-kubernetes)
<br>
<br>
#### installing helm
```shell script
> curl https://raw.githubusercontent.com/kubernetes/helm/master/scripts/get | bash
> kubectl --namespace kube-system create sa tiller
> kubectl create clusterrolebinding tiller --clusterrole cluster-admin --serviceaccount=kube-system:tiller
> helm init --service-account tiller
> helm repo update
```
#### deleteing chart
```shell script
# --purge 옵션으로 관련된 모든 정보를 지운다. 
helm delete my-kafka --purge
```
#### running kafka using helm chart
```shell script
> kubectl create ns kafka
> helm repo add incubator http://storage.googleapis.com/kubernetes-charts-incubator
> helm install --name my-kafka --namespace kafka incubator/kafka
```
#### creating fluentd out topic
```shell script
> kubectl -n kafka exec my-kafka-0 -- /usr/bin/kafka-topics \
--zookeeper my-kafka-zookeeper:2181 --topic fluentd-container-logging \
--create --partitions 3 --replication-factor 3

Created topic "fluentd-container-logging".
```
#### topic list
```shell script
> kubectl -n kafka exec my-kafka-0 -- /usr/bin/kafka-topics --zookeeper my-kafka-zookeeper:2181 --list

fluentd-container-logging
```
#### listening for messages on "fluentd-container-log" topic
```shell script
> kubectl -n kafka exec -ti my-kafka-0 -- /usr/bin/kafka-console-consumer \
--bootstrap-server my-kafka:9092 --topic fluentd-container-logging --from-beginning
```
#### select consumer&offset
```shell script
> kubectl -n kafka exec my-kafka-0 -- /usr/bin/kafka-consumer-groups \
--bootstrap-server my-kafka:9092 --group foo --describe
> kubectl -n kafka exec my-kafka-0 -- /usr/bin/kafka-topics \
--zookeeper my-kafka-zookeeper:2181 --topic test1 --describe
```
#### start console for producing message
```shell script
> kubectl -n kafka exec -ti my-kafka-0 -- /usr/bin/kafka-console-producer \
--broker-list my-kafka:9092 --topic test1
```
