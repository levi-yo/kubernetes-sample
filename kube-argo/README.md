# Argo CD
## Argo CD install
```shell script
> kubectl create namespace argocd
> kubectl apply -n argocd -f ./install.yaml
```

최초 비밀번호는 아래 명령으로 알수 있다.

````shell script
> kubectl get pods -n argocd -l app.kubernetes.io/name=argocd-server -o name | cut -d'/' -f 2
argocd-server-6cbf8f7677-ssskw
````

id/password는 "admin/argocd-server-6cbf8f7677-ssskw" 이 된다. 패스워드를 변경해 사용해보자.
만약 argo CLI 다운로드를 받지 않았다면, 설치하자.

````shell script
> brew tap argoproj/tap
> brew install argoproj/tap/argocd
````

패스워드를 변경해보자.

````shell script
> argocd login <ARGOCD_SERVER>
> argocd login argocd.local.com:32226
  WARNING: server certificate had error: x509: certificate is valid for localhost, argocd-server, argocd-server.argocd, argocd-server.argocd.svc, argocd-server.argocd.svc.cluster.local, not argocd.local.com. Proceed insecurely (y/n)? y
  Username: admin
  Password: argocd-server-6cbf8f7677-ssskw
  'admin' logged in successfully
  Context 'argocd.local.com:32226' updated
> argocd account update-password
  *** Enter current password: argocd-server-6cbf8f7677-ssskw
  *** Enter new password: 변경할 패스워드
  *** Confirm new password: 변경할 패스워드
  Password updated
  Context 'argocd.local.com:32226' updated
````

이제 앱을 배포할 클러스터를 등록해준다(선택사항)

````shell script
> argocd cluster add
ERRO[0000] Choose a context name from:                  
CURRENT  NAME                          CLUSTER                       SERVER
*        docker-desktop                docker-desktop                https://kubernetes.docker.internal:6443
         docker-for-desktop            docker-desktop                https://kubernetes.docker.internal:6443

> argocd cluster add docker-desktop
INFO[0000] ServiceAccount "argocd-manager" created in namespace "kube-system" 
INFO[0000] ClusterRole "argocd-manager-role" created    
INFO[0000] ClusterRoleBinding "argocd-manager-role-binding" created 
Cluster 'https://kubernetes.docker.internal:6443' added
````
