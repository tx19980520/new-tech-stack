# 使用Istio 内置Promethues & Grafana进行集群监控

## Istio监控的原理与配置

Istio的监控主要是基于sidecar注入来进行，从这个角度来看，即sidecar能够直接监控到Pod层级的性能和流量，而不是通过Pod中的服务进行主动暴露和注册来进行资源的监控，只要是经过istio的流量或者被istio管控的Pod，都是有相应data source进入到Promethues和Grafana中的。

为了在集群中进行监控，Istio的配置文件中需要开启Promethues和Grafana

```yaml
#
# addon grafana configuration
#
grafana:
  enabled: true

#
# addon prometheus configuration
#
prometheus:
  enabled: true

```

### Promethues & Grafana

通过Promethues能够非常轻易的看到相关的统计数据

![promethues](./images/promethues.png)

我们可以在Promethues中进行相应的调试之后，将最终的统计方式在Grafana中更好的展示，其语法是相通的。

grafana中首先要设置好相应smtp和server的url，这相对比较重要，由于在Istio的相关配置中未见到相应的暴露的方法来修改grafana的配置文件，因此我们决定自己打包一个image进行操作，主要是修改了配置文件：

```ini
#################################### Server ##############################
[server]
# Protocol (http, https, socket)
protocol = http

# The ip address to bind to, empty will bind to all interfaces
http_addr =

# The http port to use
http_port = 3000

# The public facing domain name used to access grafana from a browser
domain = grafana.casecloud.com.cn

# Redirect to correct domain if host header does not match domain
# Prevents DNS rebinding attacks
enforce_domain = false

# The full public facing url
root_url = http://grafana.casecloud.com.cn/

# Log web requests
router_logging = false

# the path relative working path
static_root_path = public

# enable gzip
enable_gzip = false

# https certs & key file
cert_file =
cert_key =

# Unix socket path
socket = /tmp/grafana.sock
#################################### SMTP / Emailing #####################
[smtp]
enabled = true
host = smtp.qq.com:465
user = 1115433638@qq.com
# If the password contains # or ; you have to wrap it with triple quotes. Ex """#password;"""
password = xginrskzcftjbaah
cert_file =
key_file =
skip_verify = false
from_address = 1115433638@qq.com
from_name = Grafana
ehlo_identity =
```

smtp部分主要是host、user、password、以及from_address需要进行相关的配置，server部分其实主要是root_url，这个部分事关在邮件中的相关按钮的url是否正确（否则会是localhost:3000），smtp的开启可以自行百度qq邮箱smtp

之后在Grafana的页面中进行Alert的channel配置并且sent for notification，在邮箱中会收到如下的邮件。

![notification](./images/notification.png)

Alert可以认为是基于一个view的，我们可以新建一个view，之后在里面使用Promethues的语法进行统计， 并组织撰写相应的Rule，最后等待相应的邮件即可：

![email-alert](./images/email-alert.png)

简单流程测试如上

## 指标的选择

现阶段简单指标选取主要考虑三个大的方面，数据库和service和付费service

### 数据库

数据库方面因为不在Istio的负责范围内，因此我们主要监控的是其他服务从mysql中接收到的流量的速率，我们简单的认为，mysql中发送的数据量与之资源的消耗有直接的关系。

后序我们将会考虑到数据库消耗文件系统的量的大小来考虑相应的容积将如何处理。

### service

一般service方面，作为主体，我们能够直接观测到其CPU 和memory的使用，这也是我们重点将要考虑的，如果出现Alert的情况，我们希望的自动化的程度是在资源充足的情况下直接进行水平扩展，而不需要人为操作，如若遇到资源不足的情况，我们初期准备进行手动的添加资源，后期会逐步走向自动化。

### 付费Service

付费Service由于不对外进行暴露，在初期并没有将其纳入Istio管辖范围内，与数据库不同的是，通过收到的流量来进行判断是不合理的，我们可能会在后期更进一步的讨论这个问题，目前由于已经使用了nginx gate通过IP 对相应的接口进行限制，暂可不考虑其安全性问题。