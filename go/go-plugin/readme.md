# go plugin

go plugin 模块可以认为是 reflect 模块的延续。相较于 java 的 reflect，go 欠缺的能力相对是较多的，动态加载就是其中一个，plugin 模块目前支持加载新的 go 代码进入到 process 中可直接调用相应的 function。

## 关于数据结构

对于在 plugin 中定义的数据结构，golang 中存在的问题是你在 main 中使用的类型转换实际应该是 plugin 定义的，但是我们无法用任何方式指认我们需要的是 plugin 中的类型而非什么其他类型。即便是你在 main 和 plugin 中都定义数据结构，golang 也会认为这是两个数据结构，这里比较好的方案是 main 和 plugin 都 import 相同的 package 这样 type assertion 的问题就可以被解决。

## 关于使用 C/C++ 编译出的so文件

目前而言似乎使用 plugin 的方式使用 C/C++ 编译出的 so 文件并不成功，会出现`fatal error: runtime: no plugin module data`的错误，但是仍旧可以考虑使用 cgo 进行调用

## go plugin 的源码解析

go plugin 本质上还是使用 C 代码进行`dlopen` ,`dlsym`系统调用。然后在填充进 go 的 runtime 内容之中(moduledata)，相关函数的指针将会在 `dlopen` 的时候 load 到 runtime 的数据结构中，Lookup 仅会在数据结构中进行查询。

plugin 的地位本质上和main函数的地位是一样的，当然 main 函数一定程度上更高，因为是作为整个程序的入口，因此他也一定是 active modules 中的第一个。同样的， modules 在 open 之后作为最后一个 module 会调用 `plugin_lastmoduleinit` 函数，初始化相关的内容，里面比较重要的一项就是`doInitTask()`，即调用我们在 package 中声明的 `init` 函数。

