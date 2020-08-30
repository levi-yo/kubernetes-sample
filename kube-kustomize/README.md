# Kustomize를 이용한 kubernetes 오브젝트 관리
Kustomize는 kustomization 파일을 이용해 kubernetes 오브젝트를 사용자가 원하는 대로 변경(customize)하는 도구이다.

kustomization 파일을 포함하는 디렉터리 내의 리소스를 보려면 다음 명령어를 이용한다.
````shell script
#kustomize가 적용된 설정파일 결과를 보여준다.
> kubectl kustomize <kustomization_directory>
#실제 kustomize 리소스를 클러스터에 적용한다.
> kubectl apply -k <kustomization_directory>
````

## Kustomize
Kustomize는 쿠버네티스 구성을 사용자 정의화하는 도구이다. 이는 애플리케이션 구성 파일을 관리하기 위해 다음 기능들을 가진다.

- 다른 소스에서 리소스 생성
- 리소스에 대한 교차 편집 필드 설정
- 리소스 집합을 구성하고 사용자 정의

## 교차 편집 필드 설정
프로젝트 내 모든 쿠버네티스 리소스에 교차 편집 필드를 설정하는 것은 꽤나 일반적이다. 교차 편집 필드를 설정하는 몇 가지 사용 사례는 다음과 같다.

- 모든 리소스에 동일한 네임스페이스를 설정
- 동일한 네임 접두사 또는 접미사를 추가
- 동일한 레이블들을 추가
- 동일한 어노테이션들을 추가

kube-kustomize/kustomize-upsert-field 참조

```shell script
# deployment.yaml을 생성
cat <<EOF >./deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
  labels:
    app: nginx
spec:
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx
EOF

cat <<EOF >./kustomization.yaml
namespace: my-namespace
namePrefix: dev-
nameSuffix: "-001"
commonLabels:
  app: bingo
commonAnnotations:
  oncallPager: 800-555-1212
resources:
- deployment.yaml
EOF
```

```shell script
> kubectl kustomize ./kube-kustomize/kustomize-upsert-field
```

````yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  annotations:
    oncallPager: 800-555-1212
  labels:
    app: bingo
  name: dev-nginx-deployment-001
  namespace: my-namespace
spec:
  selector:
    matchLabels:
      app: bingo
  template:
    metadata:
      annotations:
        oncallPager: 800-555-1212
      labels:
        app: bingo
    spec:
      containers:
      - image: nginx
        name: nginx
````

## 구성(composition)
한 파일에 deployment, service 등을 정의하는 것은 일반적이다. kustomize는 서로 다른 리소스들을 하나의 파일로 구성할 수 있게 지원한다.

kube-kustomize/kustomize-composition 참조

```shell script
# deployment.yaml 파일 생성
cat <<EOF > deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-nginx
spec:
  selector:
    matchLabels:
      run: my-nginx
  replicas: 2
  template:
    metadata:
      labels:
        run: my-nginx
    spec:
      containers:
      - name: my-nginx
        image: nginx
        ports:
        - containerPort: 80
EOF

# service.yaml 파일 생성
cat <<EOF > service.yaml
apiVersion: v1
kind: Service
metadata:
  name: my-nginx
  labels:
    run: my-nginx
spec:
  ports:
  - port: 80
    protocol: TCP
  selector:
    run: my-nginx
EOF

# 이들을 구성하는 kustomization.yaml 생성
cat <<EOF >./kustomization.yaml
resources:
- deployment.yaml
- service.yaml
EOF
```

```shell script
> kubectl kustomize /kube-kustomize/kustomize-composition
```

```yaml
apiVersion: v1
kind: Service
metadata:
  labels:
    run: my-nginx
  name: my-nginx
spec:
  ports:
    - port: 80
      protocol: TCP
  selector:
    run: my-nginx
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-nginx
spec:
  replicas: 2
  selector:
    matchLabels:
      run: my-nginx
  template:
    metadata:
      labels:
        run: my-nginx
    spec:
      containers:
        - image: nginx
          name: my-nginx
          ports:
            - containerPort: 80
```

