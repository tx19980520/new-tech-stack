# zookeeper的基本使用和利用zookeeper watcher 实现服务发现

## 简单使用

```bash
sudo docker run -itd --name zookeeper -p 30308:2181 --restart always zookeeper
sudo docker exec -it --tty zookeeper /bin/bash
```

