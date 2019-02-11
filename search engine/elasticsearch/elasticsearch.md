# ElasticSearch

ElasticSearch（以下简称ES）是基于java开发的一套快速易上手的搜索框架，就目前本人的掌握的情况而言，它有如下的几个优势：

1. 易搭建，开箱即用
2. Kibana可视化操作简单易懂
3. RESTful API更新索引

## 安装与运行

下载请点击[这里](https://www.elastic.co/downloads)，建议安装ES和Kibana，在此之前请配置好java环境

1. 运行ES

   ```shell
   cd /path/to/es/bin
   ./elasticsearch
   ```

2. 运行Kibana

   ```shell
   cd /path/to/kibana/bin
   ./kibana
   ```

关于端口的相关配置，具体请移步config文件夹下进行配置

入门级操作可以选择进入到Kibana中进行操作，Kibana中存在Dev Tools，可以模拟进行对RESTful API进行访问，从索引的创建到更新索引，都可以通过RESTful API进行操作，且在入门情况下，可以不需要与数据库搭配进行操作。

详细的一系列操作请参考[这里](https://blog.csdn.net/linhaiyun_ytdx/article/details/79601743)