## 사용자 정의(user patch define)
패치는 리소스에 다른 사용자 정의를 적용하는 데 사용할 수 있다. Kustomize는 patchesStrategicMerge와 patchesJson6902를 통해 서로 다른 패치 메커니즘을 지원한다. 
patchesStrategicMerge는 파일 경로들의 리스트이다. 각각의 파일은 patchesStrategicMerge로 분석될 수 있어야 한다. 
패치 내부의 네임은 반드시 이미 읽혀진 리소스 네임(ex. deployment.yaml 안의 이름)과 일치해야 한다. 
한 가지 일을 하는 작은 패치가 권장된다. 예를 들기 위해 디플로이먼트 레플리카 숫자를 증가시키는 하나의 패치와 메모리 상한을 설정하는 다른 패치를 생성한다.

/kube-kustomize/kustomize-patchesStrategicMerge

/kube-kustomize/kustomize-patchsJson6902

````shell script
# deployment.yaml 파일 생성
cat <<EOF > deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-nginx
spec:
  selector:
    matchLabels:
      run: my-nginx
  replicas: 2
  template:
    metadata:
      labels:
        run: my-nginx
    spec:
      containers:
      - name: my-nginx
        image: nginx
        ports:
        - containerPort: 80
EOF

# increase_replicas.yaml 패치 생성
cat <<EOF > increase_replicas.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-nginx
spec:
  replicas: 3
EOF

# 다른 패치로 set_memory.yaml 생성
cat <<EOF > set_memory.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-nginx
spec:
  template:
    spec:
      containers:
      - name: my-nginx
        resources:
        limits:
          memory: 512Mi
EOF

cat <<EOF >./kustomization.yaml
resources:
- deployment.yaml
patchesStrategicMerge:
- increase_replicas.yaml
- set_memory.yaml
EOF
````

```shell script
> kubectl kustomize /kube-kustomize/kustomize-patchesStrategicMerge
```

````yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-nginx
spec:
  replicas: 3
  selector:
    matchLabels:
      run: my-nginx
  template:
    metadata:
      labels:
        run: my-nginx
    spec:
      containers:
        - image: nginx
          name: my-nginx
          ports:
            - containerPort: 80
          resources:
            limits:
              memory: 512Mi
````

모든 리소스 또는 필드가 patchesStrategicMerge를 지원하는 것은 아니다. 임의의 리소스 내 임의의 필드의 수정을 지원하기 위해, 
Kustomize는 patchesJson6902를 통한 JSON 패치 적용을 제공한다. Json 패치의 정확한 리소스를 찾기 위해, 
해당 리소스의 group, version, kind, name이 kustomization.yaml 내에 명시될 필요가 있다. 
예를 들면, patchesJson6902를 통해 디플로이먼트의 리소스만 증가시킬 수 있다. 또한 patchesStrategicMerge, patchesJson6902를
같이 혼합해서 사용도 가능하다.

```yaml
#deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-nginx
spec:
  selector:
    matchLabels:
      run: my-nginx
  replicas: 2
  template:
    metadata:
      labels:
        run: my-nginx
    spec:
      containers:
        - name: my-nginx
          image: nginx
          ports:
            - containerPort: 80
          resources:
            limits:
              memory: 256Mi

#patch-replica.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-nginx
spec:
  replicas: 3

#patch-resource.yaml
- op: replace
  path: /spec/template/spec/containers/0/resources/limits/memory
  value: 512Mi

#kustomization.yaml
resources:
  - deployment.yaml

patchesStrategicMerge:
  - patch-replica.yaml

patchesJson6902:
  - target:
      kind: Deployment
      name: my-nginx
      group: apps
      version: v1
    path: patch-resource.yaml
``` 

```shell script
> kubectl kustomize /kube-kustomize/kustomize-patchesJson6902
```

````yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-nginx
spec:
  replicas: 3
  selector:
    matchLabels:
      run: my-nginx
  template:
    metadata:
      labels:
        run: my-nginx
    spec:
      containers:
        - image: nginx
          name: my-nginx
          ports:
            - containerPort: 80
          resources:
            limits:
              memory: 512Mi
````
patchesJson6902는 "replace"라는 오퍼레이션 말고, add, remove, move, copy, test라는 오퍼레이션도 존재한다.

