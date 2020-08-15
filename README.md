# Kubernetes sample(쿠버네티스 manifast 샘플)

<div>
  <img width="1000" src="https://subicura.com/assets/article_images/2019-05-19-kubernetes-basic-1/kubernetes-logo.png">
</div>

## ./kube-resource/.. -> kubernetes manifast sample files in directory

## apply labels to node(쿠버테티스 노드에 레이블 적용하기)
```shell script
#show node's labels
> kubectl get nodes --show-labels
#create label to node
> kubectl label nodes <node_name> <label_key>=<label_vale>
ex)
> kubectl label nodes docker-desktop disk_type=ssd
#delete label
> kubectl label nodes <node_name> <label_key>-
ex)
> kubectl label nodes docker-desktop dist_type-
```

## apply node selector(노드셀렉터 적용)
### 특정 노드에만 팟이 배포되도록 스케쥴링하는 방법에는 노드셀렉터를 적용할 수 있다.
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: springboot-web
spec:
  containers:
  - name: springboot-web
    image: 1223yys/springboot-web:0.1.6
    ports:
    - containerPort: 8080
  nodeSelector:
    disk_type: ssd
```

## Volume(볼륨)
### 1.emptyDir
- pod가 실행되는 호스트의 디스크를 임시로 컨테이너에 볼륨으로 할당해서 사용하는 방법이다.
- pod가 사라지면 emptyDir에 할당해서 사용했던 볼륨의 데이터로 함께 사라진다.
- container에 문제가 생겨 재시작되면, 데이터는 유지된다.(pod가 재시작 혹은 삭제된 것은 아니기 때문)
- 주로 대용량 데이터 계산에 사용한다.(중간 연산의 결과를 임시저장하는 정도의 용도)
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: springboot-web
spec:
  containers:
  - name: springboot-web
    image: 1223yys/springboot-web:0.1.6
    ports:
    - containerPort: 8080
    volumeMounts:
      - mountPath: /emptyDir
        name: emptyDir_vol
  volumes:
    - name: emptyDir_vol
      emptyDir: {}
```

### 2.hostPath
- pod가 실행된 호스트의 파일이나 디렉터리를 pod에 마운트한다.
- emptyDir가 임시 디렉터리를 마운트하는 것이라면, hostPath는 호스트에 있는 실제 파일이나 디렉터리를 마운트한다.
- pod를 재시작하더라도, 데이터가 보존된다.
- /var/lib/docker 같은 도커 시스템용 디렉터리를 컨테이너에 마운트하여 시스템 모니터링 등을 진행할때 사용하기도 한다.
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: springboot-web
spec:
  containers:
  - name: springboot-web
    image: 1223yys/springboot-web:0.1.6
    ports:
    - containerPort: 8080
    volumeMounts:
      - mountPath: /test-volume
        name: hostPath-vol
  volumes:
    - name: hostPath-vol
      hostPath:
        path: /tmp
        type: Directory
```
실제로 볼륨이 잘 마운트 되었는지 확인해보자.
```shell script
> kubectl exec <pod-name> -it sh
> cd /test-volume
> touch test.txt
> exit
> ls /tmp
```

### 2.1.hostpath volume type
- null: hostPath 볼륨을 마운트하기 전에 아무것도 확인하지 않는다.
- DirectoryOrCreate: 설정한 경로에 디렉터리가 없으면 퍼미션이 755인 빈 디렉터리를 만든다.
- Directory: 설정한 경로에 디렉터리가 존재해야한다. 호스트에 해당 디렉터리가 없으면 파드는 ContainerCreating 상태로 남고 생성이 안된다.
- FileOrCreate: 설정한 경로에 파일이 없으면 퍼미션이 644인 빈 파일을 만든다.
- File: Directory와 동일
- Socket: 설정한 경로에 유닉스 소켓 파일이 있어야한다.
 
## Persistent volume & Persistent volume claim