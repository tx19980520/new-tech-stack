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

## 中文分词

由于elasticsearch的中文分词是单字分词，对中文的原生支持并不好，因此我们需要下载中文分词器

```shell
./bin/elasticsearch-plugin install https://github.com/medcl/ela
sticsearch-analysis-ik/releases/download/v6.5.4/elasticsearch-analysis-ik-6.5.4.zip
#具体的版本号请到插件的github仓库中进行查看
```

之后重启ES，对于需要使用ik分词器的字段，请进行如下的配置

```json
{
  "properties": {
      "content": {
         "type": "text",
         "analyzer": "ik_max_word",
         "search_analyzer": "ik_max_word"
    }
  }
}'
```

`ik_max_word`和`ik_smart`为最细和最粗两种分词模式，可以根据不同的需要进行选择

## postman测试

我们最后使用postman进行测试，方便之后的接口提供，非常的方便，只需要将在body中写入在Kibana中原有的json字段即可。

```json
{
    "query": {
        "match": {
            "address": "山东"
        }
    }
}
```

