# Ordering Guarantee

我们需要考虑到相关的需要ordering的使用场景，比如说我们会考虑到数据库binlog的传输，我们对于数据库的操作需要进行进行重排序，则我们需要考虑到相关情况下的ordering guarantee

## Kafka

Kafka能够保证的是分配到partition上是能够保证相应的顺序性的，但是我们的消息再写入时，会被分配到不同的partition，这个层面上，仍旧是不能够满足我们的业务需求。即使Kafka能够保证一个consumer收到的信息能够“内聚”，但是我们consumer会分配到其下的thread pool，thread pool还要保证执行的顺序，也是需要相对的考虑（这个无可避免）。问题的根本在于数据之间的关系，比如我们的log，他就需要按照一个commit来进行操作（commit作为一个上下文），则会存在consumer没法直接消费相关的message，他需要将信息汇总之后再进行以一个commit为基础的操作。

## RabbitMQ

RabbitMQ存储在queue中的内容是有序的，但是因为多个consumer都会subscribe到同一个queue上，则consumers会肢解掉相应的commit。



## Solution

可以在consumer之后设置一个redis，然后当redis中相应的commit收集完后，作为一个task交给一个worker进行工作，但是多出来的就是一个redis的额外操作，相应的latency也会增加，并且MQ的最大功效也难以得到体现。另一方面，也可以选择在数据源头，就做好相应数据收集，一条message就是一个相应的操作。

以下是ref cannal的相关描述，详情见[这里](https://github.com/alibaba/canal/wiki/Canal-Kafka-RocketMQ-QuickStart)，这其实就是我们所说的在数据源头先进行处理。因为binlog中记录的都是已经commit的内容，则不需要考虑是不是一个commit中的内容，更加看重的是“表中的一行”。

## mq顺序性问题

binlog本身是有序的，写入到mq之后如何保障顺序是很多人会比较关注，在issue里也有非常多人咨询了类似的问题，这里做一个统一的解答

1. canal目前选择支持的kafka/rocketmq，本质上都是基于本地文件的方式来支持了分区级的顺序消息的能力，也就是binlog写入mq是可以有一些顺序性保障，这个取决于用户的一些参数选择
2. canal支持MQ数据的几种路由方式：单topic单分区，单topic多分区、多topic单分区、多topic多分区

- canal.mq.dynamicTopic，主要控制是否是单topic还是多topic，针对命中条件的表可以发到表名对应的topic、库名对应的topic、默认topic name
- canal.mq.partitionsNum、canal.mq.partitionHash，主要控制是否多分区以及分区的partition的路由计算，针对命中条件的可以做到按表级做分区、pk级做分区等

1. canal的消费顺序性，主要取决于描述2中的路由选择，举例说明：

- 单topic单分区，可以严格保证和binlog一样的顺序性，缺点就是性能比较慢，单分区的性能写入大概在2~3k的TPS
- 多topic单分区，可以保证表级别的顺序性，一张表或者一个库的所有数据都写入到一个topic的单分区中，可以保证有序性，针对热点表也存在写入分区的性能问题
- 单topic、多topic的多分区，如果用户选择的是指定table的方式，那和第二部分一样，保障的是表级别的顺序性(存在热点表写入分区的性能问题)，如果用户选择的是指定pk hash的方式，那只能保障的是一个pk的多次binlog顺序性 ** pk hash的方式需要业务权衡，这里性能会最好，但如果业务上有pk变更或者对多pk数据有顺序性依赖，就会产生业务处理错乱的情况. 如果有pk变更，pk变更前和变更后的值会落在不同的分区里，业务消费就会有先后顺序的问题，需要注意

