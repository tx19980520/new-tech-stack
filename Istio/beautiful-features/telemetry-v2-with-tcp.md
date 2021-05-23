# Telemetry v2 with TCP

Telemetry v2 中选择使用 ALPN 完成 TCP telemetry。

## ALPN Intro

本身对于一个 host 而言，port 能够极大的扩展 host 部署应用的能力，能够满足绝大多数的需求已区分逻辑。但是，如果服务使用的协议已更改且不向后兼容，则需要使用新的端口号。比如目前的大多数做法是 HTTPS 是在 443 端口，HTTP 协议是 80 端口，但是HTTPS 仅仅只是相对于 HTTP 加入了简单的 TLS Session。为了能够满足在一个端口就能够即接受 HTTP Request 也能够接受 HTTPS Request，ALPN 提出在 TLS ClientHello 和 ServerHello 阶段加入meta data 以完成相应逻辑进行区分。这里以 [golang-alpn-example](https://github.com/jefferai/golang-alpn-example) 为例，分别跑不通的 client 以体现不通协议在同一个端口下的表现。

```shell
foo client connected
handle foo started
h2 client sending request
handling /h2 for client
h2 client got: gotcha
foo client wrote f6710124-c4e3-1a4b-07eb-45e4a312c484

foo server got f6710124-c4e3-1a4b-07eb-45e4a312c484

foo server wrote f6710124-c4e3-1a4b-07eb-45e4a312c484

foo client read: f6710124-c4e3-1a4b-07eb-45e4a312c484
```

作者在代码中的基本逻辑为，如果是http2，则返回gotcha；如果是 **foo** 协议（base tcp），返回用户传入的uuid。这里的 **foo** 协议定义的相关内容是在 X509 证书中的，需要在请求时在 `TLSConfig` 中指定相关的内容从而选择相应的 handler 进行工作。

## ALPN in Istio

由于我不熟练 Envoy 的实现，这里选择 MOSN 的实现来理解该部分的内容，了解 Istio 如何使用 ALPN 完成 Telemetry v2。(问题来了，他们好像没实现，直接override了)。

好，然后还是回去看这个envoy，这个部分写得，就不太好看

首先在[istio/proxy](https://github.com/istio/proxy/) 里面全局搜索**istio-peer-exchange**，发现该文件就是一个很普遍的 filter，[L285](https://github.com/istio/proxy/blob/0efe658755ead2864b7b079bb8344e4af5ac4a9a/src/envoy/tcp/metadata_exchange/metadata_exchange.cc#L285) 发现这里有call wasm 的痕迹，然后开始在 [plugin](https://github.com/istio/proxy/blob/master/extensions/metadata_exchange/plugin.cc) 中看到`onConfigure ` 中调用`proxy_call_foreign_function` 和参数 `declare_property`，这个`declare_property` 函数需要在[envoy](https://github.com/envoyproxy/envoy/blob/main/source/extensions/common/wasm/foreign.cc)中寻找到，大致的情况就是在declare property。后来我们追到了`kServerConnectionsOpenCountView` 的设计，然后我懵了，这什么b东西

## REF

[alpn-intro](https://medium.com/geekculture/exploring-application-layer-protocol-negotiation-alpn-c47b5ec3b419)