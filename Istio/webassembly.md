# WebAssembly in service mesh 调研

WebAssembly in service mesh 的调研主要分为两个部分，WebAssembly 的工作机制和WebAssembly 运行在 service mesh 数据面中。

## WebAssembly 工作机制

WebAssmbly(后简称wasm) 本质上是一种字节码，运行在相应的沙盒中，其运行的速度接近 native speed，可以做到热插拔的运行方式。

wasm 在性能方面上的表现有如下数据可供参考：

Istio 中如果使用 wasm 代替原有 Mixer，则将在load wasm 时 CPU 使用大幅度提升，在持续使用的过程中，cpu使用量将提升 30% ~ 50%， CPU 使用量将翻倍。

## WebAssembly 运行在 service mesh 数据面中

使用 wasm 作为一种可扩展方式在 service mesh sidecar 中执行，有如下的收益：

敏捷：借助相应的控制面，能够让数据面快速的获取到下发的 binaray 并进行热加载。

可靠与隔离：extension 将被运行在沙河之中，即便运行故障也不会影响 sidecar 的正常运行。

安全：sidecar 和 wasm runtime 的交互是通过 API 完成的，因此扩展只能访问和修改连接或请求的有限数量的属性，可以清除掉某些敏感信息。

多语言支持：多种语言都支持转码为wasm，因此可以接入更多的开发者进行开发。

Istio 相关的工作记录在这个[issue](https://github.com/envoyproxy/envoy/issues/4272)。

其中支持 wasm 的虚拟机如下：

- [WAVM](https://github.com/WAVM/WAVM)
- [V8](https://v8.dev/)
- Null Sandbox (use the API, compile directly into Envoy)

