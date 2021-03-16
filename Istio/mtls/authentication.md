---
authors: [""]
reviewers: [""]
---

# 认证

由于 mTLS 在数据层面上是依靠证书进行工作，本文中也将对证书进行一部分的探究，并结合到 Istio 的具体使用中体验。

## mTLS handshake 过程

1. ClientHello
2. ServerHello
3. ServerCertificate
4. CertificateRequest
5. ServerHelloDone
6. ClientCertificate
7. ClientSendEncryptText
8. ClientKeyExchange
9. ClientChangeCipherSpec
10. ClientHandshakeFinished
11. ServerChangeCipherSpec
12. ServerHandshakeFinished

mTLS 的鉴权方式是 TLS 的一种扩展，这种扩展主要表现在第4步至第7步，我们将分段为大家进行阐述。在此我们假定 Client 端和 Server 端的证书均由同一个机构签发。

### step1->step3

![mtls-handshake-partI](../images/mtls-handshake-parti.jpg)

step1 到 step3 整体与 TLS 的前3步没有区别。
client 首先执行 ClientHello，提供自己的 TLS Version，Compression Method，Cipher Suites，等待 Server 进行选择达成一致。并且发送一个随机数记为 C，注意这里的 C 是以明文传输的。SessionID 因为这个 session 还未建立，默认为0。

Server 对 Compression Method 和 Cipher Suites 进行选择，回传 SessionID，并附带上一个随机数记为 S，注意这里的 S 是以明文传输的。

Server 首先将请求 OCSP Server 以确保自己的证书没有被主动吊销(一般情况下我们仅需要通过证书中的 expire time 就可知晓证书是否过期，但也存在主动吊销证书的情况)，随即 Server 将自己的证书发送给 Client，Client 使用 Root Certificate 对 Server Certificate 进行验证。有关证书发放和证书验证以及根证书的相关支持在后文的实践中可以进一步学习和理解。


前3步中较为重要信息为：
1. Client 与 Server 分别知晓了随机数 C 和 S 
2. Server 端的证书在 Client 端得到验证

### step4->step7

![mTls-handshake-partI](../images/mtls-handshake-partii.jpg)

step4->step7是 mTLS 中额外增加的一个部分。主要进行的工作是 Server 端验证 Client 端证书。

Server 向 Client 请求的 Client 端的相关证书。

Server 发出 ServerHelloDone 来表示自己在 Hello 部分的信息已经传递完成。

Client 在收到请求后，将 Client Certificate 发送至 Server。

Server 使用 Root Certificate 对 Client Certificate 进行验证。

Client 将之前收到的所有状态通过自己的私钥加密，发送给 Server 端。Server 端收到相应的内容后将使用 Client Certificate 中的公钥对内容进行解锁，并与自己的状态进行核对。

在完成 step7 之后，相比于 step3 完成后，主要完成的工作在 Server 端完成了 Client Certificate 的检验。此时两端已经相互信任，即将进入到收尾工作。

### step8->step12

![mTls-handshake-partIII](../images/mtls-handshake-partiii.jpg)
该阶段主要在于生成最终通信的 session key，并相互告知切换到使用 session key在整个 session 中用于加密 (对称加密)。
s
ClientKeyExchange 将会在 client 端生成一个 pre-master secret，利用 Server public Key 进行加密传输至Server端。此时两端分别使用随机数 C，S以及 pre-master secret 根据之前约定好的算法生成 session key。

ClientChangeCipherSpec 指 Client 端确认在本条信息之后 Client 端发出的消息均使用 session key 加密。

ClientHandshakeFinished 指 Client 将自身所有的状态通过 session key 加密传输到 Server 端供 Server 验证，以确保 handshake 过程中信息没有被篡改。

ServerChangeCipherSpec 指 Server 端确认在本条信息之后 Server 端发出的消息均使用 session key 加密。

ServerHandshakeFinished 指 Server 将自身所有的状态通过 session key 加密传输到 Client 端供 Client 验证，以确保 handshake 过程中信息没有被篡改。

