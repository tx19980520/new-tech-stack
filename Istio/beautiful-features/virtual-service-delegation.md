# Virtual Service Delegation

上班就忘了更新了，github 上白得一批。

kubecon 上听闻到 Virtual Service Delegation 的改动，跑到 release note 上一看确实有，开始进行了解。

进行该项开发的原始需求来自于一个过于巨大的 VirtualService 将给运维带来极大的麻烦，大家都打开一个文件，进行修改，最后 apply，没有在各种层面上进行分层，隔离，是一个扁平化的 VirtualService。现在的情况而言是希望把更多的心智负担留给 Istio 而非 end user，end user 仅仅只是加了一层抽象。

在设计文档中，认为其中的难点在于，如何 Merge(正则等规则的merge 方式)， 如何解决 dependency loop。这里的修改是从头到尾都需要进行修改的，pilot 需要修改，需要检查是否存在 dependency loop， 不过仔细思考似乎是有利于增量 xds 的实现的，另外是 envoy 部分也要考虑一些实现细节的适配，比如多个正则规则如何规定规则的优先级，这也是需要充分考虑的。

Ref

[Virtual Service chaining](https://docs.google.com/document/d/1TzXjvbEqeRKHQ7FonAY_yS1zHa8rVH2DIq4EM5BuDQ4/edit#heading=h.xw1gqgyqs5b)

[Virtual Service Delegation](https://docs.google.com/document/d/1TzXjvbEqeRKHQ7FonAY_yS1zHa8rVH2DIq4EM5BuDQ4/edit#)

[Delegation](https://istio.io/latest/docs/reference/config/networking/virtual-service/#Delegate)