# canal

本文档主要介绍canal的使用，canal主要用途是基于 MySQL 数据库增量日志解析，提供增量数据订阅和消费。在实践中，我们主要用于进行业务cache的刷新。详细的情报见[官网](https://github.com/alibaba/canal)

## 实验性部署

首先部署一个mysql服务，并开启logbin

```mysql
[mysqld]
log-bin=mysql-bin
binlog-format=ROW
server_id=1
```

之后启动即可使用mysql，进入mysql中创建一个slave账号：

```mysql
CREATE USER canal IDENTIFIED BY 'canal';  
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%';
-- GRANT ALL PRIVILEGES ON *.* TO 'canal'@'%' ;
FLUSH PRIVILEGES;
```

之后通过官方的run.sh脚本启动docker

```bash
sh run.sh -e canal.auto.scan=false \
		  -e canal.destinations=test \
		  -e canal.instance.master.address=127.0.0.1:3306  \
		  -e canal.instance.dbUsername=canal  \
		  -e canal.instance.dbPassword=canal  \
		  -e canal.instance.connectionCharset=UTF-8 \
		  -e canal.instance.tsdb.enable=true \
		  -e canal.instance.gtidon=false
```

注意我们这里是启动了一个叫test的destination，后文要一致。

之后我们准备在服务器上使用相应python sdk进行canal client的demo级测试

dependency如下：

```
pip3 install protobuf
pip3 install google-cloud-translate
pip3 install canal-python
```

之后修改官网上脚本，将其destination改为test（他们大概是一个组的几个同事，然后名字没统一）。

运行脚本结果如下：

```json
{'event_type': 1, 'data': {'name': 'a'}, 'table': 'test', 'db': 'test'}
{'event_type': 1, 'data': {'name': 'b'}, 'table': 'test', 'db': 'test'}

```

## kubernetes 集群部署


