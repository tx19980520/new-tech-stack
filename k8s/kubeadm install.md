# kubernetes 安装部署全过程

## 简介

kubernetes=1.13.5 kubedashboard=1.10.3  ubuntu 18.03

### 通用操作

1. 科学上网

   有部分镜像是pull不下来的，需要我们使用国内源（见下）进行操作

   ```bash
   docker pull xxxx/name:0.0
   docker tag xxxx/name:0.0 k8s.gcr.io/name:0.0
   ```

2. 基本环境的搭建

   ```bash
   sudo apt-get install \
       apt-transport-https \
       ca-certificates \
       curl \
       gnupg-agent \
       software-properties-common
   curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
   sudo add-apt-repository \
      "deb [arch=amd64] https://download.docker.com/linux/ubuntu \
      $(lsb_release -cs) \
      stable"
   sudo apt-get update
   sudo apt-get install docker-ce docker-ce-cli containerd.io
   ### 安装docker 完成
   sudo vim /etc/apt/sources.list
   添加下列源用于安装各项kubernetes的部件
   #deb [arch=amd64] https://mirrors.ustc.edu.cn/kubernetes/apt kubernetes-xenial main
   gpg --keyserver keyserver.ubuntu.com --recv-keys BA07F4FB
   gpg -a --export 6A030B21BA07F4FB | sudo apt-key add - #OK
   # 添加源对应的key
   sudo apt-get update
   sudo apt-get install -y kubelet kubeadm kubectl
   ```

## master

```shell
kubeadm init --pod-network-cidr=10.244.0.0/16 # 参数要求是flannel的要求
# 请记录下kubeadm join这行命令，用于后序方便的添加node
### 给予一般用户权限进行操作
mkdir -p $HOME/.kube 
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config
###
###
vim /etc/kubernetes/manifests/kube-apiserver.yaml
command: 
--enable-admission-plugins=NamespaceLifecycle,LimitRanger,ServiceAccount,DefaultStorageClass,DefaultTolerationSeconds,MutatingAdmissionWebhook,ValidatingAdmissionWebhook,ResourceQuota
### 记得把老的那个没running的kube-apiserver给删了
kubectl apply -f kube-flannel.yaml # 创建flannel
kubectl apply -f ssl-kube-dashboard.yaml # 创建dashboard
kubectl -n kube-system edit service kubernetes-dashboard # 将dashboard改为NodePort的 type
kubectl -n kube-system get service kubernetes-dashboard # 查看service的类型
### 上面这一步已经可以看到具体的页面了，但是没有任何的权限
kubectl create -f 328-admin-user.yaml
### 创建用户并给予角色
kubectl -n kube-system describe secret $(kubectl -n kube-system get secret | grep kubernetes-dashboard | awk '{print $1}')# 查看token
https://<master-ip>:<apiserver-port> # 访问dashboard
```

## node

安装各插件的操作与master相同。

```bash
### 首先将master节点上的./kube/config复制为该node节点上的/etc/kubernetes/admin.conf
mkdir -p $HOME/.kube 
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config
###
# sudo vim /etc/systemd/system/kubelet.service.d/10-kubeadm.conf 这个操作不靠谱，在高版本里面，因为这个参数的设置地点不在这里了，而且如果是修改其中某句话，将是一个没网的孤立节点
# https://kubernetes.io/docs/setup/cri/ 供大家参考
sudo swapoff -a # 关闭交换区

sudo kubeadm join xxx
# 如果出现错误，大致是token的问题 在master上面使用kubeadm token create --print-join-command生成新的替换
# 如果遇到“kubelet-config-1.14" is forbidden: User "system:bootstrap:gmt42b" cannot get resource "configmaps" in API group "" in the namespace "kube-system"请核对你master上的kubeadm、kubectl、kubelet的版本号和node节点上的一不一样，下载正确的版本
# 版本号非常的奇特是x.xx.x-00
# 似乎kubeadm可以直接
### 如果节点已经加入到了集群中，如果仍旧是notready的状态，那我们需要debug就可以看日志
journalctl -u kubelet
###
# 常见的错误是node服务器上面的网络设置出现问题，可以通过我们之前设置的dashboard查看错误


### Container runtime network not ready: NetworkReady=false reason:NetworkPluginNotReady message:docker

docker pull quay.io/coreos/flannel:v0.10.0-amd64 
mkdir -p /etc/cni/net.d/
cat <<EOF> /etc/cni/net.d/10-flannel.conf
{"name":"cbr0","type":"flannel","delegate": {"isDefaultGateway": true}}
EOF
mkdir /usr/share/oci-umount/oci-umount.d -p
mkdir /run/flannel/
cat <<EOF> /run/flannel/subnet.env
FLANNEL_NETWORK=172.100.0.0/16
FLANNEL_SUBNET=172.100.1.0/24
FLANNEL_MTU=1450
FLANNEL_IPMASQ=true
EOF
kubectl apply -f https://raw.githubusercontent.com/coreos/flannel/v0.9.1/Documentation/kube-flannel.yml
### flannel 没装没配置
### 如果报pull image相关的错误，详见前方的通用操作。
### 建议在join之前先直接把flannel装好就差不多了
```

### node使用nodeport简易部署一个nginx服务

相关附件后文显示

```bash
### 使用deployment部署一个类型的集群
kubectl apply -f nginx-deployment.yaml
###
### 创建service与pod进行对接，在此service的type为NodePort
kubectl apply -f nginx-service.yaml
###
### 如果需要删除某部署，则直接进行kubectl delete deployment xxx
```

