# 关于使用kubespray启动集群

```bash
sudo apt-get install python-dev python-pip libxml2-dev libxslt1-dev zlib1g-dev libffi-dev libssl-dev #安装相关依赖
pip3 install -r requirements.txt

```

上述均为kubespray的基本环境的安装，保证其相应的脚本能正常运行

```bash
CONFIG_FILE=inventory/mycluster/hosts.yml python3 contrib/inventory_builder/inventory.py ${IPS[@]}
# 使用脚本生成hosts的配置

all:
  hosts:
    node1:
      access_ip: 10.0.0.78
      ip: 10.0.0.78
      ansible_host: 10.0.0.78
    node2:
      access_ip: 10.0.0.24
      ip: 10.0.0.24
      ansible_host: 10.0.0.24
    node3:
      access_ip: 10.0.0.74
      ip: 10.0.0.74
      ansible_host: 10.0.0.74
    node4:
      access_ip: 10.0.0.37
      ip: 10.0.0.37
      ansible_host: 10.0.0.37
  children:
    kube-master:
      hosts:
        node2:
        node1:
    kube-node:
      hosts:
        node4:
        node2:
        node3:
        node1:
    etcd:
      hosts:
        node2:
        node3:
        node1:
    k8s-cluster:
      children:
        kube-node:
        kube-master:
    calico-rr:
      hosts: {}
```

基本是集群的基本网络设置，基础网络设施和集群基本设施的决策位置

具体的集群配置参数在`inventory/mycluster/group_vars`

我们这里具体讲讲这个文件夹内的我们比较在意的相关配置文件，主要在k8s-cluster

k8s-cluster.yaml:

```yaml
# 本文件里面主要关注的是k8s的相关初始化和引用配置, 主要考虑如下字段的修改
kube_image_repo: "gcr.azk8s.cn/google-containers" # 国内换源
kube_service_addresses: 10.233.0.0/18
kube_pods_subnet: 10.233.64.0/18 # 这两个配置主要是要配合我们的网络相关配置，一定要是未使用的网段。
```

addons.yaml:

```yaml
# 一些kubernetes相关的插件，我们这里关闭了dashboard，开启了helm和metrics server，volume provisioner我们选择关闭，因为我们使用的是阿里云nas进行相关文件的配置，metrics_server开启是观察在新版本下这metrics这么安装有没有bug
# RBAC required. see docs/getting-started.md for access details.
dashboard_enabled: false

# Helm deployment
helm_enabled: true

# Registry deployment
registry_enabled: false
# registry_namespace: kube-system
# registry_storage_class: ""
# registry_disk_size: "10Gi"

# Metrics Server deployment
metrics_server_enabled: true
# metrics_server_kubelet_insecure_tls: true
# metrics_server_metric_resolution: 60s
# metrics_server_kubelet_preferred_address_types: "InternalIP"

# Rancher Local Path Provisioner
local_path_provisioner_enabled: false
# local_path_provisioner_namespace: "local-path-storage"
# local_path_provisioner_storage_class: "local-path"
# local_path_provisioner_reclaim_policy: Delete
# local_path_provisioner_claim_root: /opt/local-path-provisioner/
# local_path_provisioner_debug: false
# local_path_provisioner_image_repo: "rancher/local-path-provisioner"
# local_path_provisioner_image_tag: "v0.0.2"

# Local volume provisioner deployment
local_volume_provisioner_enabled: false
# local_volume_provisioner_namespace: kube-system
# local_volume_provisioner_storage_classes:
#   local-storage:
#     host_dir: /mnt/disks
#     mount_dir: /mnt/disks
#     volume_mode: Filesystem
#     fs_type: ext4
#   fast-disks:
#     host_dir: /mnt/fast-disks
#     mount_dir: /mnt/fast-disks
#     block_cleaner_command:
#       - "/scripts/shred.sh"
#       - "2"
#     volume_mode: Filesystem
#     fs_type: ext4
```

```bash
# 这个地方我们需要注意，就是常用的操作是我们的某master当做跳板机，然后我们先连接到跳板机然后再连接到其他的work node(其实基本上都没有到work node的需求)，这个时候各work node的公钥其实是跳板机的公钥，但是跳板机自己的公钥是不对应自己的私钥的因此你需要
cat id_rsa.pub >> ./authorized_keys #可选步骤，这个比较特殊
ansible-playbook -i inventory/mycluster/hosts.yml cluster.yml -b -v \
  --private-key=~/.ssh/id_rsa
# 该步是通过yml的配置文件，对各节点进行自动化的基建部署
# 此处建议大家使用grep -r 'gcr'过一遍文件，不然后面下载文件会出错，建议换源
```

```bash
# 最后全部安装好了之后则会发现并无法使用 localhost:8080来获取信息，需要开启权限和端口
# 在kubespray/inventory/mycluster/group_vars/k8s-cluster$ sudo vim k8s-cluster.yml开放8080
#即便是root也会报 Unable to connect to the server: x509: certificate signed by unknown authority (possibly because of "crypto/rsa: verification error" while trying to verify candidate authority certificate "kubernetes")
sudo cp /etc/kubernetes/admin.conf $HOME/
sudo chown $(id -u):$(id -g) $HOME/admin.conf
export KUBECONFIG=$HOME/admin.conf
```

之后`kubectl get nodes`可以使用。

## 添加节点

添加节点的主要问题是网络连接的问题，需要考虑到新节点的网络与原集群网络的一个融合，之前进行节点的添加导致了整个集群的网络混乱。

```bash
# 首先建议新添加的机子是个只有系统盘的机子，不然会因为环境的相关问题导致一些意外的情况
# 修改host.yml,添加新加点的ip
ansible-playbook -i inventory/mycluster/hosts.yml scale.yml -b -v \
--private-key=~/.ssh/id_rsa
# 上述命令我跑不通，原因不详 仍旧使用cluster.yml
# 请注意尽量保证各集群的节点是干净的，不然会出现不必要的麻烦
# master的防火墙记得关闭，work node的交换区请关闭
```

