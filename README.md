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
쿠버네티스에서 볼륨을 사용하는 구조는 PV와 PVC로 분리되어있다. PV는 볼륨 자체를 뜻하고 클러스터 안에서 자원으로 다룬다. 파드하고는 별개로 관리되고 별도의 생명주기가 있다.

PVC는 사용자가 PV에 하는 요청이다. 사용하고 싶은 용량은 얼마인지, 읽기/쓰기는 어떤 모드로 설정하고 싶은지 등을 정하여 요청한다. 
쿠버네티스는 이처럼 파드에 볼륨을 직접 할당하는 형태가 아니라, 중간에 PVC를 두어 파드와 파드가 사용할 스토리지를 분리했다.

- 다양한 스토리지를 PV로 사용할 수 있지만, 파드에 직접 연결하는 것이 아니고 PVC를 거쳐 사용하므로 파드는 어떤 스토리지를 사용하는지 신경쓸 필요가 없다.

*PV와 PVC 생명주기*
<div>
  <img align="center" width="500" src="https://t1.daumcdn.net/cfile/tistory/99705F485B796C433A">
</div>
<br>

- Provisioning: PV를 만드는 단계를 뜻한다.
    - static provisioning: 미리 PV를 만들어 두고 사용자의 요청이 있으면 미리 만들어둔 PV를 할당한다.(보통 스토리지 용량의 제한이 있을때 사용한다.)
    - dynamic provisioning: 사용자가 PVC를 거쳐 PV를 요청했을 때, PV를 생성해 제공한다.
- Binding: 바인딩은 프로비저닝으로 만든 PV를 PVC와 연결하는 단계이다. PVC에서 원하는 스토리지의 용량과 접근 방법을 명시해서 요쳥하면 맞는 PV가 할당된다.
이때 PVC에서 원하는 PV가 없다면 요청은 실패하고, PVC에서 원하는 PV가 생길 때까지 대기하다가 PVC에 바인딩된다.(PVC 하나에 여러 PV가 매핑될 수 없다.)
- Using: PVC는 파드에 설정되고 파드는 PVC를 볼륨으로 인식해서 사용한다. 할당된 PVC는 파드를 유지하는 동안 계속 사용하며 시스템에서 임의로 삭제할 수 없다.
이 기능을 "Storage Object In Use Protection"이라 한다.
- Reclaiming: 사용이 끝난 PVC는 삭제되고 PVC를 사용하던 PV를 초기화하는 과정을 뜻한다. 초기화 정책으로는 아래와 같다.
    - Retain: PV를 그대로 보존한다. PVC가 삭제되면 사용 중이던 PV는 해제(released)상태라서 아직 다른 PVC가 재사용할 수 없다.(데이터는 아직 그대로 보존되어있다.)
    만약 해당 PV를 재사용하려면 아래와 같은 순서로 직접 초기화해줘야한다.
        1. PV삭제. 만약 PV가 외부 스토리지와 연결되어있다면 PV는 삭제되더라도 외부 스토리지의 볼륨은 그대로 남아있다.
        2. 외부 스토리지에 남은 데이터를 직접 정리한다.
        3. 남은 스토리지의 볼륨을 삭제하거나 재사용하려면 해당 볼륨을 이용하는 PV를 다시 만든다.
    - Delete: PV를 삭제하고 연결된 외부 스토리지 쪽의 볼륨도 삭제한다. 동적 프로비저닝은 기본적으로 해당 정책을 따른다.
    - Recycle: PV의 데이터들을 삭제하고 다시 새로운 PVC에서 PV를 사용할 수 있도록한다.

#### - persistent volume template
```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: persistent-volume
  namespace: levi-volume
spec:
  capacity:
    storage: 1Gi
  volumeMode: Filesystem
  accessModes:
    - ReadWriteOnce
  storageClassName: manual
  persistentVolumeReclaimPolicy: Delete
  hostPath:
    path: /tmp
```

- accessModes
    - ReadWriteOne: 노드 하나에만 볼륨을 읽기/쓰기하도록 마운트한다.
    - ReadOnlyMany: 여러 개 노드의 읽기 전용으로 마운트한다.
    - ReadWriteMany: 여러 개 노드에서 읽기/쓰기를 허용하도록 마운트한다.
