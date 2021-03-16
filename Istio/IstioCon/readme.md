# IstioCon notes

all resources are [here](https://events.istio.io/istiocon-2021/sessions/)

## Performance tuning and best practices in a Knative based, large-scale serverless platform with Istio

Istio as an Ingress Gateway

Security with Service Mesh enabled

就mesh的一个能力而言，每隔5s，顺序的创建Knative Service，每一个Route ready 的时间小于 30s

Ingress gateway 的内存增长与 route 成正比，针对到 Knative 而言，一个Knative Service 对应750k 内存，这个bug在 1.6.0 中修复，主要原因是 envoy 没有释放对应的内存。

另一个内存问题是当创建 Knative Service 存在 1k 个时，内存的使用达到了 5-10g，后续对pilot的代码进行了优化，我们进一步查看了[该优化](https://github.com/istio/istio/pull/25532)，简单来讲就是key用得太大了，后来做了个hash变小了而已。

后续也提到了**Pilot Pushes shows long latencies**，首先是发现了一个Bug，这个Bug主要是ack的race[导致](https://github.com/istio/istio/issues/23029#issuecomment-642952307)。

后续进一步测试发现在 550 个 Knative Service 的情形下，仍旧超出了相当量的预想，具体而言IBM调节了 debounce 时间，缓解了该现象，但是仍旧在850 个 Knative Service 出现了瓶颈。

backpressure机制：**在数据流从上游生产者向下游消费者传输的过程中，上游生产速度大于下游消费速度，导致下游的 Buffer 溢出，这种现象就叫做 Backpressure 出现**

具体到pilot里面，其实就是减少并发，变成了获取了上次ack之后才会发下一次的（及时性的牺牲），我寻思这都快串行了。

final solution 还是 `delta XDS`

第二步讲到的是`Enable Istio mesh on Knative`，遇到的几个问题如下：

- init container 消耗了5s左右的时间（这个部分主要是Knative的业务逻辑）

  解决方案主要是考虑启动 CNI plugin（后续去了解下）

- sidecar本身获取的信息超量，利用 `Sidecar`配置，进行namespace的分割

- activator 需要时间检测到容器的IP，这需要跑满整个Istio DS的过程(https://github.com/istio/istio/issues/23494)。

- sidecar 本身需要冷启动时间。

## Extending service mesh capabilities using a streamlined way based on WASM and ORAS

总结了wasm for proxy的优缺点：

 Pros 

○ 敏捷性：过滤器可以动态加载到正在运行的Envoy进程中，而无需停止或重新编译。 ○ 可维护性：不必更改Envoy自身基础代码库即可扩展其功能。 

○ 多样性：可以将流行的编程语言（例如C/C++和Rust）编译为WASM，因此开发人员可 以选择实现过滤器的编程语言。 

○ 可靠性和隔离性：过滤器会被部署到VM沙箱中，因此与Envoy进程本身是隔离的；即使 当WASM Filter出现问题导致崩溃时，它也不会影响Envoy进程。 

○ 安全性：过滤器通过预定义API与Envoy代理进行通信，因此它们可以访问并只能修改有 限数量的连接或请求属性。 

● Cons 

○ 性能约为C++编写的原生静态编译的Filter的70％; 

○ 由于需要启动一个或多个WASM虚拟机，因此会消耗一定的内存使用量; 

○ The WebAssembly ecosystem is still young;

然后出来OCI Registry As Storage，主要是存储WASM binary



## Know your peers

这个topic比较适合拿去准备面试，因为关于Istio比较低层的一些通信安全的内容讲的比较透彻。

SPIFFE ID: 一个URI 类型的UUID，主要用于区分不同host上的process。

SPIFFE Verifiable Identity Document(SVID) SVID有两种实现，本质是证明自己的SPIFFE ID 是可信的，一种是 JWT，一种是 X509，前者比较偏向 application layer，一般放在 http header 里面，后者在 TLS/SSL，而且 Istio 本来就要做 mTLS，所以选择X509实际会更简单，也不会将该实现暴露给用户。具体而言，SPIFFE ID 会存在证书的SAN中，当然SAN不只有SPIFFE信息，还有一些metadata。

后文讲述了关于整个证书颁发的过程，本质还是 pod 内的 envoy 会在一开始向 istiod 申请证书，之后这里的细节是如何证明这个 pod 的请求与本身该 pod 的的身份一致，依靠的是kubernetes token。

## Accelerate istio-cni with ebpf

Istio-CNI：去除init-container，通过daemon的容器部署，来更改iptables，降低容器启动时间，并且因为不存在修改网络的init-container，权限方面的风险也降低了。

然后类似 cilium 选择了 SOCK_OPS SK_SKB 的程序进行 ebpf 编程。

