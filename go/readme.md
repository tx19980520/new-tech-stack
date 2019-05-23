# Go 入门

## GOROOT & GOPATH

GOROOT是你安装Go语言的安装目录

GOPATH是你自己的work space

目前我认为GOPATH和GOROOT可以分离，但是实际情况是他们不能分离

需要进一步探究

另外再goland里面可以重新设置环境变量，这样解耦会比较方便，在不同project里面的项目都比较独立。

GOPATH里面的目录结构如下：

```bash
/home/halfrost/gorepo
├── bin
├── pkg
└── src
```

在bin里面主要是放置可执行文件，和g++编译出a.exe是相似的，在pkg中是一些package，在你的代码中不包含main.go的情况下会产出一些package(类似dll？)的文件，在src中是你项目的源码。