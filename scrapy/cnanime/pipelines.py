# -*- coding: utf-8 -*-

# Define your item pipelines here
#
# Don't forget to add your pipeline to the ITEM_PIPELINES setting
# See: https://doc.scrapy.org/en/latest/topics/item-pipeline.html
import scrapy
from scrapy.pipelines.images import ImagesPipeline
from scrapy.exceptions import DropItem
import cnanime.items as Items
from scrapy.exporters import JsonLinesItemExporter

#class BilibiliPipeline(object):
#    def process_item(self, item, spider):
#        return item
fileBilibili = open("./data/bilibili2.json",'wb')
exporterBilibili = JsonLinesItemExporter(fileBilibili,encoding="utf-8",ensure_ascii=False)
exporterBilibili.start_exporting()
class bilibiliImagesPipeline(ImagesPipeline):
    def close_spider(self, spider):
        exporterBilibili.finish_exporting()
        fileBilibili.close()
    def get_media_requests(self, item, info):
        if isinstance(item,Items.BilibiliItem):
            image_url = item['animePictureUrl']
            print (image_url)
            yield scrapy.Request(image_url)
    def item_completed(self, results, item, info):#这个放在后面
        if isinstance(item,Items.BilibiliItem):
            image_paths = [x['path'] for ok, x in results if ok]      # ok判断是否下载成功
            if not image_paths:
                raise DropItem("Item contains no images")
            item['animePicturePath'] = image_paths[0]
            exporterBilibili.export_item(item);
        return item
class bilibiliSpecificPipeline(object):
    def __init__(self):
        self.fileSpecific = open('./data/specific2.json','wb')
        self.exporterSpecific = JsonLinesItemExporter(self.fileSpecific,encoding="utf-8",ensure_ascii=False)
        self.exporterSpecific.start_exporting()
    def process_item(self,item,spider):
        if isinstance(item,Items.animeSpecificItem):#这个地方只处理我们的animeSpecificItem
            self.exporterSpecific.export_item(item)
        return item;

    def close_spider(self,spider):
        self.exporterSpecific.finish_exporting()
        self.fileSpecific.close()
