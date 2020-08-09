# Kubernetes sample(쿠버네티스 manifast 샘플)

<div>
  <img width="1000" src="https://subicura.com/assets/article_images/2019-05-19-kubernetes-basic-1/kubernetes-logo.png">
</div>

## ./kube-resource/.. -> kubernetes manifast sample files in directory

## apply labels to node(쿠버테티스 노드에 레이블 적용하기)
```
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
```
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
  **nodeSelector:
    disk_type: ssd**
```
