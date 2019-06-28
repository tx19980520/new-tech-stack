# TiDB的使用场景

TiDB是一种New SQL，底层使用TiKV进行键值对存储，支持MySQL协议和

![tidb-architecture](./images/tidb-architecture.png)

TiDB的具体三大模块——TiDB Server PD Server TiKV Server的相关描述可以看[这里](https://pingcap.com/docs-cn/v3.0/architecture/)。本文只是重点强调一下各个部分。

TiDB Server是面向用户的，主要是在处理SQL，因此该服务是stateless的，当我们的请求增长时，我们可以单纯的提升TiDB Server来提升整体处理能力，在外层还可以添加LB。

TiKV是开源分布式事务性key-value的数据库，注意到TiKV是提供transactional APIs，是遵循raft协议。可能会有疑问为什么我们使用的MySQL的scheme，但是我们背后的存储引擎是一个KV类型的，这其中的转换其实是靠TiDB Server来实现的。

PD是遵从的Raft协议，主要负责对TiKV集群进行调度和负载均衡，会记录相应数据的分布等信息，PD是集成了etcd来持久化其中的信息。PD分配任务是在TiKV发送心跳，在返回时带上任务进行执行的，而不是主动去发送信息给TiKV。

### TiDB的缺点

1. MySQL的兼容性上不支持触发器，存储过程，自定义函数和外键，我认为这几个中最重要的是外键，因为外键是非常常见的一个情况
2. TiDB是使用快照隔离来实现一致性，只能达到MySQL的可重复读，不能达到串行化的地步。（虽然大部分情况也不需要）

### TiKV的对Index的实现方式

TiKV对于Primary Index实质上是直接通过Key即可，但是我们会对Secondary Index有一些疑惑，对于Secondary Index，TiKV的实现方式是在key中与Primary Key进行融合，形成一个全新的key，按一定规则进行拼接，例如`t10_i1_10_1`则为10号table上的1号index，index的对应值为10，是第一个，实际表格如下：

```
1, "TiDB", "SQL Layer", 10
2, "TiKV", "KV Engine", 20
3, "PD", "Manager", 30
```

对这种实现方式的效率有所堪忧不知道能不能达到相应的性能要求，从TiDB的天梯rank来看，其在relational DBMS上排名在60+，详见[这里](https://db-engines.com/en/ranking)。

## 单独使用TiKV作为key value 分布式数据库

TiKV是开源分布式事务性key-value的数据库，注意到TiKV是提供transactional APIs，遵循raft协议，在分布式中使用版本管理实现乐观锁。

我们能单独用TiKV做什么呢，我们发现使用Redis的时候Redis是无法执行事务的，为了能够执行事务，我们可以让TiKV在上层遵守Redis协议。

我发现Redis在性能上还是比TiKV好，我哭了。

