# Istio migrate 1.1 to 1.2

本文主要描述为什么我们要从Istio 1.1.3 迁移到Istio1.2.5，简要描述在迁移过程中的相关难点，具体的迁移步骤详见[这里](<https://istio.io/docs/setup/kubernetes/upgrade/steps/>)

## migrate的原因

对于框架的更新，鉴于本项目开发进度比较紧急，且在之前没有相关需求需要新版本支持才能进行实践操作，因而一直对Istio的新版本奉行”鸵鸟政策“，但由于[jwt-in-istio-exact](https://github.com/istio/istio/issues/16099)和出现[503-race](https://github.com/istio/istio/issues/14037)的情况相继出现在1.1.x版本中。我们需要进行相关的操作来最低程度的保证服务的正常运行和安全，因此选择升级。

## migrate带来的影响

在本项目中暂无影响，但近期的操作表明authentication仍旧无效，原因不明，需要进一步排查，感觉应该是有cache的因素在其中。

## 启示

使用框架或者软件，尤其是在商业用途中，一定要对version, lisence进行check，注意release note和社区的活跃程度，不能够对demo的查阅就开始进行实践，以免给后序维护和实践带来不必要的麻烦。