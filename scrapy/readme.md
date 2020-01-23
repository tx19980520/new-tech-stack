# Scrapy

Scrapy 是python种比较成熟的爬虫框架，日常生活中，爬虫主要为技术人员的提供丰富的数据用后后续的分析，我们研究爬虫的目的在于获取数据进行学习

![img](./images/scrapy_architecture.jpg)

## 安装&&创建项目

我们使用anaconda安装scrapy 并且在某目录下进行项目创建

```bash
conda install scrapy
cd /to/a/local/path
scrapy startproject cnanime
```

这个时候我们的文件夹下创建了一个名为NewsSpider的文件夹，里面就是我们该项目的具体文件，我们将针对文件对该架构进行描述

### settings.py

项目的配置文件，主要管理项目下的crawl、middleware和pipelines以及一些默认设置，我列几个比较重要的设置

```python
BOT_NAME = 'cnanime'
# 框架的pipeline注册和优先级设置
ITEM_PIPELINES = { 'cnanime.pipelines.bilibiliImagesPipeline': 2,"cnanime.pipelines.bilibiliSpecificPipeline":1}
#是否遵守协议，会从网站的相关地方下载一个robot.txt里面会标示一些禁忌，即不允许得到的数据
ROBOTSTXT_OBEY = True
#框架的middlewares注册和优先级测试
SPIDER_MIDDLEWARES = {
    'bilibili.middlewares.BilibiliSpiderMiddleware': 543,
}
# http请求头的相关设置
DEFAULT_REQUEST_HEADERS = {
   'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
   'Accept-Language': 'en',
}
```

在你对项目的其他部分进行修改后，请注意修改settings里面的内容。

## spiders.py

spider文件夹下可以存在多个spiders，用于定义爬虫的相应方法。

```python
class CnanimeSpider(scrapy.Spider):
    name = "cnanime" #define spider name
    allowed_domains = ["bilibili.com"]
    start_urls=start_urls # must defined
    # class method must define 
    def parse(self, response):
            yield item1 #yeild now item ,but the function is not over.  
            yield scrapy.Request(url,callback = self.sub_parse,meta={'id':int(item["animeId"].encode('utf-8'))}) #yield a new request and point to a sub_parse function

    def sub_parse(self,response):
        item = animeSpecificItem()
        yield item2

```

注意一定要理解yield用法，yield才能实现真正的pipeline操作。

## pipeline.py

pipeline适用于经过spider处理之后对数据model的进一步处理，以某种方式存入数据库或者文件中。

```python
# -*- coding: utf-8 -*-

# Define your item pipelines here
#
# Don't forget to add your pipeline to the ITEM_PIPELINES setting
# See: https://doc.scrapy.org/en/latest/topics/item-pipeline.html
import scrapy
from scrapy.pipelines.images import ImagesPipeline
from scrapy.exceptions import DropItem
import cnanime.items as Items
from scrapy.exporters import JsonLinesItemExporter# 选择导出json
class bilibiliSpecificPipeline(object):
    def __init__(self):
        self.fileSpecific = open('./data/specific2.json','wb')
        self.exporterSpecific = JsonLinesItemExporter(self.fileSpecific,encoding="utf-8",ensure_ascii=False)#创建导出器
        self.exporterSpecific.start_exporting()#打开导出方法
    def process_item(self,item,spider):
        if isinstance(item,Items.animeSpecificItem):#这个地方只处理我们的animeSpecificItem
            self.exporterSpecific.export_item(item)# 导出具体的数据
        return item;

    def close_spider(self,spider):
        self.exporterSpecific.finish_exporting()#关闭导出器
        self.fileSpecific.close()#关闭文件

```

`__init__`、`__process_item__`和`__close_spider__`是相对必要的，另外导出文件的格式和地址可以设置在settings.py中

## middleware=>selenium+webdriver

在爬取的内容进入到core被分发到各处进行工作之前，首先可以进入到我们的middleware进行处理，这个地方我们主要强调的是我们使用selenium+webdriver来进行模拟的操作。

```python
#a speicial define show here but actually it is in the spider.py
def __init__(self):
        self.browser = webdriver.Chrome(executable_path="C:/Program Files (x86)/Google/Chrome/Application/chromedriver.exe")
        super(cnanime, self).__init__()
        
        
        
        
class JSPageMiddleware(object):

    #通过chrome 动态访问
    def process_request(self,request,spider):
        if spider.name =="qicha":
            spider. browser.get(request.url)
            import time
            time.sleep(3)
            print "访问：{0}".format(request.url)
            
            #spider.browser.find_element_by_xpath('//div[@class="bottom hide"]/a[@class="link"]').click()

            spider.browser.find_element_by_xpath('//div[@class="form-group has-feedback m-l-lg m-r-lg m-t-xs m-b-none"]/input[@name="nameNormal"]').send_keys("你自己的账号")
            spider.browser.find_element_by_xpath('//div[@class="form-group has-feedback m-l-lg m-r-lg m-t-xs m-b-none"]/input[@name="pwdNormal"]').send_keys("你自己的密码")
            time.sleep(9)
            #spider.browser.find_element_by_xpath('//div[@class="m-l-lg m-r-lg m-t-lg"]/button[@class="btn  btn-primary     m-t-n-xs btn-block btn-lg font-15"]').click()
            return HtmlResponse(url=spider.browser.current_url,body=spider.browser.page_source,encoding="utf-8")
```

这里我们会在spider里面多加一个定义,我们在middleware.py中间件中增加一个新的中间件，用于模拟我们登录到了页面，进行表单的填写和提交，可以用于模拟用户登录。给大家推荐一个看middleware结构的[repo](https://github.com/zhangshier/scrapy-/tree/master/qichacha)