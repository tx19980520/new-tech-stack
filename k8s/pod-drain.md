# Kubernetes Pod Drain

该博客是在以为群友提出相应的问题之后，基于基础的解答，进行了相应深入的探讨之后成文。

## what drain

drain的对象为一个Node，行为是将Node上所有的服务迁移到其他的Node上，并将自己设置为不可调度。

## why drain

从大家反应的情况和自身使用的情况，我们总结出如下几种情况：

- 该节点网络会出现不稳定的现象，为了保证稳定工作和排查对该节点进行drain

  这个情况是本人集群中出现的情况，由于OpenStack下的虚拟机网络出现了不稳定的情况，导致一些服务无法工作，为了稳定起见，决定先将其上的服务迁移到其他的节点上。

- 对该节点的操作系统内核进行升级

  主要是一些安全方面的需要，务必将Node的操作系统升级。

- 该节点的资源使用过于饱和，导致服务的运行速度降低，通过drain将资源重新进行编排

  是drain比较重要的一个功能，scheduler进行调度时，因时间上的局限性，没办法做到全局最优，一段时间之后会导致资源浪费和性能的下降，从而需要在一段时间后进行相应的”重编排“的操作。

## how drain

注意到drain操作和delete操作实质上是不同的。drain强调的是迁移，并且是对Node进行操作，这个过程中需要考虑到Disruption Budgets的相关策略；delete操作（暂时指对Pod的delete），他仅仅删除资源，如果部署的是一个Pod则不存在能够自动重新部署，如果是Deployment下的Pod，是因为删除之后replica没有达到相应的需求，进而重新创建的，是Deployment的相应操作因delete而触发。

这里要重点讲一讲Disruption Budget，Disruption Budget主要考虑的是服务的可用性，用户肯定是希望在进行“重编排”的情况下仍旧能够对外提供服务，因此希望迁移是一个相对平滑的过程。因此需要设置Disruption Budget。

```yaml
apiVersion: policy/v1beta1
kind: PodDisruptionBudget
metadata:
 name: zk-pdb
spec:
 minAvailable: 2
 selector:
   matchLabels:
     app: zookeeper
```

因此在我们进行drain Node的时候，会存在我们同时进行drain的操作，我们希望他们是并行化的，因为并行化的drain更加能够得到“更加全局”最优解，但是并行化带来的困难会更对，从而导致一系列的问题，例如处理的时候的consistency的问题，因为我们是对node的一个drain，drain的这个指令不是针对集群的指令，从而很难从集群层面上得到更好的编排。Pod Disruption Budget在此其实是一个约束，将完全并行化的操作向串行化方向有所倾斜，从数据流方面影响了控制流。从而在并行化drain的过程中因为可用节点数量的保证从而出现block的情况。



