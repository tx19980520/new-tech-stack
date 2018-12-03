# Jenkins

## introduction

为了实现CI/CD，自动化测试与部署，与docker进行联动，我们需要使用Jenkins。

## install(Centos7)

```bash
yum install -y java
sudo wget -O /etc/yum.repos.d/jenkins.repo https://pkg.jenkins.io/redhat-stable/jenkins.repo
sudo rpm --import https://pkg.jenkins.io/redhat-stable/jenkins.io.key
yum install jenkins
```

到此Jenkins的初始化安装已经完成，我们后序的安装需要我们在浏览器中进行，如果你的服务器8080端口存在其他的服务，则应当修改Jenkins端口号

```
vi /etc/sysconfig/jenkins
JENKINS_PORT="XXXX"
```

启动Jenkins

```bash
service jenkins start/stop/restart
```

然后我们访问该服务器的XXXX端口，跳转到新的网页

该网页最开始会询问我们密码，我们使用`cat /var/lib/jenkins/secrets/initialAdminPassword `来获取password，之后按需选择就好。

本次我们实践的是将自己的github仓库与jenkins同步

我们需要在github仓库里面激活webhook，这个部分详见[here](https://www.cnblogs.com/weschen/p/6867885.html)

## config and working

