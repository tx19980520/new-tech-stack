# Knative & Fission
作为severless的先驱，我们的需要对比一下两个相关框架的设计理念和性能。最后在测试集群中进行测试。
## Knative
Knative一开始就已经与Istio进行了结合，因此在网络方面knative能够做到的事情更多，能够通过网络来进行HPA。
### Service  Function
在knative中主要是serving，最基本的Service的配置文件如下：

``` yaml
apiVersion: serving.knative.dev/v1 # Current version of Knative
kind: Service
metadata:
  name: helloworld-go # The name of the app
  namespace: default # The namespace the app will use
spec:
  template:
    spec:
      containers:
        - image: gcr.io/knative-samples/helloworld-go # The URL to the image of the app
          env:
            - name: TARGET # The environment variable printed out by the sample app
              value: "Go Sample v1"
```
我们可以注意到，这个配置文件和一般的k8s Deployment没有特别大的区别，主要是apiVersion有些不同。
#### Knative Pod Autoscaler (KPA)
knative的HPA实现是KPA，在Istio强大网络的支持下，knative不仅仅在cpu层次上进行监控，在pod数量上，甚至可以在网络请求上面做相关的工作。

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
 name: config-autoscaler
 namespace: knative-serving
data:
 container-concurrency-target-default: 100
 container-concurrency-target-percentage: 1.0
 enable-scale-to-zero: true
 enable-vertical-pod-autoscaling: false
 max-scale-up-rate: 10
 panic-window: 6s
 scale-to-zero-grace-period: 30s
 stable-window: 60s
 tick-interval: 2s
```
这个方面knative能够做到的功能更多，但是我们也要考虑到性能相关的问题。
### Knative Image Prepare
knative 在 service中仍旧是传统的image准备，代码和环境是紧耦合的，对于部署的过程也是相应的简单，这一点从上述的配置文件中也是有所体现。
## Fission
fission在使用一段时间后给人的感觉是更加侧重于web的开发，直接将router对外暴露，直接添加相关的代码进入到对应的环境中，就能够进行部署，网络转发部分不依赖于Istio，而是使用fission自带的router。
### Fssion Executer
executer基本位于Fission架构的中心。主要是负责管理Fission生成的Pod，Fission有两种部署的方式：PoolManager &NewDeploy
![Alt](https://i.loli.net/2019/11/06/SlszqZTYCpOBnuM.jpg)
PoolManager主要理念是维护一个Pod 池，以一个环境为基础，初始化多个Pod，在需要使用的时候，将已经build过的代码植入到相应的Pod中，即Specialized过程。关于已经特化过的Pod后续的生命周期将是一个非常有趣也值得用户进行进一步设置的部分。
![Alt](https://i.loli.net/2019/11/06/BZC8LTjHbY1pIxP.jpg)
NewDeploy和knative的KPA比较相近，可以在Pod数量和cpu两个角度进行调控，但是这里要强调的是，我们不能从网络的角度进行控制。

> The new deployment based executor provides autoscaling for functions based on CPU usage. In future custom metrics will be also supported for scaling the functions. 

```
--mincpu value         Minimum CPU to be assigned to pod (In millicore, minimum 1)
--maxcpu value         Maximum CPU to be assigned to pod (In millicore, minimum 1)
--minmemory value      Minimum memory to be assigned to pod (In megabyte)
--maxmemory value      Maximum memory to be assigned to pod (In megabyte)
--minscale value       Minimum number of pods (Uses resource inputs to configure HPA)
--maxscale value       Maximum number of pods (Uses resource inputs to configure HPA)
--targetcpu value      Target average CPU usage percentage across pods for scaling (default: 80)
```
### Fission Environment Prepare
Fission将environment和code两者进行了分离，我们环境可以通过image直接创建，而对于代码，则使用mount将相应的二进制文件或脚本置入 environment Pod中，完成特化。这样的好处在于，使用PoolManager来管理环境，可以进行复用，但是这样的话，两个function在性能层面上将会存在一些依赖，这也是需要进行探讨