```yaml
# nginx-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
  labels:
    app: nginx # 是与service联系的纽带
spec:
  replicas: 3
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
        image: nginx:1.7.9
        ports:
        - containerPort: 80 # container向外暴露的端口
```

```yaml
# nginx-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: nginx-service-nodeport
spec:
  selector:
    app: nginx # 联系上development中定制的pods
  ports:
   - name: http
     port: 8080 
     protocol: TCP
     targetPort: 80
  type: NodePort

```

#### nodePort port targetPort的不同点

1. nodePort是指在设定为NodePort类型下的service 在物理机上能够用`<IP>:<nodePort>`进行访问

2. port是指在clusterIP下对应的端口，即`<clusterIP>:<port>`能访问到

3. targetPort是指我们直接访问pod(endpoint)的时候的端口，要保证有效必须要能够和我们的pod上暴露的端口要一致（在此都是nginx的http默认端口80）

   ```bash
   endpoint:10.244.1.2:80,10.244.1.3:80,10.244.1.4:80
   curl 10.244.1.2:80
   ```

   结果为：

   ```html
   <!DOCTYPE html>
   <html>
   <head>
   <title>Welcome to nginx!</title>
   <style>
       body {
           width: 35em;
           margin: 0 auto;
           font-family: Tahoma, Verdana, Arial, sans-serif;
       }
   </style>
   </head>
   <body>
   <h1>Welcome to nginx!</h1>
   <p>If you see this page, the nginx web server is successfully installed and
   working. Further configuration is required.</p>
   
   <p>For online documentation and support please refer to
   <a href="http://nginx.org/">nginx.org</a>.<br/>
   Commercial support is available at
   <a href="http://nginx.com/">nginx.com</a>.</p>
   
   <p><em>Thank you for using nginx.</em></p>
   </body>
   </html>
   ```

   ## Helm 和 Tiller的安装

   这个部分是随着历史的演进而来的，我们如果有很大的工程的话，我们将会存在多个deployment、service文件，并且启动的先后顺序也是非常敏感的，我们手动去一个个启动可能会出现错误，管理将存在很大的问题。最终将Helm作为包管理器，则能够解决这问题，将一个体系的deployment、service打包，成为一个chart，最终可以将这些chart以某种顺序进行部署。

   ```bash
   sudo docker pull haojianxun/gcr.io.kubernetes-helm.tiller:v2.13.1
   sudo docker tag haojianxun/gcr.io.kubernetes-helm.tiller:v2.13.1 gcr.io/kubernetes-helm/tiller:v2.13.1 # 每个节点上都需要pull该images
   # helm 请自行前往官网直接下载相关二进制编译好的文件， 直接cp到bin里面去就能用了https://helm.sh/docs/install/
   ### install tiller
   wget https://raw.githubusercontent.com/istio/istio/release-1.1/install/kubernetes/helm/helm-service-account.yaml
   kubectl apply -f helm-service-account.yaml # 为tiller 创建账户
   helm init --service-account tiller --skip-refresh # 通过helm的init创建tiller
   ###
   ### 但是我直接装没发现怎么装istio-injector，找了个博客去生成模板去create istio.yaml
   helm template install/kubernetes/helm/istio --name istio --namespace istio-system --set sidecarInjectorWebhook.enabled=true --set ingress.service.type=NodePort --set gateways.istio-ingressgateway.type=NodePort --set gateways.istio-egressgateway.type=NodePort --set tracing.enabled=true --set servicegraph.enabled=true --set prometheus.enabled=true --set tracing.jaeger.enabled=true --set grafana.enabled=true > istio.yaml
   
   kubectl create namespace istio-system
   kubectl apply -f istio.yaml# 我们自己的文件去istio的安装目录下去找
   ### 
   ### 
   ```

   ## Istio

   ![k8s+Istio](./k8s+Istio.png)

   我们的kubernetes实现了分布式部署的相关工作，但对于在线管理上kubernetes能做的并不多，我们需要实现熔断限流、动态路由，因而我们需要在kubernetes的体系下融入Istio。

   ## 初步试探相关bookinfo的相关结果

   对官网的bookinfo进行了一些初步试探，得到的初步的结论如下：

   1. gateway是整个流量的入口。
   2. 会使用某个Istio的Virtual Service，设置其对应的gateway
   3. 在Virtual Service中设置相应路由，路由直接我们定义的实际Service挂钩（host与Service的name）
   4. Service回到kubernetes的Service和Deploy的层级，如果不对外暴露，则直接使用ClusterIP即可

   ### 初步试探熔断机制的相应结果

   熔断机制的实现是针对某项服务而言的，当访问该服务达到一定的条件之后，我们将对其进行相应的阻拦[ref](https://istio.io/zh/docs/reference/config/istio.networking.v1alpha3/#trafficpolicy)

   ```yaml
   apiVersion: networking.istio.io/v1alpha3
   kind: DestinationRule
   metadata:
     name: httpbin
   spec:
     host: httpbin # 某个服务的名称
     trafficPolicy: # 设置上游池的连接设置
       connectionPool:
         tcp:
           maxConnections: 1
         http:
           http1MaxPendingRequests: 1
           maxRequestsPerConnection: 1
       outlierDetection:
         consecutiveErrors: 1
         interval: 1s
         baseEjectionTime: 3m
         maxEjectionPercent: 100
   ```

   ### Istio的可视化工作

   

   ## 现在仍存在的问题

   1. 我每一个node物理机上都需要手动对kubeadm进行配置并且因为网络问题主动pull相关docker的images吗？