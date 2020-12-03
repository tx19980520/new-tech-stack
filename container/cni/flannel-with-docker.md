# flannel with docker

目前有新的需求，关于在多个虚拟机中容器层面在三层互通，这里最终选取的方案是 fiannel with docker。

以下记录相关环境安装的过程。

## Version

Etcd：v3.2.17

flannel：v0.10.0

docker: 19.03.14

## Etcd

这里 etcd 是在公网安装的，需要注意的是，我们虽然使用的是Etcd v3，但是 flannel v0.10.0 只支持 Etcd v2，为了兼容，这里在启动的时候需要的添加额外的参数。

```shell
docker run --name flannel-etcd -itd -e ALLOW_NONE_AUTHENTICATION=yes -p 3379:2379 -p 3380:2380 quay.io/coreos/etcd:v3.2.17 etcd --advertise-client-urls http://0.0.0.0:2379 --listen-client-urls http://0.0.0.0:2379 --enable-v2
```

相关的参数可以自己调配，这里设置的是最简单的参数。

启动之后，需要注入相关的参数

```shell
ETCDCTL_API=2
ectdctl set  /flannel/network/config '{"Network": "10.1.0.0/16", "SubnetLen": 24, "Backend": {"Type": "vxlan"}}'
```

这里的这个 key 和 之后的 `FLANNEL_ETCD_PREFIX`对应。

## flannel

flannel 在此主要推荐使用systemd进行启动，比较符合新需求的要求（新的虚拟机比较好注入）

```shell
wget https://github.com/coreos/flannel/releases/download/v0.10.0/flannel-v0.10.0-linux-amd64.tar.gz
tar xzvf ./flannel-v0.10.0-linux-amd64.tar.gz
cp ./flanneld /usr/local/bin
cp ./mk-docker-opts.sh /usr/local/bin/
```

### 配置 Systemd

配置 /lib/systemd/system/flanneld.service

```ini
[Unit]
Description=Flanneld overlay address etcd agent
Documentation=https://github.com/coreos/flannel
After=network.target
After=network-online.target
Wants=network-online.target
Before=docker.service

[Service]
User=root
Type=notify
LimitNOFILE=65536
EnvironmentFile=/etc/flannel/flanneld.conf
ExecStart=/usr/local/bin/flanneld \
-etcd-endpoints=${FLANNEL_ETCD_ENDPOINTS} \
-etcd-prefix=${FLANNEL_ETCD_PREFIX} $FLANNEL_OPTIONS
ExecStartPost=/usr/local/bin/mk-docker-opts.sh -k DOCKER_NETWORK_OPTIONS -d /run/flannel/docker
Restart=on-failure


[Install]
WantedBy=multi-user.target
```

配置 /etc/flannel/flanneld.conf

```ini
FLANNEL_ETCD_ENDPOINTS="http://106.15.225.249:3379" # etcd 的位置
FLANNEL_ETCD_PREFIX="/flannel/network" # 这里的参数与 etcd 里面数据的prefix key是对应的
FLANNEL_OPTIONS="-iface=ens3 #具体该用什么iface 建议ifconfig 看一下
```

## 安装 docker

详见 [这里](https://www.digitalocean.com/community/tutorials/how-to-install-and-use-docker-on-ubuntu-20-04)

## 配置 docker

docker 需要添加相关的参数，从而使用 flannel 来分配 ip

```ini
[Service]
Type=notify
# the default is not to use systemd for cgroups because the delegate issues still
# exists and systemd currently does not support the cgroup feature set required
# for containers run by docker
EnvironmentFile=/run/flannel/docker
ExecStart=/usr/bin/dockerd -H fd:// $DOCKER_NETWORK_OPTIONS --containerd=/run/containerd/containerd.sock
```

这里的 /run/flannel/docker 里面的参数是 flanneld 启动之后生成的，里面存入了 `DOCKER_NETWORK_OPTIONS`为 docker 启动添加参数

## 启动 overlay

在不同的机器上启动

```
sudo systemctl restart flanneld.service
sudo systemctl restart docker.service
```

之后各自 run 两个的 busybox， inspect 发现 ip 不是默认用 docker0 的 ip， 进入容器内 ping 对方的 ip，发现能连通。

进一步我们用 tcpdump 听一下 flannel.1 ，整个表现比较正常，基本可以完成网络互通。