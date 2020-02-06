

# cgroup

cgroup是linux内核提供的资源管理组件，cgroup主要管理其中的各式各样的subsystem，每一个子系统将分管一种资源，cgroup的管理单元为一组进程，而进程也有其相应的子进程，所以cgroup也有相应的hierarchy。

![slice](D:\new-tech-stack\docker\images\slice.png)。



systemd会自动创建slice、scope和service，所有的资源如上图会被默认划分成为三个cgroup：System、User和Machine。每一个cgroup都是一个slice，每一个slice都可以继续进行slice的划分，也与之前提到的层级结构相呼应。

而cgroup的整个机制，就在于设置相对应的rule，存储在相应的文件中，建立相应的监控，最终作出相应的应对策略。

## CPU

在阿里云学生机上进行实践：

```bash
[root@izuf6j17ifxfedan90o85nz ~]# systemd-cgls --no-page
├─1 /usr/lib/systemd/systemd --switched-root --system --deserialize 21
├─user.slice
│ └─user-0.slice
│   ├─session-619277.scope
│   │ ├─ 3471 sshd: root@pts/7
│   │ ├─ 3473 -bash
│   │ └─11453 systemd-cgls --no-page
│   ├─session-29876.scope
│   │ └─19609 proftpd: (accepting connections)
│   └─session-1.scope
│     ├─ 1474 tmux
│     ├─ 2936 -bash
│     ├─ 3217 -bash
│     ├─ 7376 -bash
│     ├─12425 -bash
│     ├─12495 -bash
│     ├─13759 java -jar -client oss-0.0.1-SNAPSHOT.jar
│     ├─18677 -bash
│     ├─26173 java -Dserver.port=3031 -jar pledge-0.0.1-SNAPSHOT.jar --spring.profiles.act...
│     ├─26946 -bash
│     ├─27015 -bash
│     ├─27092 sudo vim a.dump
│     ├─27093 vim a.dump
│     ├─27696 -bash
│     ├─29131 node /usr/bin/http-server -p 7070
│     └─32445 -bash
└─system.slice
  ├─docker-d2587c13dc25fd73663fcb689ce9ab6e38ad80be2aec7d4d85cbde229c3b514f.scope
  │ └─26605 redis-server *:6379
  ├─docker-8c767de561fbf90492c8d46fc8f3c96cd27f824922a7e32e51351715ce53f726.scope
  │ └─26329 mysqld
  ├─docker-2ed57da897d2941b35646851831b543bde00659078793d28012c10952b2c8a7c.scope
  │ ├─25575 nginx: master process nginx
  │ ├─25620 nginx: worker process
  │ ├─25621 nginx: worker process
  │ └─25622 nginx: cache manager process
  ├─docker.service
  │ ├─ 5619 /usr/bin/dockerd-current --add-runtime docker-runc=/usr/libexec/docker/docker-...
  │ ├─ 5624 /usr/bin/docker-containerd-current -l unix:///var/run/docker/libcontainerd/doc...
  │ ├─25563 /usr/bin/docker-containerd-shim-current 2ed57da897d2941b35646851831b543bde0065...
  │ ├─26272 /usr/libexec/docker/docker-proxy-current -proto tcp -host-ip 0.0.0.0 -host-por...
  │ ├─26319 /usr/bin/docker-containerd-shim-current 8c767de561fbf90492c8d46fc8f3c96cd27f82...
  │ ├─26583 /usr/libexec/docker/docker-proxy-current -proto tcp -host-ip 0.0.0.0 -host-por...
  │ └─26587 /usr/bin/docker-containerd-shim-current d2587c13dc25fd73663fcb689ce9ab6e38ad80...
  ├─lvm2-lvmetad.service
  │ └─7801 /usr/sbin/lvmetad -f
  ├─auditd.service
  │ └─7132 /sbin/auditd
  ├─aliyun.service
  │ └─1202 /usr/sbin/aliyun-service
  ├─sshd.service
  │ └─1131 /usr/sbin/sshd -D
  ├─aegis.service
  │ ├─1770 /usr/local/aegis/aegis_update/AliYunDunUpdate
  │ └─1800 /usr/local/aegis/aegis_client/aegis_10_75/AliYunDun
  ├─tuned.service
  │ └─781 /usr/bin/python -Es /usr/sbin/tuned -l -P
  ├─ntpd.service
  │ └─798 /usr/sbin/ntpd -u ntp:ntp -g
  ├─atd.service
  │ └─486 /usr/sbin/atd -f
  ├─crond.service
  │ └─483 /usr/sbin/crond -n
  ├─network.service
  │ └─716 /sbin/dhclient -1 -q -lf /var/lib/dhclient/dhclient--eth0.lease -pf /var/run/dhc...
  ├─dbus.service
  │ └─467 /usr/bin/dbus-daemon --system --address=systemd: --nofork --nopidfile --systemd-...
  ├─polkit.service
  │ └─465 /usr/lib/polkit-1/polkitd --no-debug
  ├─systemd-logind.service
  │ └─463 /usr/lib/systemd/systemd-logind
  ├─rsyslog.service
  │ └─462 /usr/sbin/rsyslogd -n
  ├─systemd-udevd.service
  │ └─351 /usr/lib/systemd/systemd-udevd
  ├─system-getty.slice
  │ └─getty@tty1.service
  │   └─507 /sbin/agetty --noclear tty1 linux
  ├─system-serial\x2dgetty.slice
  │ └─serial-getty@ttyS0.service
  │   └─506 /sbin/agetty --keep-baud 115200 38400 9600 ttyS0 vt220
  └─systemd-journald.service
    └─329 /usr/lib/systemd/systemd-journald

```

