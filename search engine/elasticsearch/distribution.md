# ElasticSearch多机分布式基本原理

本文重点讲述ElasticSearch在分布式情况下的相关设计，基本组件的相互工作原理。

我们在集群中使用helm启动ElasticSearch集群，其中包含的Pod主要分为三个部分——master、client、data。我们曾今使用其他方式启动ElasticSearch时，一个Pod是可以被配备多个角色的。

## ElasticSearch角色分配

### master

master的主要功能是维护元数据，管理各节点状态，不负责数据写入和查询，配置上内存可以相对小一些，但是机器要稳定，我们的在集群中是使用PV使之能够“无状态”。

master主要负责的是Index的管理，观察各节点的健康情况，决定相对应分片被分配到什么节点上 

#### master选举

我们的初始化的ElasticSearch集群中存在三个master-eligible，则master-eligible就需要进行选举，多master-eligible是为了提升可靠性。master进行选举的条件如下：

1. 该master-eligible节点当前不是master
2. 该master-eligible节点通过ZenDiScovery模块的ping操作询问其他已知的集群节点，没有任何节点连接到master
3. 包括本节点在内，当前已有minimum_master_nodes个节点没有连接到master

当如果master-eligible发现有现成的master时，其也会直接认可现在的master。

选举的主要依据是通过排序选出第一个MasterCandidate，排序是按照clusterStateVersion、Id的主次顺序降序。注意到这里的排序工作是在发起选举的节点上进行的。

选举的结果有如下几种处理，这里假定NodeA选定了NodeB作为master

1. 如果NodeB本来就是master，则这次选举只是为了让NodeA加入到集群
2. 如果NodeB在竞选master，那就需要NodeA进行等待最终的结果，NodeA选举NodeB只是一张选票
3. 如果NodeB不是master也没有竞选，那NodeB将会拒绝，NodeA会开始新一轮的选举

如果NodeA选择自己当master，则他会向其他Node征集选票，如果选票过半，他就会给所有节点发布自己是master的信息。

我们这里是需要承认在此种情况下，出现网络的不稳定的情况，确实会出现的相应的脑裂的现象，这个脑裂的情况的根本来源就在于有可能出现一个节点会给投多次票。主要在于如果NodeB给NodeA投票之后，如果NodeA迟迟不成为master，超时之后NodeB可能会先NodeX又投出一票，这时候NodeX和NodeA都得到了NodeB的一票，他们都有可能成为master。

ElasticSearch的选择是在于使用错误检测的方式解决脑裂的情况

**MasterFaultDetection** 主要是Master检测到某个Node连接不上，就会删除相应的Node，各个模块和节点收到相应的信号之后，会采取相应的措施。

**NodesFaultDetection** 主要是Node节点发现Master节点连接不上了，则会清空还没commit的state，发起rejoin。

如果发现master所掌控的节点已经不满足多数派条件，则该master应该主动退位，避免可能出现或已经出现的脑裂现象。

对于发布新的消息使用two phase commit，如果没有成功，abort之后会出现rejoin的操作。

如果定期检查时发现有存在另一个master的情况，则进行cluster_state的version比较，version较小的启动rejoin。

### data

data主要负责数据的写入与查询，压力相对较大，需要大内存，也需要使用PV，最好设置成为DaemonSet。

这里我们主要考虑到两个方面，第一个方面是容错的，第二个方面是性能。我们也是使用的常见的方式进行处理，比如使用shard和replica的方式进行这两个方面的提升。

ElasticSearch中存在primary shard和replica shard，replica shard是primary shard的副本，负责容错，以及承担读请求负载。

primary shard的默认数量是5，replica默认是1,primary shard不能喝自己的replica shard放在同一个节点上，否则节点宕机，primary shard和副本都消失，容错机制将失效。

当我们添加或删除节点时，数据都会发生rebalance，rebalance的决策是在master，rebalance的最小单元是shard，不是在于单条数据。

这里需要注意，我们的primary shard的数量不能发生变化，这个问题主要是因为数据路由算法他使用的不是一致性hash，如果某个primary shard处于失效的状态，则会在replica中选出一个新的primary，然后会重新创建一个新的replica shard，注意这个举动是master进行指派的。

有关ES的博客中提及到，相应的如果出现同时的primary shard和 replica shard都宕机的情况，确实是无法避免会丢失相应的数据，但是如果你使用了PV，则受灾将会减少不少。

### client

主要负责任务分发和结果汇聚，主要是面向用户进行使用的，例如我们的kibana与ES进行交互的就是与client进行交互，client外面包一层Service进行统一对外进行工作。

> **client nodes** are smart load balancers that take part in some of the processing steps. 

这些“客户端”节点仍然是集群的一部分，它们可以将操作精确地重定向到保存相关数据的节点，而不必查询所有节点。但是，它们不存储数据，也不执行集群管理操作。另一个好处是，对于基于分散/收集的操作(例如搜索)，由于客户机节点将启动分散过程，它们将执行实际的收集处理。这使数据节点无需处理HTTP请求(解析)、使网络过载或执行收集处理，就可以完成繁重的索引和搜索任务，即如果我们直接向data的node发送的相应的请求，在data node上，我们会进行相应收集工作，实质上是对其本职工作——读写数据有一个的打扰，因此需要相应的既知道存储地点，又负责收集相应的数据。

在集群内部，其相互之间进行信息交互，是可以选择关闭http的，内部使用TCP Transport，消息传输是异步通信，在通信过程中，不会有等待的线程。