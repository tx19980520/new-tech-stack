# Travis CI

Travis CI 是一个在线的，分布式的持续集成服务，用来构建及测试在GitHub上托管的代码。

## 优势

1. 无需像jenkins一样自己提供服务器，自己搭建
2. 除去一个配置文件没有多余的文件配置

## 核心步骤

### github 页面配置

在github中对Travis CI进行授权

### 添加 .travis.yml

文件结构主要如下

```yaml
language: go
sudo: true
git:
  depth: 3 # 表示只clone最近三次commit 防止timeout
env:
  - DUMP=true # gommon/util/testutil.Dump
	# 配置环境变量
go:
  - "1.10"
  - tip
  # go 的版本等相关配置
  
before_install: # 在后期进行部署时，需要远程登录，进而需要一些安全设置
  - openssl aes-256-cbc -K $encrypted_d89376f3278d_key -iv $encrypted_d89376f3278d_iv
    -in id_rsa.enc -out ~/.ssh/id_rsa -d
  - chmod 600 ~/.ssh/id_rsa
install:
  - go get -u github.com/golang/dep/cmd/dep
  - dep version
  - make dep-install

script:
  - make install
  - make test

notifications: # 用于发送项目构建的message
  email:
    recipients:
    - xxxx@xxx.com
    on_success: always
    on_failure: always
after_success:
  - ssh xxx@xxxx.xxxx.xxxx.xxxx "./deploy.sh" # 请替换成自己的登录IP和登录用户，deploy.sh是自己写的部署脚本

addons:
  ssh_known_hosts: xxxx.xxxx.xxxx.xxxx # 请替换成自己的服务器IP
```

