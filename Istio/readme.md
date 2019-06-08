# Istio相关kind

在本文中我们将对Istio补充相关kind进行一些学习

### DestinationRule

主要的功能是注册子集**`subset`**，设置一下host名称（当然也可以不写，默认按照kubernetes的DNS命名方法来）

当然你可以对该子集下或者该规则下的loadBalancer进行规则的设置。

```yaml
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: bookinfo-ratings
spec:
  host: ratings.prod.svc.cluster.local
  trafficPolicy: # 对指定端口生效
    portLevelSettings:
    - port:
        number: 80
      loadBalancer:
        simple: LEAST_CONN
    - port:
        number: 9080
      loadBalancer:
        simple: ROUND_ROBIN
  subsets:
  - name: testversion
    labels:
      version: v3
    trafficPolicy:
      loadBalancer:
        simple: ROUND_ROBIN
```

你在此设置好的rule可以在VirtualService里面得到印证

基本的结构形式为：**`VirtualService | DestinationRule | Service | Deployment(StatefulSet)`**

这个结构里面是可以没有Destination的，你可以直接将Service对接到VirtualService上去。

### ServiceEntry

主要是在Istio内部的服务注册表中加入额外的条目，从而让网格中自动发现的服务能够访问，可以添加网格外的服务。

里面涉及到一个SNI值的问题，SNI（Server Name Indication）是为了解决一个服务器使用多个域名和证书的SSL/TLS扩展，保证你在建立https之前，让服务器知道你需要获取的证书是哪一个，这样服务器才能将正确的证书发给你，进行正常的交流。

Istio默认的`global.outboundTrafficPolicy.mode`的值为ALLOW_ANY，即可以代理未知服务，简单的说就是你在POD内是可以通过sidecar把流量发出去，当然这样你无法控制你的向外的流量，无法写入Mixer的日志中，因此我们要把mode的值改为REGISTRY_ONLY，则你需要使用到**ServiceEntry**

在ServiceEntry中注册了我们需要访问的外部服务，就可以查看Mixer的日志和proxy的日志

```yaml
apiVersion: networking.istio.io/v1alpha3
kind: ServiceEntry
metadata:
  name: httpbin-ext
spec:
  hosts:
  - httpbin.org
  ports:
  - number: 80
    name: http
    protocol: HTTP
  resolution: DNS
  location: MESH_EXTERNAL
```

```bash
$ kubectl logs podname -c istio-proxy | tail
$ kubectl -n istio-system logs -l istio-mixer-type=telemetry -c mixer | grep 'httpbin.org'
 
```

请注意如果外部是HTTPS的服务，由于存在加密Istio无法查看，只能看到SNI以及发送和接受的字节数，如果要想监视相关的服务，需要应用程序配置Istio以执行TLS。

## MTLS(mutual TLS)

相互传输层安全性协议是指在服务（器）到服务（器）的连接依赖使用MTLS进行身份验证，发出消息的服务器和接收消息的服务器交换相互信任的CA证书。证书可向一台服务器证明另一台服务器的身份。简单的go实现可以看[这里](https://venilnoronha.io/a-step-by-step-guide-to-mtls-in-go)

