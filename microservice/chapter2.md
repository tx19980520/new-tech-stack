# Chapter2 演化式架构师

本章的逻辑结构明显，主要是在讲，一个架构师到底应当做什么样的工作，应该围绕架构师，建立怎么样的组织，来处理好**分治与统一**之间的关系。

## 分治

> 我们主要关注服务之间的交互，而不需要关注各个服务内部发生的事情

本章节中，分区对应着分治，这里仅仅是强调，架构师需要协调好各个分区，粒度的粗细非常重要，另外一个方面是要关注在服务服务边界，服务边界的交互是及其重要的。我们手头上的工具Istio等等，等式在强调服务之间的交互，例如服务之间的网络等等，另一方面kubernetes也没有任何有关于内部代码的显示暴露，对其他的服务而言，他们只能看到另一服务的端口。

## 统一——原则、实践和标准

架构师需要能够通过公司的战略目标，来设计原则，比如，我们的原则就是快速发布，保证伸缩性。原则的内容比较少，但是在我们具体的实践或者是标准，以及到具体的代码中，我们都需要有对原则的体现，这就是一种统一。实践与标准都是需要巩固标准的，整个围绕架构师展开的团队，需要能够就相关的实践和标准在团队内部进行相应。架构师的手不能伸得太远，但各团队需要与你分享时，则需要与大家进行进一步的讨论，从而自身也能感受到相应的原则是否有需要改动的地方。

> 在重用代码的驱动下，我们可能会引入服务之间的耦合...
>
> 这个时候他们就要非常小心地防止DRY的追求导致系统过度耦合

实话实说，我在大二的时候对DRY是非常追崇的，但是DRY却是在微服务架构下有非常大的问题。这里我们以项目中对JWT的处理，我们是将JWT的解码作为公共库放置在众多的服务之中，这带来的好处就是，我们在本地就能进行JWT的解码，进行相应的工作，但带来的问题，在于对于该代码的修改，将会使得所有的服务都需要重新构建（Springboot）。这确实是一件非常难以维护的工作。

在标准方面，我认为需要一定的自治性，也需要集中统一的管理，或者是一种发扬光大，这有些类似于包产到户的实施，这就是需要我们的组织能够反馈各个组别里面的相关工作，具体的处理情况，统一讨论后可以进行推广，甚至上升到原则的高度（太细粒度的不需要）。

> 我们的技术愿景有其本身的道理，所以偏离了这个愿景短期可能会带来利益，但是长期来看是需要

项目里面，这样的场景其实是常见的，这里最明显的就是我们在数据库上的设计，至今存在着非常大的技术债务，我们的数据库可扩展性太差，之前经历过一次扩展，作出的相关无可奈何地操作使得项目的可维护性和耦合度更高，是应当对一些不当且无奈的操作进行记录，甚至再当时就设计好相应的恢复方案，这样才是长久之策。
