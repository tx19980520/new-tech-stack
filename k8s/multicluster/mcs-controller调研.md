# MCS controller 调研

## Multi Cluster Problems

MCS controller 的设计方案提出，意在解决 multicluster 的相关问题，关于用户为何需要使用 multi cluster，可见[文档](https://docs.google.com/document/d/1G1lfIukib7Fy_LpLUoHZPhcZ5T-w52D2YT9W1465dtY/edit#)，阅读下文请先详细的阅读完文档之后参与讨论。

我个人的认为是这里主要强调隔离(isolation)，这个隔离的主要来源是从人的角度而言的，例如文章中提到的组织隔离，另一方面比较偏向从机器的角度来思考，主要考虑部署和数据的可用性和安全性，以及相应的，单个集群过大可能将导致一些组件性能达到了瓶颈(scheduler)。

但是这种隔离，对于现阶段的用户而言，有松动的情况(开始希望白名单一些功能，这里最迫切的需要就是网络层面的打通)

在思考 multicluster 的问题上，主要是要考虑的问题，在文中也有比较详细的罗列，这里针对几个比较突出的方面进行探讨。

**network**：网络的连通性直接影响了如何实现 multicluster 之间的通信，换言之，大部分的用户原始需求就是希望能够打通不同集群的网络通信，不仅是需要打通，而且是希望对于 k8s 现有功能而言是无感修改。那集群之间的原有通信模式将有较大的影响。例如原文中提到的在同一个VPC下的 multicluster，“强化”实现DNS的功能即可，但是对于得通过固定 ingress 和 egress 进行访问的 multicluster，则需要考虑如何 dynamic 的配置网关等内容，以及对方如何识别流量，这里的实现实质上比较类似于利用 Istio [多control plane实现](https://istio.io/latest/docs/setup/install/multicluster/gateways/)。

**DNS**：DNS 部分比较重要的就是，我们需要区别 other cluster 和 local cluster 的的信息，对应的比较重要的功能就是容灾，在本期群内的压力过大或不可用的情况下，将考虑将流量导向其他集群的服务。

**multiclusterAggregation**: 之前的两点都是关于如何从现有方案过渡到multicluster的方法，文档中也详细的谈到，从 multicluster 的角度如何进行运维操作，这里就讲到的 multicluster Deployment ,ConfigMap and Secret，是自顶向下设计的。

## MCS controller 接口设计

详见[文档](https://docs.google.com/document/d/1hFtp8X7dzVS-JbfA5xuPvI_DNISctEbJSorFnY-nz6o/edit#)

整体而言 ACK 不仅仅需要知道自己集群内部的信息，从另外的角度而言，ACK 需要同步到其他集群的内容，MCS controller 目前的设计主要是解决“Different Services Each Deployed to Separate Cluster ” 和 "Single Service Deployed to Multiple Clusters"，没有限制实现的方式，你可以是纯中心的，也可以是分布式的。中心式的就比较套娃，让 k8s 类似于层级式的下发配置，但是实现相对简单，分布式的讲实现一些协议，从实现而言不简单，性能也不一定会好。

mcs controller 当前定义了相应的 CRD，ServiceExport 是 local cluster 主动对外暴露的服务， ServiceImport 将主要记录引入的 Service 的相关信息。ServiceImport 是具有状态的，Initialized 和 Exported 分别表示被 validate 和 消息已经被 sync 到了其他的集群，这里没有提出任何有关 sync 的相关细节，只是定义了终态，ServiceExport 被要求与 Service 同名。

### Exported Service Behavior

在 ServiceExport 创建完成之后，需要提供一个VIP 作为 supercluster 的 VIP，在侧边栏中有讨论关于 Headless Service 的适配问题，Headless Service 是否也需要有一个VIP，或者Headless Service 在 multicluster 层面上也是 headless 的。

DNS 方面，大家更倾向于更换 suffix， 以方便平滑迁移。

ServiceExport 在创建后将会在其他集群中创建对应的 EndpointSlice ，这个 EndpointSlice 将不被local cluster 管理，整体归 MCS Controller 管理。

Endpoint TTL 主要应用于更新 endpoints， 保证 endpoints 的健康，而这个 endpoints 是否健康，是 provider cluster 来保证的，MCS Controller 来维护一个租约，如果租约到期没有续签，endpoints将被定期删除。

这里引入了一个新的概念 ImportedService，这主要是 Service 的一个扩展，因为 ImportedService 会关联多个集群和多个 EndpointSlice，因此额外提出了相应的概念，使用annotation 将 ImportedService 与 EndPointSlice 做绑定。