- storageClassName: 스토리지 클래스를 설정하는 필드이고, PVC가 특정 스토리지 클래스를 명시하여 요청하면 해당 스토리지 클래스로 선언된 PV와 연결된다. 
만약 스토리지 클래스를 설정하지 않았다면, 특정 스토리지 클래스를 명시하지 않은 PVC가 요청하면 매핑된다.
- persistentVolumeReclaimPolicy: PV가 해제되었을 때의 초기화 옵션을 설정한다.(Retain/Recycle/Delete)
- .spec.hostPath: 해당 PV의 볼륨 플러그인을 명시한다.

```shell script
> kubectl apply -f ./kube-resource/persistent-volume-sample.yaml
> kubectl get pvc -n levi-volume
NAME                CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS      CLAIM   STORAGECLASS   REASON   AGE
persistent-volume   1Gi        RWO            Delete           Available           manual                  31s
```

- STATUS
    - Available: PVC에서 사용할 수 있는 상태
    - Bound: 특정 PVC에 연결된 상태
    - Released: PVC는 삭제되었고, PV는 아직 초기화되지 않은 상태
    - Failed: 자동 초기화를 실패한 상태
    
#### - persistent volume claim template
```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: persistent-volume-claim
  namespace: levi-volume-claim
spec:
  volumeMode: Filesystem
  accessModes:
    - ReadWriteOnce
  storageClassName: manual
  resources:
    requests:
      storage: 500Mi
```
- .spec.resources.requests.storage: 자원을 얼마나 사용할 것인지 명시한다.
PV의 용량보다 높다면, 할당되지 않고 Pending 상태가 된다.

```shell script
> kubectl apply -f ./kube-resource/persistent-volume-claim-sample.yaml
> kubectl get pvc -n levi-volume-claim
NAME                      STATUS   VOLUME              CAPACITY   ACCESS MODES   STORAGECLASS   AGE
persistent-volume-claim   Bound    persistent-volume   1Gi        RWO            manual         11s
> kubectl get pv -n levi-volume
NAME                CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS   CLAIM                                       STORAGECLASS   REASON   AGE
persistent-volume   1Gi        RWO            Delete           Bound    levi-volume-claim/persistent-volume-claim   manual                  4h44m
```
PVC와 PV가 binding 된 이후에는 각각 STATUS가 Bound 상태로 변경된다.

### Label로 PVC와 PV 연결하기
```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: persistent-volume
  namespace: levi-volume
  labels:
    location: local
spec:
  capacity:
    storage: 1Gi
  volumeMode: Filesystem
  accessModes:
    - ReadWriteOnce
  storageClassName: manual
  persistentVolumeReclaimPolicy: Delete
  hostPath:
    path: /tmp
```

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: persistent-volume-claim
  namespace: levi-volume-claim
spec:
  volumeMode: Filesystem
  accessModes:
    - ReadWriteOnce
  storageClassName: manual
  resources:
    requests:
      storage: 500Mi
  selector:
    matchLabels:
      location: local
```

### pod에 persistent volume mount
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: sample-deployment
spec:
  replicas: 2
  selector:
    matchLabels:
      app: springboot-web
  template:
    metadata:
      labels:
        app: springboot-web
    spec:
      containers:
        - name: springboot-web
          image: 1223yys/springboot-web:0.2.5
          imagePullPolicy: Always
          ports:
            - containerPort: 8080
          volumeMounts:
            - mountPath: /test-volume
              name: persistent-volume
          env:
            - name: SERVER_PORT
              valueFrom:
                configMapKeyRef:
                  name: config-dev
                  key: WEP-APP-PORT
          livenessProbe:
            httpGet:
              port: 9090
              path: /api
            initialDelaySeconds: 60
          readinessProbe:
            httpGet:
              port: 9090
              path: /api
            initialDelaySeconds: 60
      volumes:
        - name: persistent-volume
          persistentVolumeClaim:
            claimName: persistent-volume-claim
```
만약 위 예제에 만들어 놓은 persistent volume claim에 마운트를 하면, pod은 뜨지 못하고 pending 상태가 되는데 그 이유는
"클러스터는 클레임을 사용하는 pod와 동일한 네임스페이스에 있어야 한다." 이다. 위 deployment의 네임 스페이스를 default 이므로, 
default namespace에 persistent volume claim을 하나 만들어 놓아야한다.