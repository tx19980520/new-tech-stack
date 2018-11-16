# Xunsearch

xunsearch 是一个国产的支持中文分词的搜索引擎，我们在一些项目中对他进行使用是因为它对中文的支持非常好，默认的分词都是非常好的，但该搜索引擎只允许在linux服务器下使用。

## 服务器安装与配置

```shell
wget http://www.xunsearch.com/download/xunsearch-full-latest.tar.bz2
tar -xvf xunsearch-full-latest.tar.bz2 
cd xunsearch/
./setup.sh 
```

安装结束后的文件夹目录如下

```
bin
data
etc
include
lib
sdk
  php
    app
    doc
    lib
    util
share
```

我们重点关心的sdk/php中的文件夹

在其中app里面放的是一些配置文件，这个和sphinx比较相似，具体的ini配置也是针对数据库，服务的端口进行配置。注意这个配置文件我们在本地也需要的保存一份，但是我们需要注意里面的ip在本地的要改成服务器ip。

util文件夹里面有一些接口用于更新index，真正的数据是在data文件夹中，和ini的文件名匹配

我们可以通过以下的指令进行index的简单初始化，当然你可以使用脚本进行的相对应的初始化

```shell
util/Indexer.php --info --project=articles # 针对不同的project进行初始化
```

我本地是一个Laravel framework，我们需要使用composer进行相应的安装

```shell
composer require --prefer-dist hightman/xunsearch
```

之后我们可以将之前说的ini文件放在xunsearch对应的app下面，并在config.php文件中写下path

我们在php中的具体使用为

```php
$xs = new \XS(config_path('articles.ini'));
            $search = $xs->search; // 获取 搜索对象
            $query = $key;
            $search->setFuzzy();
            $search->setQuery($query);

            $docs = $search->search(); // 执行搜索，将搜索结果文档保存在 $docs 数组中
            $count = $search->count(); // 获取搜索结果的匹配总数估算值
```

我们可以对search进行一些设置，之后便可以进行搜索。

我们对数据库中的相关内容进行修改后必须要在一定的时间内将新的index进行插入或者修改。

我们选择的是在Laravel框架中对index进行修改

```php
$data = array(
    'pid' => 234, // 此字段为主键，是进行文档替换的唯一标识
    'subject' => '测试文档的标题',
    'message' => '测试文档的内容部分',
    'chrono' => time()
);
 
// 创建文档对象
$doc = new XSDocument;
$doc->setFields($data);
 
// 更新到索引数据库中
$index->update($doc);
```

