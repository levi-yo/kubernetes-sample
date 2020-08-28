# Kubernetes monitoring
## 1. Prometheus
클러스터의 팟들의 메트릭정보를 수집하는 역할.

### 1.1 create namespace
```shell script
> kubectl create namespace monitoring
```

### 1.2 create cluster role
prometheus가 kubernetes api에 접근할 수 있는 권한을 부여해주기 위해 ClusterRole을 설정하고 바인딩해준다.
```yaml
apiVersion: rbac.authorization.k8s.io/v1beta1
kind: ClusterRole
metadata:
  name: prometheus
  namespace: monitoring
rules:
  - apiGroups: [""]
    resources:
      - nodes
      - nodes/proxy
      - services
      - endpoints
      - pods
    verbs: ["get", "list", "watch"]
  - apiGroups:
      - extensions
    resources:
      - ingresses
    verbs: ["get", "list", "watch"]
  - nonResourceURLs: ["/metrics"]
    verbs: ["get"]
---
apiVersion: rbac.authorization.k8s.io/v1beta1
kind: ClusterRoleBinding
metadata:
  name: prometheus
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: prometheus
subjects:
  - kind: ServiceAccount
    name: default
    namespace: monitoring
```

### 1.3 create prometheus config map
prometheus의 환경 설정 파일 정의
- prometheus.rules : 수집한 지표에 대한 알람조건을 지정하여 특정 조건이 되면 AlertManager로 알람을 보낼 수 있다.
- prometheus.yml : 수집할 지표의 종류와 수집 주기등을 기입한다.
````yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-server-conf
  labels:
    name: prometheus-server-conf
  namespace: monitoring
data:
  prometheus.rules: |-
    groups:
    - name: container memory alert
      rules:
      - alert: container memory usage rate is very high( > 55%)
        expr: sum(container_memory_working_set_bytes{pod!="", name=""})/ sum (kube_node_status_allocatable_memory_bytes) * 100 > 55
        for: 1m
        labels:
          severity: fatal
        annotations:
          summary: High Memory Usage on
          identifier: ""
          description: " Memory Usage: "
    - name: container CPU alert
      rules:
      - alert: container CPU usage rate is very high( > 10%)
        expr: sum (rate (container_cpu_usage_seconds_total{pod!=""}[1m])) / sum (machine_cpu_cores) * 100 > 10
        for: 1m
        labels:
          severity: fatal
        annotations:
          summary: High Cpu Usage
  prometheus.yml: |-
    global:
      scrape_interval: 5s
      evaluation_interval: 5s
    rule_files:
      - /etc/prometheus/prometheus.rules
    alerting:
      alertmanagers:
      - scheme: http
        static_configs:
        - targets:
          - "alertmanager.monitoring.svc:9093"

    scrape_configs:
      - job_name: 'kubernetes-apiservers'

        kubernetes_sd_configs:
        - role: endpoints
        scheme: https

        tls_config:
          ca_file: /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
        bearer_token_file: /var/run/secrets/kubernetes.io/serviceaccount/token

        relabel_configs:
        - source_labels: [__meta_kubernetes_namespace, __meta_kubernetes_service_name, __meta_kubernetes_endpoint_port_name]
          action: keep
          regex: default;kubernetes;https

      - job_name: 'kubernetes-nodes'

        scheme: https

        tls_config:
          ca_file: /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
        bearer_token_file: /var/run/secrets/kubernetes.io/serviceaccount/token

        kubernetes_sd_configs:
        - role: node

        relabel_configs:
        - action: labelmap
          regex: __meta_kubernetes_node_label_(.+)
        - target_label: __address__
          replacement: kubernetes.default.svc:443
        - source_labels: [__meta_kubernetes_node_name]
          regex: (.+)
          target_label: __metrics_path__
          replacement: /api/v1/nodes/${1}/proxy/metrics


      - job_name: 'kubernetes-pods'

        kubernetes_sd_configs:
        - role: pod

        relabel_configs:
        - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
          action: keep
          regex: true
        - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
          action: replace
          target_label: __metrics_path__
          regex: (.+)
        - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
          action: replace
          regex: ([^:]+)(?::\d+)?;(\d+)
          replacement: $1:$2
          target_label: __address__
        - action: labelmap
          regex: __meta_kubernetes_pod_label_(.+)
        - source_labels: [__meta_kubernetes_namespace]
          action: replace
          target_label: kubernetes_namespace
        - source_labels: [__meta_kubernetes_pod_name]
          action: replace
          target_label: kubernetes_pod_name

      - job_name: 'kube-state-metrics'
        static_configs:
          - targets: ['kube-state-metrics.kube-system.svc.cluster.local:8080']

      - job_name: 'kubernetes-cadvisor'

        scheme: https

        tls_config:
          ca_file: /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
        bearer_token_file: /var/run/secrets/kubernetes.io/serviceaccount/token

        kubernetes_sd_configs:
        - role: node

        relabel_configs:
        - action: labelmap
          regex: __meta_kubernetes_node_label_(.+)
        - target_label: __address__
          replacement: kubernetes.default.svc:443
        - source_labels: [__meta_kubernetes_node_name]
          regex: (.+)
          target_label: __metrics_path__
          replacement: /api/v1/nodes/${1}/proxy/metrics/cadvisor

      - job_name: 'kubernetes-service-endpoints'

        kubernetes_sd_configs:
        - role: endpoints

        relabel_configs:
        - source_labels: [__meta_kubernetes_service_annotation_prometheus_io_scrape]
          action: keep
          regex: true
        - source_labels: [__meta_kubernetes_service_annotation_prometheus_io_scheme]
          action: replace
          target_label: __scheme__
          regex: (https?)
        - source_labels: [__meta_kubernetes_service_annotation_prometheus_io_path]
          action: replace
          target_label: __metrics_path__
          regex: (.+)
        - source_labels: [__address__, __meta_kubernetes_service_annotation_prometheus_io_port]
          action: replace
          target_label: __address__
          regex: ([^:]+)(?::\d+)?;(\d+)
          replacement: $1:$2
        - action: labelmap
          regex: __meta_kubernetes_service_label_(.+)
        - source_labels: [__meta_kubernetes_namespace]
          action: replace
          target_label: kubernetes_namespace
        - source_labels: [__meta_kubernetes_service_name]
          action: replace
          target_label: kubernetes_name
      - job_name: 'spring'
        metrics_path: '/actuator/prometheus'
        static_configs:
          - targets: ['springboot-web.springboot-web-service.svc.cluster.local:80']
