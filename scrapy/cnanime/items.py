# -*- coding: utf-8 -*-

# Define here the models for your scraped items
#
# See documentation in:
# https://doc.scrapy.org/en/latest/topics/items.html

import scrapy


class BilibiliItem(scrapy.Item):
    # define the fields for your item here like:
    # name = scrapy.Field()
    __v = scrapy.Field()
    animeId = scrapy.Field()
    animeTitle = scrapy.Field()
    animePicturePath = scrapy.Field()
    animePictureUrl = scrapy.Field()
    animeFinished = scrapy.Field()
    fans = scrapy.Field()
class animeSpecificItem(scrapy.Item):
    __v = scrapy.Field()
    actor = scrapy.Field()
    evaluate = scrapy.Field()
    coins = scrapy.Field()
    episodes = scrapy.Field()
    rating = scrapy.Field()
    tags = scrapy.Field()
    animeId = scrapy.Field()
