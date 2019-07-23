# Istio Quota的理解与实践

我们对官网的文档进行拆解之后进行解析

## handler

```yaml
# handler
apiVersion: config.istio.io/v1alpha2
kind: handler
metadata:
  name: quotahandler
  namespace: istio-system
spec:
  compiledAdapter: memquota
  params:
    quotas:
    - name: requestcountquota.instance.istio-system
      maxAmount: 500
      validDuration: 1s
      # The first matching override is applied.
      # A requestcount instance is checked against override dimensions.
      overrides:
      # The following override applies to 'reviews' regardless
      # of the source.
      - dimensions:
          destination: reviews
        maxAmount: 1
        validDuration: 5s
      # The following override applies to 'productpage' when
      # the source is a specific ip address.
      - dimensions:
          destination: productpage
          source: "10.28.11.20"
        maxAmount: 500
        validDuration: 1s
      # The following override applies to 'productpage' regardless
      # of the source.
      - dimensions:
          destination: productpage
        maxAmount: 2
        validDuration: 5s
```

handler部分主要关注的是你想要达到的最终目的，即我们rate limit最终想要达到的效果，首先是在没有任何其他限制条件的情况下，普适的使用500次/s来进行限制，对于特定的服务，例如上文中提到的reviews，我们限制为1次/5s。

对于productipage，我们配置了两条规则：

1. 如果访问productpage，且来源为10.28.11.20，限制为500次/s
2. 如果访问productpage，但来源不为10.28.11.20，限制为2次/5s

这个地方强调的重点是在于，读取规则是从上到下进行读取的，先适配到哪一条就先执行哪一条，所以如果10.28.11.20的限制是靠后的，最终的规则将变为

1. 如果访问productpage，限制为2次/5s

## instance

```yaml
apiVersion: config.istio.io/v1alpha2
kind: instance
metadata:
  name: requestcountquota
  namespace: istio-system
spec:
  compiledTemplate: quota
  params:
    dimensions:
      source: request.headers["x-forwarded-for"] | "unknown"
      destination: destination.labels["app"] | destination.service.name | "unknown"
      destinationVersion: destination.labels["version"] | "unknown"
```

在之前handler中，我们使用了较为固定的模板对规则进行定义，instance主要是为了告诉mixer我们的模板中的各变量对应了什么，需要与什么进行匹配。

例如我们对于source的匹配，这里选择了读取请求的头中的x-forward-ded-for这个字段进行比对，destination会先会比对labels，之后会比对service的name

## rule

```yaml
apiVersion: config.istio.io/v1alpha2
kind: rule
metadata:
  name: quota
  namespace: istio-system
spec:
  # quota only applies if you are not logged in.
  # match: match(request.headers["cookie"], "user=*") == false
  actions:
  - handler: quotahandler
    instances:
    - requestcountquota
```

rule主要是将我们的handler和我们的instance进行整合（“编译”）。再对该规则的整体匹配条件进行一个约束。

## QuotaSpec

```yaml
apiVersion: config.istio.io/v1alpha2
kind: QuotaSpec
metadata:
  name: request-count
  namespace: istio-system
spec:
  rules:
  - quotas:
    - charge: 1
      quota: requestcountquota
```

最为重要变量为charge，即每一次计数的数量（?）

```yaml
apiVersion: config.istio.io/v1alpha2
kind: QuotaSpecBinding
metadata:
  name: request-count
  namespace: istio-system
spec:
  quotaSpecs:
  - name: request-count
    namespace: istio-system
  services:
  - name: productpage
    namespace: default
    #  - service: '*'  # Uncomment this to bind *all* services to request-count
```

最终是将我们的计数器绑定到具体的服务上，实质上在官网上check services的参数，可以发现其主要目的是为了拼接我们的service domain。

## 针对具体业务的限流实现

### 短信业务

为了防止短信业务被DDOS，我们需要限制短信业务流量大小

```yaml
apiVersion: config.istio.io/v1alpha2
kind: handler
metadata:
  name: sms-quotahandler
  namespace: istio-system
spec:
  compiledAdapter: memquota
  params:
    quotas:
    - name: sms-requestcountquota.instance.istio-system
      maxAmount: 500
      validDuration: 1s
      # The first matching override is applied.
      # A requestcount instance is checked against override dimensions.
      overrides:
      # The following override applies to 'reviews' regardless
      # of the source.
      - dimensions:
          destination: sms
        maxAmount: 1
        validDuration: 5s
---
apiVersion: config.istio.io/v1alpha2
kind: instance
metadata:
  name: sms-requestcountquota
  namespace: istio-system
spec:
  compiledTemplate: quota
  params:
    dimensions:
      source: request.headers["x-forwarded-for"] | "unknown"
      destination: destination.labels["app"] | destination.service.name | "unknown"
      destinationVersion: destination.labels["version"] | "unknown"
---
apiVersion: config.istio.io/v1alpha2
kind: rule
metadata:
  name: sms-quota
  namespace: istio-system
spec:
  # quota only applies if you are not logged in.
  # match: match(request.headers["cookie"], "user=*") == false
  actions:
  - handler: sms-quotahandler
    instances:
    - sms-requestcountquota
---
apiVersion: config.istio.io/v1alpha2
kind: QuotaSpec
metadata:
  name: sms-request-count
  namespace: istio-system
spec:
  rules:
  - quotas:
    - charge: 1
      quota: sms-requestcountquota
---
apiVersion: config.istio.io/v1alpha2
kind: QuotaSpecBinding
metadata:
  name: sms-request-count
  namespace: istio-system
spec:
  quotaSpecs:
  - name: request-count
    namespace: istio-system
  services:
  - name: sms
    namespace: default
```