````

마지막에 우리가 수집할 애플리케이션을 job으로 넣어준것도 유의해서 보자.

### 1.4 prometheus deployment
````yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: prometheus-deployment
  namespace: monitoring
spec:
  replicas: 1
  selector:
    matchLabels:
      app: prometheus-server
  template:
    metadata:
      labels:
        app: prometheus-server
    spec:
      containers:
        - name: prometheus
          image: prom/prometheus:latest
          args:
            - "--config.file=/etc/prometheus/prometheus.yml"
            - "--storage.tsdb.path=/prometheus/"
          ports:
            - containerPort: 9090
          volumeMounts:
            - name: prometheus-config-volume
              mountPath: /etc/prometheus/
            - name: prometheus-storage-volume
              mountPath: /prometheus/
      volumes:
        - name: prometheus-config-volume
          configMap:
            defaultMode: 420
            name: prometheus-server-conf

        - name: prometheus-storage-volume
          emptyDir: {}
````

### 1.5 node exporter
````yaml
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: node-exporter
  namespace: monitoring
  labels:
    k8s-app: node-exporter
spec:
  selector:
    matchLabels:
      k8s-app: node-exporter
  template:
    metadata:
      labels:
        k8s-app: node-exporter
    spec:
      containers:
      - image: prom/node-exporter
        name: node-exporter
        ports:
        - containerPort: 9100
          protocol: TCP
          name: http
---
apiVersion: v1
kind: Service
metadata:
  labels:
    k8s-app: node-exporter
  name: node-exporter
  namespace: kube-system
spec:
  ports:
  - name: http
    port: 9100
    nodePort: 31672
    protocol: TCP
  type: NodePort
  selector:
    k8s-app: node-exporter
````

### 1.6 prometheus service
````yaml
apiVersion: v1
kind: Service
metadata:
  name: prometheus-service
  namespace: monitoring
  annotations:
    prometheus.io/scrape: 'true'
    prometheus.io/port:   '9090'
spec:
  selector:
    app: prometheus-server
  type: NodePort
  ports:
    - port: 5050
      targetPort: 9090
      nodePort: 30003
````

### 1.7 배포
```shell script
> kubectl apply -f ./prometheus/prometheus-cluster-role.yaml
> kubectl apply -f ./prometheus/prometheus-config-map.yaml
> kubectl apply -f ./prometheus/prometheus-deployment.yaml
> kubectl apply -f ./prometheus/prometheus-service.yaml
> kubectl get pod -n monitoring
  NAME                                     READY   STATUS              RESTARTS   AGE
  prometheus-deployment-58db98dbb5-br8zb   0/1     ContainerCreating   0          9s
> kubectl get configmap -n monitoring
  NAME                     DATA   AGE
  prometheus-server-conf   2      43s
```

