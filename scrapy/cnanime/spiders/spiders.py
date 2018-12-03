# -*- coding: utf-8 -*-
import scrapy
import json
import time
from cnanime.items import BilibiliItem,animeSpecificItem;

start_urls = []
'''
for bilibili specific and bilibili simple data;
'''
for page in range(1,21):
    url = "http://bangumi.bilibili.com/web_api/season/index_cn?page=%d&page_size=20&version=0&is_finish=0&start_year=0&tag_id=&index_type=1&index_sort=0&quarter=0"%(page)
    start_urls.append(url)
class CnanimeSpider(scrapy.Spider):
    name = "cnanime"
    allowed_domains = ["bilibili.com"]
    start_urls=start_urls
    def parse(self, response):
        body= json.loads(response.body)
        result =body['result']
        animeList = result['list']
        for anime in animeList:
            item = BilibiliItem()
            item["animeId"] = anime['season_id']
            item["animeTitle"] = anime["title"]
            item["animePictureUrl"] = anime["cover"]
            item['fans'] = anime['favorites']
            item['animeFinished'] = anime['is_finish']
            now = int(time.time())
            url = "https://bangumi.bilibili.com/jsonp/seasoninfo/%d.ver?callback=seasonListCallback&jsonp=jsonp&_=%d"%(int(item["animeId"].encode('utf-8')),now)
            yield item
            yield scrapy.Request(url,callback = self.sub_parse,meta={'id':int(item["animeId"].encode('utf-8'))})

    def sub_parse(self,response):
        body =json.loads(response.body[19:-2])
        result = body['result']
        item = animeSpecificItem()
        item['actor'] = result["actor"]
        item['evaluate'] = result['evaluate']
        item['coins'] = result['coins']
        item['episodes'] = result['episodes']
        item['animeId'] = response.meta['id']
        try:
            item['rating'] = result['media']['rating']
        except:
            item['rating'] = {}
            pass
        try:
            item['tags'] = []
            for i in range(0,len(result['tags'])):
                item['tags'].append(result['tags'][i]['tag_name'])
        except:
            item['tags'] = []
            pass
        yield item
