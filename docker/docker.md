# docker的基本操作

1. 为什么我们要使用docker

   主要是因为一个比赛需要快速配环境，使用docker能够快速配置好linux服务器的相关环境。

## 安装

由于docker在windows或者macOS下是在虚拟机内跑的，我们需要使用oracle VM virtualBox，这里建议大家下载DockerToolBox，用于自动安装，比较方便。

下载完成并一直next之后，按照一般操作是需要你使用Docker QuickStart Terminal，需要使用到你的bash，所以大家在下载DockerToolBox同时下载bash并安装。

因为在windows下，docker container是在你的虚拟机下面跑的，当你打开Oracle VM virtualBox的时候，你需要能够对default虚拟机的网卡和bridge进行设置，如下图。

![net](./images/net.png)

需要提醒的是，虚拟机需要在每次使用docker的时候打开，这也是为什么不提倡大家在windows下使用的原因之一。

## 使用

docker的操作和git比较相似，dockerhub上有许多的images供大家选择，根据不同的需求可以得到不同的image

```shell
docker pull nginx #获得image
docker run --name demo xxxxxx nginx:latest # 通过image nginx:latest创建名为demo的docker container并运行
docker ps# 可以看到正在使用的docker进程
docker stop {id} # 停止docker进程
docker kill {id} # 无条件停止docker进程
```

另外大家可以使用Kitematic这个docker GUI，也比较方便大家使用

docker多用于部署web应用，我们必然会关注到端口的问题，docker可以进行端口映射，即我们docker容器的端口和我们主机的端口之间存在一个map，指令如下

```shell
docker run --name demo -p 8080:80 nginx:latest
```

上述的语句基本是说你主机的8080端口映射到container的80端口，比如我们就可以通过localhost:8080看到一个container中80端口nginx的默认欢迎页面。

注意，windows和mac用户没法在localhost看到，这是因为他们是运行在虚拟机中的，只有linux环境下这个操作才是可以的，windows和mac用户可以通过Kitematic看到该container的80端口被映射到了那个ip下面。

## mysql环境的搭建

为了搭建mysql环境，我预期使用mysql的image进行create container。在通过上述的类似操作之后，我发现我能execute command中用命令行进入到mysql，但是我使用PHP脚本的时候却被拒绝，这个原因在于mysql8.0的auth的默认模式并不是用户名密码的，因为我并没有什么特别的用途，我就直接退回到了mysql5.7，当然你也可以进入到数据库内部调整auth的方式为naïve mode

## Dockerfile的使用

为了更加自动化的搭建环境，我们选择使用Dockerfile来进行环境的搭建，Dockerfile的主要用意为以某个镜像为基础，在其上使用一系列的指令安装或启动其他软件达到最终的效果。

```dockerfile
# base image
FROM centos

# MAINTAINER
MAINTAINER ty0207 ty0207@sjtu.edu.cn

# put nginx-1.12.2.tar.gz into /usr/local/src and unpack nginx
ADD http://nginx.org/download/nginx-1.14.0.tar.gz .

#RUN 执行以下命令 
RUN yum install -y pcre-devel wget net-tools gcc zlib zlib-devel make openssl-devel
RUN useradd -M -s /sbin/nologin nginx
RUN tar -zxvf nginx-1.14.0.tar.gz
RUN mkdir -p /usr/local/nginx
RUN cd nginx-1.14.0 && ./configure --prefix=/usr/local/nginx --user=nginx --group=nginx --with-http_stub_status_module && make && make install
RUN ln -s /usr/local/nginx/sbin/* /usr/local/sbin/
 
#EXPOSE 映射端口
EXPOSE 80
 
#CMD 运行以下命令
CMD ["nginx"]
```

注意我们在run的时候，docker认为每run一次就是一层，最终会导致整个docker image层数非常深，于是我们应当在 Run的后面加上斜杠例如

```dockerfile
RUN useradd -M -s /sbin/nologin nginx
RUN tar -zxvf nginx-1.14.0.tar.gz
```

可以转化成：

```dockerfile
RUN useradd -M -s /sbin/nologin nginx \
    tar -zxvf nginx-1.14.0.tar.gz
```

## 挂载

另外由于我们在部署之后，可能很长时间我们是在更新代码，而如果仅仅是为了更新代码，我们完全没有必要更新docker（除非有一些新的软件需要使用，我们需要配置与安装），并且我们几个docker container可能会共享一些数据，比如一些静态数据或者是数据库，这需要我们对这些文件进行共享并且与docker中的数据双向绑定（当然大部分时候是改容器外的东西），我们常使用：

1. 命令行中的 `-v`options
2. Dockerfile中的 `VOLUME`关键字

