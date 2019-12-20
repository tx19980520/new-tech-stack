# JMeter 分布式压力测试

为了突破单机测试的性能局限性，我们将启用集群进行更进一步的压力测试，用以提供更加全面和更深层次的测试结果。

## JMeter各远端服务器的配置

```bash
sudo apt-get install default-jre
wget http://mirrors.tuna.tsinghua.edu.cn/apache//jmeter/binaries/apache-jmeter-5.1.1.tgz
tar -zxvf apache-jmeter-5.1.1.tgz
```

配置文件中主要关注的部分如下

```properties
# Set this if you don't want to use SSL for RMI, 一般我们测试情况就不开SSL
server.rmi.ssl.disable=true 
# 有关于你远端调用的IP or DNS
remote_hosts=node2,node3,node4
# 远端服务具体的port，最好指定，不然会是随机的端口，防火墙不好设置
server.rmi.localport=3030
# Set this if you don't want to use SSL for RMI 作为测试关闭RMI的SSL
server.rmi.ssl.disable=true
# 有的博文里面说改成false，但是这样我的jmeter-server就启动不了，原因不明。
server.rmi.create=true
```

之后先启动各slave节点的jmeter-server，等待我们的controller调度

```bash
./jmeter-server
Using local port: 3030
Created remote object: UnicastServerRef2 [liveRef: [endpoint:[10.0.0.24:3030](local),objID:[-300bb601:16d8b3a7c1c:-7fff, 12160871764474459]]]
```

之后我们的在controller node(node1)运行相应的jmx（注意测试汇总最好是直接指定好汇总报告和聚合报告，再通过jtl生成相应的report html，这样的report html才是通过所有数据得出的，不然会存在各自的报告存在同一文件夹下的not empty的问题）

启动测试`./jmeter -n -t path/to/jmx/test.jmx -R node4,node3,node2 `

在各节点上看到的log如下：

```bash
Starting the test on host node2 @ Wed Oct 02 07:03:53 UTC 2019 (1569999833859)
Finished the test on host node2 @ Wed Oct 02 07:07:19 UTC 2019 (1570000039577)
Starting the test on host node2 @ Wed Oct 02 07:42:27 UTC 2019 (1570002147889)
Finished the test on host node2 @ Wed Oct 02 07:42:38 UTC 2019 (1570002158085)
```

## 补充

[这里](https://hub.helm.sh/charts/stable/distributed-jmeter)是JMeter在helm上准备的一个chart，有兴趣可以直接使用这套进行配置，但是可能相应的网络结构需要自己更加多的注意，并且我们的测试可能只是在多台服务器上，而不限于是在集群中，其实用性还有待考证。