我们可以看到这里只存在system和user两个slice，这是因为我们在该台服务器上没有开启虚拟机，所以只需要两份slice，另外一个细节在于，我们在该服务器上启用了一个java的server，按道理他会启用jvm来运行，但是这里将该进程放置在了user sclice。

我们在此简单讲解一下在linux下使用cmd如何对cpu进行限制

```bash
systemctl set-property user-0.slice CPUQuota=4%
systemctl set-property user-1000.slice CPUShares=256
```

上述两种控制cpu使用时间的方法有所不同，CPUQuota属于严格控制，即任何时刻都不能使用超过5%的CPU时间，CPUShares不同，他指在CPU繁忙时期，该进程（组）能过获取到的CPU份数，而份数的总数量是不确定的。

但是对于多核的情况，CPUShares是没有办法处理多核的情况的，因为CPUShares中的share主要是针对有单核进行，我们要是多核系统下还是使用CPUQuota进行相应的限制工作。

```bash
systemctl set-property user-0.slice CPUQuota=200%
```

## Memory

从程序运行的角度而言，可压缩资源(conpressible resurces)——CPU资源和不可压缩资源(non-compressible resources)——内存和磁盘资源，相对而言，Memory更加的严格。

我们这里提到了磁盘资源，但是确实我们没有办法按照进程来监控磁盘资源，因为我们可能在这个磁盘上创建了文件，但是之后我们便不再继续使用，但是内存的隔离在进程上是天然的，因此只存在数值上的限制即可。

这里我们是使用相关创建文件的方式来进行新的cgroup，没有找到相应的ctl进行操作。

我们在`/sys/fs/cgroup/memory`进行相应的mkdir操作，就能够进行相应的cgroup的操作，在我们创建了test文件夹之后，相应的与复层类似的文件结构就已经创建好了，我们只需要对文件进行操作即可

```bash
cgroup.clone_children               memory.memsw.failcnt
cgroup.event_control                memory.memsw.limit_in_bytes
cgroup.procs                        memory.memsw.max_usage_in_bytes
memory.failcnt                      memory.memsw.usage_in_bytes
memory.force_empty                  memory.move_charge_at_immigrate
memory.kmem.failcnt                 memory.numa_stat
memory.kmem.limit_in_bytes          memory.oom_control
memory.kmem.max_usage_in_bytes      memory.pressure_level
memory.kmem.slabinfo                memory.soft_limit_in_bytes
memory.kmem.tcp.failcnt             memory.stat
memory.kmem.tcp.limit_in_bytes      memory.swappiness
memory.kmem.tcp.max_usage_in_bytes  memory.usage_in_bytes
memory.kmem.tcp.usage_in_bytes      memory.use_hierarchy
memory.kmem.usage_in_bytes          notify_on_release
memory.limit_in_bytes               tasks
memory.max_usage_in_bytes
```

我们讲述一些常规的操作：

### 添加进程进入到cgroup

主要是对cgroup.procs的操作，将PID号加入到相应的cgroup.procs即可

```bash
sudo sh -c "echo $$ >> cgroup.procs"
```

### 设置限制值

```bash
sudo sh -c "echo 1M > memory.limit_in_bytes"
```

如果不需要再进行限制时，则直接设置的-1即可。

### 触发限制

系统默认的行为的是kill掉cgroup中继续申请内存的进程，如果需要自定义相应的操作，需要配置memory.oom_control，这个文件中存在一个控制是否为当前cgroup启动OOM-killer的标识。如果对这个文件写0，将启动OOM-killer，当内核无法给内存分配足够的内存时，将会直接kill掉该进程；如果写1，则表示不启动的OOM-killer，而是暂定该进程知道有空余的内存之后再继续进行。

该文件还可以配合cgroup.event_control实现OOM的通知，当OOM发生时，可以收到相关的事件，主要是配合eventfd进行操作。

这其实也就是docker为什么能收到相关event。

更多的详情可见[这里](https://segmentfault.com/a/1190000008125359)

## jvm与docker

我们这之前很早就提及到相关在docker中部署springboot应用的相关问题，就是有关于jvm申请内存和CPU使用的问题。因为默认的在java8中，java是不会对cgroup进行理会的。Docker通过CGroups完成的是对内存的限制，而/proc目录是已只读形式挂载到容器中的，由于默认情况下Java压根就看不见CGroups的限制的内存大小，而默认使用/proc/meminfo中的信息作为内存信息进行启动，这种不兼容情况会导致，如果容器分配的内存小于JVM的内存，cgroup会认为JVM是一个流氓进程，最终会被意外的杀掉。

这里需要提到，如果我们开启jvm的相应参数，低版本的jvm的处理方式是将其认为是jvm环境下的host memory容积，所以，在这种情况下jvm仍旧只是会设置相应的最大堆内存为docker 限制内存的1/4，这确实会浪费不少的memory资源。