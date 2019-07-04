# Istio Stack

本文档将详细讲述Istio的一些详细技术和实践

## Istio JWT check with mTLS

![istio-jwt-mtls](D:\kubecon\Istio\istio-jwt-mtls.png)

上图能够非常明确的体现出对我们的JWT check是在proxy的部分实现的。

### mTLS

mTLS是双向TLS，主要是考虑到服务到服务之间的通信，需要在同一信息量上进行双向加锁（即两个证书都会在一个信息上）。mTLS在Istio里面默认是宽容模式PERMISSIVE，PERMISSIVE下是允许进行明文交互。这种情况下是不进行authentication or authorization因此，我们需要把该模式改成STRICT。

#### JWT

jwt部分需要我们自己提供issuer，网络上大部分的博客都是使用auth0，即第三方的token发放和鉴权，我们这里准备自行提供issuer。

在此我们需要使用非对称加密，然后对外暴露我的公钥，将公钥放置在服务器上，对外公开。

这个地方要把过期时间设置好，注意在JAVA里面时间是以毫秒为单位，如果过期时间到了，仍旧会返回401。

#### RSA Public Key  Convert Into JWK(JSON WEB KEY)

详见convert.py脚本，注意只是格式上的改变，之后将该json放置在集群内部，我们后续将尝试挂载在阿里云的内网中。