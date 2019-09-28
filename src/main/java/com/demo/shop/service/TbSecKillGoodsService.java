package com.demo.shop.service;

import com.demo.shop.entity.TbSeckillGoods;
import com.demo.shop.utils.ResultData;

import java.util.List;

public interface TbSecKillGoodsService {

    List<TbSeckillGoods> findAll();

    TbSeckillGoods findOne(Long id);

    ResultData saveOrder(Long id, String userId);

}
