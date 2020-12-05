# 使用非 XDS 完成动态路由

​       相关需求需要从中心下放路由规则到 MOSN ，但是消息的上游不是 遵守 XDS 的，目前希望达到的效果为 MSON 从上游获取到最新的路由信息，并且改写 Router 和 Host。目前可能考虑的方案是类比于 XDSClient 实现一个 UpstreamClient，用于完成我对于 MOSN 配置的修改。

## 观察 XDSClient 的行为

由于需要模仿 XDSClient 的行为，对 XDSClient 进行解析。

XDSClient 里面的大部分内容都是关于如何请求，如何解析 XDSResponse，这一部分就小项目而言完全可以简化。重点在于如何转换写入到 MOSN 的协议中去。

这一步主要是依靠 `MngAdapter` 和  `RouterManager`  和 `ListenerAdapter` 完成，大量的可用接口可以修改相关内容，进一步的我们根据 XDS 各个部分，到底如何影响了 MOSN 的config 内容。

```
LDS => AddOrUpdateListener/DeleteListener
RDS => AddOrUpdateRouters
CDS => TriggerClusterAddOrUpdate/TriggerClusterDel
EDS => TriggerClusterHostUpdate
```

XDSClient 启动是由其 `Start` 函数完成的，其调用在 `mosn.Start`中被调用，因为 MOSN 可以只依赖静态配置就可以启动，因此 XDSClient 如果没有相关配置，Start 就会不成功，即default 使用了静态配置。

## 需求分析

就我目前的需求而言，listeners, routers 的大部分内容使用静态资源应该就可以了，routers.virtual_hosts.routers 需要动态修改，cluster_manager 需要修改。