至此，整个 mTLS 的 handshake 过程完成，进入到使用 session key 对称加密进行通信的阶段。我们将以 Envoy 为例，进一步探讨 mTLS 在 Envoy 中的实现，以及 Envoy 如何支持 Istio 的相关工作。

### X.509 证书结构简析

X.509 标准是密码学里公钥证书的格式标准。去除掉数学上的一些细节，我们重点关注的几个数据为：
1. 证书序列号，证书的唯一标识
2. 主体密钥，证书所有者的 public key
3. 证书的颁发者(CA 机构)
4. 签名值，将证书中的信息用 CA 机构的 private key 进行加密后的值，可用 public key 解密，用于比对上述信息没有被篡改

5. SAN（Subject Alternative Name），使用 subjectAltName 来扩展此证书支持的域名，使得一个证书可以支持多个不同域名的解析。SAN 的组织形式使用 [SPIFFE](https://github.com/spiffe/spiffe/blob/master/standards/X509-SVID.md)
```bash
$ kubectl exec $(kubectl get pod -l app=httpbin -o jsonpath={.items..metadata.name}) -c istio-proxy -- cat /etc/certs/cert-chain.pem | openssl x509 -text -noout  | grep 'Subject Alternative Name' -A 1
        X509v3 Subject Alternative Name:
            URI:spiffe://cluster.local/ns/default/sa/default
```

有关于 mTLS 的实现，我们主要观察 `istio/proxy` 中的 `validateX509` 函数。
```c
bool AuthenticatorBase::validateX509(const iaapi::MutualTls& mtls,
                                     Payload* payload) const {
  const Network::Connection* connection = filter_context_.connection();
  if (connection == nullptr) {
    // It's wrong if connection does not exist.
    ENVOY_LOG(error, "validateX509 failed: null connection.");
    return false;
  }
  // Always try to get principal and set to output if available.
  const bool has_user =
      connection->ssl() != nullptr &&
      connection->ssl()->peerCertificatePresented() &&
      Utils::GetPrincipal(connection, true,
                          payload->mutable_x509()->mutable_user());

  if (!has_user) {
    // For plaintext connection, return value depend on mode:
    // - PERMISSIVE: always true.
    // - STRICT: always false.
    switch (mtls.mode()) {
      case iaapi::MutualTls::PERMISSIVE:
        return true;
      case iaapi::MutualTls::STRICT:
        return false;
      default:
        NOT_REACHED_GCOVR_EXCL_LINE;
    }
  }

  // For TLS connection with valid certificate, validate trust domain for both
  // PERMISSIVE and STRICT mode.
  return validateTrustDomain(connection);
}
```

在 `validateX509` 会发现首先会检查 connection 中是否为明文传输等一些内容，并根据我们在运维工程中写入的配置文件进行处理（例如 PERMISSIVE 模式下在明文情况下将直接返回 true）。如果对方不为明文传输且拥有证书，则进入到 `validateTrustDomain`。

```c
bool AuthenticatorBase::validateTrustDomain(
    const Network::Connection* connection) const {
  std::string peer_trust_domain;
  if (!Utils::GetTrustDomain(connection, true, &peer_trust_domain)) {
    ENVOY_CONN_LOG(
        error, "trust domain validation failed: cannot get peer trust domain",
        *connection);
    return false;
  }

  std::string local_trust_domain;
  if (!Utils::GetTrustDomain(connection, false, &local_trust_domain)) {
    ENVOY_CONN_LOG(
        error, "trust domain validation failed: cannot get local trust domain",
        *connection);
    return false;
  }

  if (peer_trust_domain != local_trust_domain) {
    ENVOY_CONN_LOG(error,
                   "trust domain validation failed: peer trust domain {} "
                   "different from local trust domain {}",
                   *connection, peer_trust_domain, local_trust_domain);
    return false;
  }

  ENVOY_CONN_LOG(debug, "trust domain validation succeeded", *connection);
  return true;
}
```

代码中最为重要的比较就是 `peer_trust_domain != local_trust_domain`，这两个变量都是从从证书中获取。Istio 中实现 SPIFFE 来进行 identity，SPIFFE 的格式为
spiffe://ClusterName/ns/Namespace/sa/ServiceAccount，用这样的方式来记录 trust domain。