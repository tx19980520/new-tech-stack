# Configuration

APISIX 的动态配置不是类似于 Nginx Ingress Controller 采用混合式的方式，他直接额外的实现了一套 `/admin` 的接口直接通过跟这个接口交互以完成，这个接口最后会去调用 `events.post(reload_event, get_method(), ngx_time())`，这个地方最后调用的 `reload_plugin`

总而言之就是用 Lua 实现了一个 [admin API](https://github.com/apache/apisix/blob/baf843403461883c1334e63d15a6bb3622c31940/docs/zh/latest/admin-api.md)，如果使用Stand-alone mode，则会从yaml文件中定时 reload。

我粗读[该篇博客](https://juejin.cn/post/6933768239008874510)，发现整个 apisix 的路由实际上跟nginx 本身没什么关系，通过这样的方式，能够完全的 bypass 掉 config reload（APISIX 的方案里面，只会在启动的时候创建一个静态的 static nginx.config，之后不会再修改，入口全是 apisix 的 lua 代码，后面流量出去是 nginx core）。