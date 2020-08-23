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

## kubernetes logging pipeline
### 1. Running kafka
````shell script
> helm install --name my-kafka --namespace kafka incubator/kafka
> kubectl get pod,svc -n kafka
  NAME                       READY   STATUS    RESTARTS   AGE
  pod/my-kafka-0             1/1     Running   2          4m14s
  pod/my-kafka-1             1/1     Running   0          116s
  pod/my-kafka-2             1/1     Running   0          78s
  pod/my-kafka-zookeeper-0   1/1     Running   0          4m14s
  pod/my-kafka-zookeeper-1   1/1     Running   0          3m32s
  pod/my-kafka-zookeeper-2   1/1     Running   0          3m
  NAME                                  TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)                      AGE
  service/my-kafka                      ClusterIP   10.108.104.66   <none>        9092/TCP                     4m14s
  service/my-kafka-headless             ClusterIP   None            <none>        9092/TCP                     4m14s
  service/my-kafka-zookeeper            ClusterIP   10.97.205.63    <none>        2181/TCP                     4m14s
  service/my-kafka-zookeeper-headless   ClusterIP   None            <none>        2181/TCP,3888/TCP,2888/TCP   4m14s
````
### 2. Running elasticsearch
````shell script
> kubectl apply -f ./kube-logging/fluentd-elasticsearch/elasticsearch.yaml
> kubectl get pod,svc -n elk-stack
  NAME                                 READY   STATUS    RESTARTS   AGE
  pod/elasticsearch-654c5b6b77-l8k2z   1/1     Running   0          50s
  NAME                    TYPE        CLUSTER-IP     EXTERNAL-IP   PORT(S)    AGE
  service/elasticsearch   ClusterIP   10.101.27.73   <none>        9200/TCP   50s
````
### 3. Running kibana
````shell script
> kubectl apply -f ./kube-logging/fluentd-elasticsearch/kibana.yaml
> kubectl get pod,svc -n elk-stack | grep kibana
  NAME                                 READY   STATUS    RESTARTS   AGE
  pod/kibana-6d474df8c6-fsfc7          1/1     Running   0          24s
  NAME                                 READY   STATUS    RESTARTS   AGE
  service/kibana          NodePort    10.97.240.55   <none>        5601:30578/TCP   24s
````
### 4. Running logstash
````shell script
> kubectl apply -f ./kube-logging/fluentd-elasticsearch/logstash.yaml
> kubectl get pod,svc -n elk-stack | grep logstash
  NAME                                       READY   STATUS    RESTARTS   AGE  
  pod/logstash-deployment-556cfb66b5-6xrs6   1/1     Running   0          34s
  service/logstash-service   ClusterIP   10.96.13.170   <none>        5044/TCP         33s
````
### 5. Running fluentd
````shell script
> kubectl apply -f ./kube-logging/fluentd-kafka/fluentd-kafka-daemonset.yaml
> kubectl get pod,daemonset -n kube-system | grep fluentd
  NAME                                         READY   STATUS    RESTARTS   AGE
  pod/fluentd-bqmnl                            1/1     Running   0          34s
  daemonset.extensions/fluentd      1         1         1       1            1           <none>                        34s
````
### 6. Running application
````shell script
> kubectl apply -f ./kube-resource/deployment-sample.yaml
> kubectl get pod
  NAME                                 READY   STATUS    RESTARTS   AGE
  sample-deployment-5fbf569554-4pzrf   0/1     Running   0          17s
````

### 7. Request to application
```shell script
> kubectl get svc -n ingress-nginx
  NAME                                 TYPE        CLUSTER-IP     EXTERNAL-IP   PORT(S)                      AGE
  ingress-nginx-controller             NodePort    10.97.27.106   <none>        80:30431/TCP,443:31327/TCP   21d
  ingress-nginx-controller-admission   ClusterIP   10.96.76.113   <none>        443/TCP                      21d
> curl localhost:30431/api
```

### 8. Search logging
