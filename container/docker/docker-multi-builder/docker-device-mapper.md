# Docker的存储机制与Devicemapper

## Docker image的结构

从docker的使用中可知，docker的image是层状结构，所以才能在一个image的基础上能够衍生出新的image，并且能够覆写靠前层的相关内容（例如CMD）。

image使用UnionFS，可以将多层目录挂载到一起，形成一个虚拟的文件系统。docker镜像中每一层文件系统都是只读权限，即我们在没有依据某个镜像创建container的时候，我们无法对其进行修改。

而当我们创建容器之后，我们系统会分配一层空的有read-write权限的层，仅用于保存我们的修改。

## Docker image的存储形式

现目前docker支持的五种镜像层次的存储driver为：aufs、device mapper、btrfs、vfs、overlay。在此我们以device mapper为准来描述存储。

我们首先是寻找一个ContainerID为`8175e6c8b4b9`的Container的Image相关信息，在`/var/lib/docker/image/devicemapper/layerdb/mounts/8175e6c8b4b9xxxx`中，我们可以获取到该Container的`mount-id`为′f56exxx，之后我们能够在`/var/lib/docker/devicemapper/metadata`中找`mount-id`同名文件中存在如下信息：

```json
{"device_id":3386,"size":10737418240,"transaction_id":9113,"initialized":false,"deleted":false}
```

之后我们去查询`dmsetup table`找到对应的device

`docker-0:19-21278-f56e16dd270e2124fc6986cefc49a6037ce8f7ae661f8210db9bb5b3f64496b0: 0 20971520 thin 252:0 3386`

一种可行的操作是自己创造一个device（类似于一个agent）然后会在dev中发现该device，即可进行操作，其实我们直接在`/var/lib/docker/devicemapper/mnt/f56e16dd270e2124fc6986cefc49a6037ce8f7ae661f8210db9bb5b3f64496b0/rootfs`中就可以直接操作了