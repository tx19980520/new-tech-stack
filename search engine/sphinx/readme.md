# sphinx

sphinx是一款开源的搜索引擎，高级语言接口完备，可以用于数据库的全局搜索。

## 安装

首先前往sphinx的官网下在相对应的包

其次是填充config里面的内容主要有如下意义

```C
source{
    # 数据库的相关信息
    # 数据库的查询方式
    # 输出的附加内容
}

index {
	# source指定
	# 类型指定，可以是实时的更新，也可以是相对静态的
	# charset指定
	# 对于特殊文本的特殊操作
}
searchd{
    #search 服务设置在哪个端口
    # log放在那里，一些类似于nginx服务器的设置内容
}
```

如果你想看一些非常具体的接口，可以访问[这里](https://www.cnblogs.com/yjf512/p/3598332.html)

之后显示建立索引

```
indexer -c sphinx.conf goods# goods为index名
```

之后建立搜索服务

```shell
searchd -c sphinx.conf
```

注意你的服务器防火墙或者是各服务器厂商的安全组一定要对外开放

你会在share/doc/api中看到各种高级语言的api可供使用