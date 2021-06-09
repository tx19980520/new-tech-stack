# Configuration

Kong 使用额外进程 [Kubernetes Ingress Controller](https://docs.konghq.com/kubernetes-ingress-controller/1.3.x/concepts/design/) 来管理配置

![architecture](https://docs.konghq.com/assets/images/docs/kubernetes-ingress-controller/high-level-design.png)

Kubernetes Ingress Controller（以下简称 KIC）是 Golang 写的，这里主要是考虑到 client-go 对于这个部分的支持是非常强大的，如果你直接交给 Kong 内部，例如 Lua 或者 C/C++ 与 APIServer 进行交互，非常困难，没有什么好的类库可以使用。另一方面，如果 KIC 没有使用数据库的需求，则扩展性良好，使用适当的方法的可用性也较高。

[re-achitecture for KIC](https://github.com/Kong/kubernetes-ingress-controller/blob/5ff10fb79ea1ccd7d1d624440065d00f8f8b9029/railgun/keps/0003-kic-kubebuilder-re-architecture.md) 是近期的一部分工作，该工作的主要目的是不直接使用client-go，而是使用 kubebuilder 完成工作，文档里讲了几个 story 来说明这个 motivation。

这里因为没有看里面的代码，对于其实现方面，可能有一些比较大的问题。可以直接关注一下 [Nginx-Ingress-Controller 的实现](https://kubernetes.github.io/ingress-nginx/how-it-works/)（以下简称 NIC） ，需要额外强调的是，NIC 里面特意强调了关于 avoid reload 的问题，例如回避直接通过 reload 更新 endpoints 的问题，但是对于证书和路由规则等仍旧使用 reload 完成。并且提到了 [lua_dynamic_upstream_plugin ](https://github.com/openresty/lua-resty-core/blob/master/lib/ngx/balancer.md)作为其背后的实现，来完成 load balance 的工作，这里提到了更新endpoints 是通过发送 http request 来完成，然后 lua 模块直接将 endpoints 信息存储在 shared memory 里面，感觉这非常有意思，相比于 apisix，这里还是比较依赖 nginx 原生机制的，这主要是定位不一样的问题，NIC 毕竟是说基于 Nginx 做扩展，他不是要修改 Nginx。

