# mysql 主从数据库的探索

mysql的主从备份的实现机理是使用二进制log文件，master和所欲偶的slave都需要一个唯一的server-id

## 本机docker的实践

注：我们使用的是mysql5.6不适用更高的版本8的原因是要设置“另类”的auth，在功能实现层面上来说认为不必要，所以我们并没有关注该点，如果有需要，请查看[这里](https://dev.mysql.com/doc/refman/8.0/en/replication-howto-repuser.html)。

### bootstrap.cnf

```
[mysqld]
log-bin=mysql-bin
server-id = x

# 设置保证一致性
innodb_flush_log_at_trx_commit = 1
sync_binlog = 1
# slave节点需要的，需要的原因是因为default的是按照{hostname}-relay-bin来命名的，所以一旦hostname修改了就会无效。
relay-log=xxx
relay-log-index=xxxx
```



```bash
docker run -itd -p 3306:3306 --name master -v /usr/mysql/master/conf:/etc/mysql/conf.d -v /usr/mysql/master/data:/var/lib/mysql -e MYSQL_ROOT_PASlsSWORD=12345 --link master mysql:5.6 #master服务启动
docker run -itd -p 3307:3306 --name slave -v /usr/mysql/slave/conf:/etc/mysql/conf.d -v /usr/mysql/slave/data:/var/lib/mysql -e MYSQL_ROOT_PASSWORD=12345 --link master mysql:5.6
# slave服务启动，注意这里link了master

### master start ###
# 建议单独创建账号来进行主从的备份操作。
mysql -u root -p
 GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY '12345' WITH GRANT OPTION;\
 FLUSH PRIVILEGES;
 FLUSH TABLES WITH READ LOCK;
 # 上面的语句是把数据flush之后把所有表锁住，以免在做后序操作的时候有数据插入进来
 UNLOCK TABLES; # 这一句是在所有的slave已经配置完成后再开启，主要是把表去掉，相当于开始服务。
 show master status;
 +------------------+----------+--------------+------------------+-------------------+
| File             | Position | Binlog_Do_DB | Binlog_Ignore_DB | Executed_Gtid_Set |
+------------------+----------+--------------+------------------+-------------------+
| mysql-bin.000006 |      412 |              |                  |                   |
+------------------+----------+--------------+------------------+-------------------+
# 记住这个filename，slave根据这个来同步
### master end ###

### slave start ###
mysql -u root -p
change master to 
master_host='master',master_user='root',master_password='12345',master_log_file='mysql-bin.000006';
start slave;
show slave status\G
### slave end ###
```

之后在master数据库里面进行操作会发现能同步到slave数据库中。

可能出现的错误：

1. server-id或uuid出现问题，例如两者一样，这主要是因为copy的缘故，uuid出问题请到data文件夹里面删除auto.cnf然后重启数据库就好，server-id可以写在my.cnf中，你可以修改该文件然后重启数据库。
2. 'aio write' returned OS error 122.出现这个问题的主要原因是我是windows系统，我的docker容器是跑在虚拟机中，我又希望在windows里面写配置文件，所以我把目录挂载进虚拟机，docker的目录又挂载到挂载目录，出现了文件系统不支持这样的问题。
3. master_host到底填什么，这个问题我之前一直以为是填192.168.99.100（我虚拟机在我电脑上的ip）或者是127.0.0.1或者直接0.0.0.0，这些都不对，应该是填master和slave两个容器link起来之后建立的网络中的ip地址，所以我选择了使用--link，并在slave中使用了master域名。

![hosts](./images/hosts.png)

### log_slave_updates

根据文字的意图非常简单，就是是否在log中记录slave的更新。

可能出现问题的是以下这种情况：

A与B为主主复制，C为A的slave，则我们会发现C的更新只来源于A。

这就是因为默认的log_slave_updates为false，则A作为B的slave角色，则A确实会接受B的数据更新，但是A也同时是B的master，或者说至少身份不单纯，则A此时是不会讲B的更新放入自己的log中去的。

按照官方文档的意思，默认不设置**`--log-slave-updates`**的理由是为了in case you cause one of the slaves to become the new master. If `Slave 1` has [`--log-slave-updates`](https://dev.mysql.com/doc/refman/5.7/en/replication-options-slave.html#option_mysqld_log-slave-updates) enabled, it writes any updates that it receives from `Master` in its own binary log. This means that, when `Slave 2` changes from `Master` to `Slave 1` as its master, it may receive updates from `Slave 1` that it has already received from `Master`.

### 主从切换

我们的master节点出现故障时，需要我们将一个slave节点切换为新的master节点，并且将其余的slave作为新master的slave。

![redundancy-before](./images/redundancy-before.png)

0. 确保每个slave节点都没有使用--log-slave-updates

1. 通过 `show processlist`查看

   ![no-relay](./images/no-relay.png)

2. 对被选中的slave执行`stop slave`，`reset master`

3. 对其余节点`stop slave`并且`change master to`

![redundancy-after](./images/redundancy-after.png)

### mysql on kubernetes with master-slave structure

我们按照[官方教程](https://kubernetes.io/docs/tasks/run-application/run-replicated-stateful-application/#deploy-mysql)我们进行配置

1. configmap

   configmap的主要用途是为了在后续部署时能够复用一部分的配置（主要是在slave上），并且以后修改可以集中进行修改，我们的write操作是走master节点的，read是走slave节点的。

   ```yaml
   apiVersion: v1
   kind: ConfigMap
   metadata:
     name: mysql
     labels:
       app: mysql
   data:
     master.cnf: |
       # Apply this config only on the master.
       [mysqld]
       log-bin
     slave.cnf: |
       # Apply this config only on slaves.
       [mysqld]
       super-read-only
   ```

2. 创建service，主要是注意mysql的master service需要的是一个stateful的，所以他不需要clusterIP，我们这个mysql service就只是来暴露write服务的。

   ```yaml
   application/mysql/mysql-services.yaml 
   
   # Headless service for stable DNS entries of StatefulSet members.
   apiVersion: v1
   kind: Service
   metadata:
     name: mysql
     labels:
       app: mysql
   spec:
     ports:
     - name: mysql
       port: 3306
     clusterIP: None
     selector:
       app: mysql
   ---
   # Client service for connecting to any MySQL instance for reads.
   # For writes, you must instead connect to the master: mysql-0.mysql.
   apiVersion: v1
   kind: Service
   metadata:
     name: mysql-read
     labels:
       app: mysql
   spec:
     ports:
     - name: mysql
       port: 3306
     selector:
       app: mysql
   
   
   ```

3. StatefulSet的创建

   ```yaml
   apiVersion: apps/v1
   kind: StatefulSet
   metadata:
     name: mysql
   spec:
     selector:
       matchLabels:
         app: mysql
     serviceName: mysql
     replicas: 3
     template:
       metadata:
         labels:
           app: mysql
       spec:
         initContainers: # 这个里面的都是为了init一个container做的准备工作
         - name: init-mysql # 主要是准备好server-id，并且选择好正确的配置文件
           image: mysql:5.7
           command:
           - bash
           - "-c"
           - |
             set -ex
             # Generate mysql server-id from pod ordinal index.
             [[ `hostname` =~ -([0-9]+)$ ]] || exit 1
             ordinal=${BASH_REMATCH[1]}
             echo [mysqld] > /mnt/conf.d/server-id.cnf
             # Add an offset to avoid reserved server-id=0 value.
             echo server-id=$((100 + $ordinal)) >> /mnt/conf.d/server-id.cnf
             # Copy appropriate conf.d files from config-map to emptyDir.
             if [[ $ordinal -eq 0 ]]; then
               cp /mnt/config-map/master.cnf /mnt/conf.d/
             else
               cp /mnt/config-map/slave.cnf /mnt/conf.d/
             fi
           volumeMounts:
           - name: conf
             mountPath: /mnt/conf.d
           - name: config-map
             mountPath: /mnt/config-map
         - name: clone-mysql #主要是考虑到可能我们会迁移一些原始数据
           image: gcr.io/google-samples/xtrabackup:1.0
           command:
           - bash
           - "-c"
           - |
             set -ex
             # Skip the clone if data already exists.
             [[ -d /var/lib/mysql/mysql ]] && exit 0
             # Skip the clone on master (ordinal index 0).
             [[ `hostname` =~ -([0-9]+)$ ]] || exit 1
             ordinal=${BASH_REMATCH[1]}
             [[ $ordinal -eq 0 ]] && exit 0
             # Clone data from previous peer.
             ncat --recv-only mysql-$(($ordinal-1)).mysql 3307 | xbstream -x -C /var/lib/mysql
             # Prepare the backup.
             xtrabackup --prepare --target-dir=/var/lib/mysql
           volumeMounts:
           - name: data
             mountPath: /var/lib/mysql
             subPath: mysql
           - name: conf
             mountPath: /etc/mysql/conf.d
         containers: # 这里声明的两个容器和前面两个步骤使用的容器是对应的
         - name: mysql
           image: mysql:5.7
           env: # 允许进去就不需要密码，需要密码应该是需要启动指令加密码就好 mysql:8.0注意权限又其他设置
           - name: MYSQL_ALLOW_EMPTY_PASSWORD
             value: "1"
           ports:
           - name: mysql
             containerPort: 3306
           volumeMounts:
           - name: data
             mountPath: /var/lib/mysql
             subPath: mysql
           - name: conf
             mountPath: /etc/mysql/conf.d
           resources:
             requests:
               cpu: 500m
               memory: 1Gi
           livenessProbe:
             exec:
               command: ["mysqladmin", "ping"]
             initialDelaySeconds: 30
             periodSeconds: 10
             timeoutSeconds: 5
           readinessProbe: #是一个探针，专门来看这个数据库是否存活
             exec:
               # Check we can execute queries over TCP (skip-networking is off).
               command: ["mysql", "-h", "127.0.0.1", "-e", "SELECT 1"]
             initialDelaySeconds: 5
             periodSeconds: 2
             timeoutSeconds: 1
         - name: xtrabackup
           image: gcr.io/google-samples/xtrabackup:1.0
           ports:
           - name: xtrabackup
             containerPort: 3307
           command:
           - bash
           - "-c"
           - |
             set -ex
             cd /var/lib/mysql
   
             # Determine binlog position of cloned data, if any.
             if [[ -f xtrabackup_slave_info ]]; then
               # XtraBackup already generated a partial "CHANGE MASTER TO" query
               # because we're cloning from an existing slave.
               mv xtrabackup_slave_info change_master_to.sql.in
               # Ignore xtrabackup_binlog_info in this case (it's useless).
               rm -f xtrabackup_binlog_info
             elif [[ -f xtrabackup_binlog_info ]]; then
               # We're cloning directly from master. Parse binlog position.
               [[ `cat xtrabackup_binlog_info` =~ ^(.*?)[[:space:]]+(.*?)$ ]] || exit 1
               rm xtrabackup_binlog_info
               echo "CHANGE MASTER TO MASTER_LOG_FILE='${BASH_REMATCH[1]}',\
                     MASTER_LOG_POS=${BASH_REMATCH[2]}" > change_master_to.sql.in
             fi
   
             # Check if we need to complete a clone by starting replication.
             if [[ -f change_master_to.sql.in ]]; then
               echo "Waiting for mysqld to be ready (accepting connections)"
               until mysql -h 127.0.0.1 -e "SELECT 1"; do sleep 1; done
   
               echo "Initializing replication from clone position"
               # In case of container restart, attempt this at-most-once.
               mv change_master_to.sql.in change_master_to.sql.orig
               mysql -h 127.0.0.1 <<EOF
             $(<change_master_to.sql.orig),
               MASTER_HOST='mysql-0.mysql',
               MASTER_USER='root',
               MASTER_PASSWORD='',
               MASTER_CONNECT_RETRY=10;
             START SLAVE;
             EOF
             fi
   
             # Start a server to send backups when requested by peers.
             exec ncat --listen --keep-open --send-only --max-conns=1 3307 -c \
               "xtrabackup --backup --slave-info --stream=xbstream --host=127.0.0.1 --user=root"
           volumeMounts:
           - name: data
             mountPath: /var/lib/mysql
             subPath: mysql
           - name: conf
             mountPath: /etc/mysql/conf.d
           resources:
             requests:
               cpu: 100m
               memory: 100Mi
         volumes: # 因为这个是我们提供的，所以我们要主动的挂载进去，这个和前面的VolumesMount里面的conf/config-map是对应好的。
         - name: conf
           emptyDir: {}
         - name: config-map
           configMap:
             name: mysql
     volumeClaimTemplates:
     - metadata:
         name: data
       spec:
         accessModes: ["ReadWriteOnce"]
         resources:
           requests:
             storage: 10Gi
   ```

### 使用上述教程出现问题的情况

根本原因是Istio不支持statefulset的原因，详见[这里](<https://github.com/istio/istio/issues/10659>)。

会导致的现象如下：

1. 文件丢失

   ![aliyun-log](./images/aliyun-log.png)

   ![diff](./images/diff.png)

   主要是图中的一个xtrabackup_checkpoints，主要是这个文件丢失。这个错误发生在上述的clone-mysql步骤中，所以其实slave还没有生成出来，所以我也看不到具体内部的情况（其实还是希望能看到尸体，但是不知道怎么做）。

   正常的log如下

   ![202-log](./images/202-log.png)

2. 发现如果我们replica = 1，没有slave数据库，应该能够使用，但是仍旧是无法服务发现。

   ![service-cannot-connect](./images/service-cannot-connect.png)

我们最终验证是Istio不支持statefulset的方式为使用控制变量法：

1. 由于是文件问题，我们主要先测试是不是PV的问题

   我们首先是把PV从NFS改到了本地local，这样我们才最终确定了是缺失了文件，当然是没有存在说情况有所改变

2. 由于服务发现也存在问题，我们按照[教程](https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/)进行学习，看是否有能够修正的

   当然是发现POD里面的DNS是是写入了`/etc/hosts`中的，DNS应该不存在问题

3. 决定进行降级的部署，使用deployment进行部署

   我们切换会deployment进行部署，从service进去，发现是ok的

4. 最终决定先直接将环境完全切换，切换到202学校集群（因为两个服务器的初始化方式不一样）。

   在202集群上直接部署成功，但是忽然想起来在202上因为重装，我没有装Istio

5. 在阿里云服务器上的mysql namespace上进行部署

   同样直接成功，于是基本就断定了是Istio的问题

## Istio的问题根源

我们查找了相关的github上的isssue之后，我们发现主要这些解决方案里面出现的关键词主要为**`MTLS ServiceEntry MeshPolicy DestinationRule`**

我们就这几点准备再复习下再看一看ISSUE，似乎是和mTLS有关

### 7月18日更新

根据[FAQ](https://istio.io/faq/security/#mysql-with-mtls)，发现确实是和mTLS有关，采取的解决方式也是有都有strict的方式，要么直接禁用mTLS。

## mysql日志mysql 日志记录

运维的一个重要部分就是进行日志的采集，对此mysql作为存储数据的地方，我们对mysql的日志应当进行详细的记录。

mysql存在三种日志

**error log**： 主要负责数据库中错误的记录，报错数据库开启和关闭，数据量相对而言不会很大

**general query log**：包含所有的语句的记录，数据量较大，并不适合采集，并且比较影响数据库的性能。

**bin log**：包含所有更改数据的语句，性能影响小，主要是用于数据库恢复和数据库主从复制

bin log在我们的主从数据库上已经有所体现，因此，我们仅需要收集err log即可。

## mysql性能压力测评

由于我们并不是使用单机数据库，使用的是主从数据库，我们使用主从数据库的原因就在于为了在对写低影响的情况下，尽可能的进行读的优化，为了最终证实我们的性能优化是可靠的，我们决定进行压力测试，以方便进行性能调优。

我们使用sysbench(sysbench 1.1)进行性能测试，主要是针对读和写两个方面来进行衡量——读要保证尽可能的高效，写要尽可能的降低写和传播的消耗。

读的测试板子如下

```bash
sysbench oltp_read_only
--mysql-table-engine=innodb \
--mysql-host=202.120.40.8 \
--mysql-port=30854 \
--mysql-db=sys-test \
--mysql-user=root \
--mysql-password=incongrous \
prepare
```

测试数据在execel中，数据库集群的读取能力基本是单体数据库的200%，基本符合我们的预期，随着数据库表增加，相应的处理速度降低，但是没有影响到这个处理数的两倍关系，other的数量逐步降低可能需要进一步的理论解释。

写的测试板子如下：

```bash
sysbench oltp_write_only --mysql-host=127.0.0.1 /
--mysql-user=root /
--db-driver=mysql /
--threads=100 /
--time=200 /
--mysql-db=sbtest /
--mysql-port=3307 /
run/prepare/cleanup
```

我们针对主键自增和非自增两种情况我们发现，单机数据库的性能都要优于主从数据库中对主数据库的写入，且幅度较大，我们调整线程数发现，在线程数较低时集群内写入性能较好，但随着线程数量的增加，集群内数据库的写入速度放缓，最终走向降低，而裸机的写入速度基本保持稳步上升的态势。

主要原因时因为裸机的性能不如集群内服务器，因此在一个线程时，主要的瓶颈在于thread无法高效的提供相应的请求。而集群内数据库存在相应的主从复制的相关问题，因此随着请求数量的增多，处理速率变缓，最终走向下降，且错误的情况也同时增多。

后续的测试主要针对是否从数据库的数量变化会导致相应的写的能力下降的情况如何，找到这其中的trade off，并且尝试提高数据库的buffer大小，从而提高数据库性能。