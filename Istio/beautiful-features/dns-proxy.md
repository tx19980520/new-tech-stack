# Istio 1.8 DNS proxy

相当的文章讲述了 DNS proxy 的这个需求是怎么来的，主要的内容都是描述的 VM 层面的需求以及降低原生 Kubernetes 集群 DNS 服务请求的相关内容。

原始需求如此，本片文章主要围绕 DNS proxy 在 Istio 的体系中的实现。

面向需求而言，为了完成 DNS proxy 这个需求，需要在如下几个部分添加相关的内容：

1. 数据面实现，需要数据面”劫持“ DNS 的解析，使得 DNS 解析的部分对于用户的容器是透明的，这一步需要劫持相关的流量。
2. 控制面实现，控制面需要能够和上游进行交互，并且能够推送相关 DNS 的更新到数据面，即就原有的 XDS 协议进行一点扩展。

## 实现分析

熟悉 pilot 代码的同学很容易发现其中的 package 多出了 dns 的文件夹，里面的文件量不大，在此建议从 test 入手，了解其功能以及相应的代码结构。

本次解析使用的代码分支为 `istio-1f30efc6c140333b0601ae752d5d1e0ca92dbd5b`

从 test 的角度而言，我们看到更新注入数据使用的是 `UpdateLookupTable`，从而我们在代码中直接搜索该函数（因为反正 DNS 该怎么实现都很普通，主要还是看数据来源）。代码里面可以发现具体和猜想的差不多，是通过 XDS 下发的，具体的 `TypeUrl` 是 `v3.NameTableType`，这里职能体现 agent 方面的实现，我们需要更多的寻找控制面方面的代码。

通过搜索，最终准备从`Generator` 开始入手进行解析，发现针对此初始化了 `NdsGenerator`，至此其实比较明朗，因为目前的 Istio 关于 XDS 进行扩展都比较模板化，相应的几个结构体，常量进行初始化。在数据面和控制面完成相关内容即可。

具体可以看一下`NdsGenerator`的实现主要是依靠 `ConfigGenerator.BuildNameTable`，寻找到具体的实现，主要是根据该 sidecar 应该知晓的 Service 中进行寻找，这里着重强调了一下关于 `Headless Service` 的相关内容。

另外的其实就数据的内容而言，我个人认为原有的 XDS 下放的信息已经完全能够“拼凑”出一个 DNS 的代理了，这里单独实现 NDS 更多的还是代码层面上的设计。

关于劫持相关的流量，可以在`tools/istio-iptables/pkg/cmd/run.go`里面找到 redirectDNS 这个变量的相关内容，最终可以看到这里设置了 iptables ，让发到 port 53 的所有流量全部劫持到 15053，但对于 udp 的部分而言这里注释有所提及，但是 udp 的部分没有提及。此处还需要进一步的进行研究，当然也有可能是这一个iptables的规则就足矣。

## ref

[ndots](https://tizeen.github.io/2019/02/27/DNS%E4%B8%AD%E7%9A%84ndots/)

[宋净超翻译的DNS proxy 功能简介](https://zhuanlan.zhihu.com/p/309679795)

