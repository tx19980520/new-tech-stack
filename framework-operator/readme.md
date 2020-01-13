# operator-framework

本文主要记录在创建operator-framework工作环境和operator-framework整体结构，意在打造一个舒适的开发环境。

## Dockerfile & VSCode remote

我们准备使用VSCode remote 连接在kubernetes master上的一个container进行工作，这样的方式有如下的原因：

1.  测试阶段需要link到kubernetes apiserver，因此在kubernetes master上，且container需要使用`--net=host`
2. go的环境比较难配，且开源项目版本更新快，需要更新golang的版本的可能性较大，我们需要用docker来进行进一步跟进版本
3. VSCode通过ssh直接连接到具体的文件夹下，开发方便。

Dockerfile主要分为三个部分

go install

```dockerfile
FROM ubuntu:xenial
# gcc for cgo
RUN apt update && apt install -y --no-install-recommends \
		g++ \
		gcc \
		libc6-dev \
		make \
		pkg-config \
        wget \
		curl

ENV GOLANG_VERSION 1.13.6
ENV OS linux-amd64
RUN set -eux; \
	\
	url="http://mirrors.ustc.edu.cn/golang/go${GOLANG_VERSION}.${OS}.tar.gz"; \
	wget -O go.tgz "$url"; \
	tar -C /usr/local -xzf go.tgz; \
	rm go.tgz; \
	export PATH="/usr/local/go/bin:$PATH"; \
	go version

ENV GOPATH /go
ENV PATH $GOPATH/bin:/usr/local/go/bin:$PATH

RUN mkdir -p "$GOPATH/src" "$GOPATH/bin" && chmod -R 777 "$GOPATH"
WORKDIR $GOPATH
```

这个地方需要说明的是，上文是借鉴了Golang官方的Dockerfile，为什么我们不直接从Golang官方镜像开始搭建呢，是因为我们后需要安装docker，docker官方支持的linux发行版如下：

- Disco 19.04
- Cosmic 18.10
- Bionic 18.04 (LTS)
- Xenial 16.04 (LTS)

但是官方的给出的版本都是bruster or alpine，则没有办法后续安装docker因此我们选择从ubuntu:xenial开始构建，然后国内构建换了中科大源。

docker install

```dockerfile
# install docker
RUN apt -y install \
    apt-transport-https \
    ca-certificates \
    gnupg-agent \
    software-properties-common \
    && curl -fsSL https://download.docker.com/linux/ubuntu/gpg | apt-key add - \
    &&  add-apt-repository \
   "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
   && apt update \
   && apt -y install docker-ce docker-ce-cli containerd.io;
```

标准docker install，官网标配

framework-operator install

```dockerfile
ENV OPERATOR_VERSION v0.14.0
# download & install operator-framework
RUN wget http://106.15.225.249:7070/operator-sdk-v0.14.0-x86_64-linux-gnu \
    && chmod +x operator-sdk-v0.14.0-x86_64-linux-gnu \
    && mkdir -p /usr/local/bin/ \
    && cp operator-sdk-v0.14.0-x86_64-linux-gnu /usr/local/bin/operator-sdk \
    && rm operator-sdk-v0.14.0-x86_64-linux-gnu;
```

此处确实没有找到相关operator-sdk的相关源，只能是自己建了个http-server，可以使用特殊的技能进行下载。

sshd install

```dockerfile
# download & install sshd for vscode
ENV SSH_PORT 30367
RUN apt install -y openssh-server \
    && sed -ie 's/Port 22/Port 30367/g' /etc/ssh/sshd_config;
# add ubuntu for ssh login
RUN adduser ubuntu && adduser ubuntu root
```

这里因为我们需要用ssh link上我们的container，而我们又会使用到`--net=host`来启动，因此要修改ssh默认端口，另一方面，因为ssh对于root登录的限制较多，我们最终决定添加新用户来ubuntu来用于登录，并且该用户的group为root。最后我简易的将宿主机ubuntu的.ssh直接mount进容器中.

另外一个问题在于我们之前创建project的时候是使用root创建的，为了能够用VSCode进行编辑，我们需要chown到用户ubuntu。

## operator-framework代码目录结构

```tree
|-- Dockerfile.env
|-- LICENSE
|-- README.md
|-- build
|   |-- Dockerfile
|   `-- bin
|       |-- entrypoint
|       `-- user_setup
|-- cmd
|   `-- manager
|       `-- main.go
|-- deploy
|   |-- operator.yaml
|   |-- role.yaml
|   |-- role_binding.yaml
|   `-- service_account.yaml
|-- go.mod
|-- go.sum
|-- pkg
|   |-- apis
|   |   `-- apis.go
|   `-- controller
|       `-- controller.go
|-- tools.go
`-- version
    `-- version.go
```