## patch images
patch 파일을 생성하지 않고, 컨테이너의 이미지를 재정의 할 수 있다.

/kube-kustomize/kustomize-patch-images

````shell script
cat <<EOF > deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-nginx
spec:
  selector:
    matchLabels:
      run: my-nginx
  replicas: 2
  template:
    metadata:
      labels:
        run: my-nginx
    spec:
      containers:
      - name: my-nginx
        image: nginx
        ports:
        - containerPort: 80
EOF

cat <<EOF >./kustomization.yaml
resources:
- deployment.yaml
images:
- name: nginx
  newName: my.image.registry/nginx
  newTag: 1.4.0
EOF
````

```shell script
> kubectl kustomize /kube-kustomize/kustomize-patch-images
```

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-nginx
spec:
  replicas: 2
  selector:
    matchLabels:
      run: my-nginx
  template:
    metadata:
      labels:
        run: my-nginx
    spec:
      containers:
        - image: my.image.registry/nginx:1.4.0
          name: my-nginx
          ports:
            - containerPort: 80
```

## Base&Overlay
Kustomize는 base와 overlay의 개념을 가지고 있다. base는 kustomization.yaml과 함께 사용되는 디렉터리다. 
이는 사용자 정의와 관련된 리소스들의 집합을 포함한다. kustomization.yaml의 내부에 표시되는 base는 로컬 디렉터리이거나 원격 리포지터리의 디렉터리가 될 수 있다. 
overlay는 kustomization.yaml이 있는 디렉터리로 다른 kustomization 디렉터리들을 bases로 참조한다. 
base는 overlay에 대해서 알지 못하며 여러 overlay들에서 사용될 수 있다. 한 overlay는 다수의 base들을 가질 수 있고, 
base들에서 모든 리소스를 구성할 수 있으며, 이들의 위에 사용자 정의도 가질 수 있다.

```shell script
# base를 가지는 디렉터리 생성
mkdir base
# base/deployment.yaml 생성
cat <<EOF > base/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-nginx
spec:
  selector:
    matchLabels:
      run: my-nginx
  replicas: 2
  template:
    metadata:
      labels:
        run: my-nginx
    spec:
      containers:
      - name: my-nginx
        image: nginx
EOF

# base/service.yaml 파일 생성
cat <<EOF > base/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: my-nginx
  labels:
    run: my-nginx
spec:
  ports:
  - port: 80
    protocol: TCP
  selector:
    run: my-nginx
EOF
# base/kustomization.yaml 생성
cat <<EOF > base/kustomization.yaml
resources:
- deployment.yaml
- service.yaml
EOF
```

이 base는 다수의 overlay에서 사용될 수 있다. 다른 namePrefix 또는 다른 교차 편집 필드들을 서로 다른 overlay에 추가할 수 있다. 
다음 예제는 동일한 base를 사용하는 두 overlay들이다.

````shell script
> mkdir dev

cat <<EOF > dev/kustomization.yaml
#구버전 base 불러오는 방법
bases:
  - ../base
#resources:
#- ../base/kustomization.yaml

namespace: dev-my-nginx

patchesStrategicMerge:
  - patch-replica.yaml

patchesJson6902:
  - target:
      kind: Deployment
      name: my-nginx
      group: apps
      version: v1
    path: patch-resource.yaml

images:
  - name: nginx
    newName: my.image.registry/nginx
    newTag: 1.4.0
EOF

mkdir prod
cat <<EOF > prod/kustomization.yaml
#구버전 base 불러오는 방법
bases:
  - ../base
#resources:
#- ../base/kustomization.yaml

namespace: prod-my-nginx

patchesStrategicMerge:
  - patch-replica.yaml

patchesJson6902:
  - target:
      kind: Deployment
      name: my-nginx
      group: apps
      version: v1
    path: patch-resource.yaml

images:
  - name: nginx
    newName: my.image.registry/nginx
    newTag: 1.4.0
EOF
````

추가적으로 patch 파일들을 몇가지 작성하였다.

```shell script
> cd dev
> kubectl kustomize ./
> cd ../prod
> kubectl kustomize ./
```