## 2. Grafana
prometheus에서 수집한 지표를 시각화해주는 웹

### 2.1 Grafana deployment
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: grafana
  namespace: monitoring
spec:
  replicas: 1
  selector:
    matchLabels:
      app: grafana
  template:
    metadata:
      name: grafana
      labels:
        app: grafana
    spec:
      containers:
      - name: grafana
        image: grafana/grafana:latest
        ports:
        - name: grafana
          containerPort: 3000
        env:
        - name: GF_SERVER_HTTP_PORT
          value: "3000"
        - name: GF_AUTH_BASIC_ENABLED
          value: "false"
        - name: GF_AUTH_ANONYMOUS_ENABLED
          value: "true"
        - name: GF_AUTH_ANONYMOUS_ORG_ROLE
          value: Admin
        - name: GF_SERVER_ROOT_URL
          value: /
---
apiVersion: v1
kind: Service
metadata:
  name: grafana
  namespace: monitoring
  annotations:
      prometheus.io/scrape: 'true'
      prometheus.io/port:   '3000'
spec:
  selector:
    app: grafana
  type: NodePort
  ports:
    - port: 3000
      targetPort: 3000
      nodePort: 30004
```

### 2.2 deploy
```shell script
> kubectl apply -f ./grafana/grafana.yaml
> kubectl get pod -n monitoring      
  NAME                                     READY   STATUS    RESTARTS   AGE
  grafana-7b844d9446-hgp95                 1/1     Running   0          11s
  prometheus-deployment-58db98dbb5-br8zb   1/1     Running   0          4m22s
> kubectl get svc -n monitoring
  NAME                 TYPE       CLUSTER-IP       EXTERNAL-IP   PORT(S)          AGE
  grafana              NodePort   10.106.99.194    <none>        3000:30004/TCP   73m
  prometheus-service   NodePort   10.102.238.145   <none>        5050:30003/TCP   67m
```

## 3. spring application
```yaml
//monitoring
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation "io.micrometer:micrometer-registry-prometheus"
```
build.gradle에 모니터링에 필요한 라이브러리를 dependency한다.

````yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus, health, metrics, prometheus
````
application.yml에 메트릭 정보를 수집할 수 있는 endpoint를 열어준다.

## 4. Dashboard 구성
## 4.1 Datasource
<img src="https://t1.daumcdn.net/cfile/tistory/991985445B8FC81E08" width="600" height="300">
<br>
<br>
prometheus를 datasource로 추가한다.
<br>
<br>
<img src="https://user-images.githubusercontent.com/15958325/78098151-26ab8200-7419-11ea-8860-1f0988ab7fac.png" width="600" height="300">
<br>
<br>
prometheus 접속 정보를 넣어준다.
<br>
<br>
<img src="https://user-images.githubusercontent.com/15958325/78098229-5fe3f200-7419-11ea-99be-f9c49d8e4cbc.png" width="600" height="300">
<br>
<br>
클러스터 ip:port를 직접 넣어도되고, "prometheus-service.monitoring.svc.cluster.local:30003"처럼 클러스터 도메인으로 넣어줘도 된다.
<br>
<br>
<img src="https://user-images.githubusercontent.com/15958325/78098290-99b4f880-7419-11ea-9514-88293010390d.png" width="600" height="300">
<br>
<br>
위 이미지가 나오면 데이터소스 추가 성공!

## 4.2 Dashboard sample
[Grafana DashBoard web](https://grafana.com/grafana/dashboards)

위 주소에서 그라파나 대시보드 샘플들을 내려받을 수 있다.

<img src="https://user-images.githubusercontent.com/15958325/78098569-6d4dac00-741a-11ea-8379-c3e260a7615f.png" width="600" height="300">
<br>
<br>
Copy id를 눌러 id를 복사한다.
<br>
<br>
<img src="https://user-images.githubusercontent.com/15958325/78098629-9c641d80-741a-11ea-8cf6-dcc60869a614.png" width="600" height="300">
<img src="https://user-images.githubusercontent.com/15958325/78098633-9ec67780-741a-11ea-8c22-ef75ddccdaba.png" width="600" height="300">
<br>
<br>
아래와 같이 대시보드에 매트릭정보가 보인다.
<br>
<br>
<img src="https://user-images.githubusercontent.com/15958325/78098636-a128d180-741a-11ea-9f1c-af6c6ca7fd8d.png" width="600" height="300">
<br>
<br>
Spring JVM 메트릭정보로 위 그라파나 홈페이지에서 찾으면 샘플로 많이 있으니, 한번 적용해보자.
