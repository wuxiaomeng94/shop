package com.demo.shop.service.impl;

import com.demo.shop.entity.TbSeckillGoods;
import com.demo.shop.entity.TbSeckillGoodsExample;
import com.demo.shop.entity.TbSeckillOrder;
import com.demo.shop.mapper.TbSeckillGoodsMapper;
import com.demo.shop.mapper.TbSeckillOrderMapper;
import com.demo.shop.order.OrderCreate;
import com.demo.shop.order.OrderCreateThread;
import com.demo.shop.service.TbSecKillGoodsService;
import com.demo.shop.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@Service
@Transactional
public class TbSeckillGoodsServiceImpl implements TbSecKillGoodsService {

    @Autowired
    private TbSeckillGoodsMapper tbSeckillGoodsMapper;
    @Autowired
    private TbSeckillOrderMapper tbSeckillOrderMapper;
    @Autowired
    private RedisTemplate<Object, Object> jsonRedisTemplate;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private IdWorker idWorker;
    @Autowired
    private Executor executor;
    @Autowired
    private OrderCreate orderCreate;
    @Autowired
    private OrderCreateThread orderCreateThread;




    /*@Cacheable(cacheNames = "TbSeckillGoods")
    @Override
    public List<TbSeckillGoods> findAll() {
        Date nowDate = new Date();
        TbSeckillGoodsExample example = new TbSeckillGoodsExample();
        TbSeckillGoodsExample.Criteria criteria = example.createCriteria();
        criteria.andStatusEqualTo("1")
                .andStockCountGreaterThan(0)
                .andStartTimeLessThanOrEqualTo(nowDate)
                .andEndTimeGreaterThan(nowDate);
        List<TbSeckillGoods> list = tbSeckillGoodsMapper.selectByExample(example);
        return list;
    }*/

    @Override
    public List<TbSeckillGoods> findAll() {
        List list = redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).values();
        /*List<Object> list2 = jsonRedisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).values();
        list2.forEach((obj) -> {
            String str = obj.toString();
            str = str.replaceAll("=", ":");
            System.out.println(str);
            TbSeckillGoods goods = JsonUtil.jsonStrToPo(str, TbSeckillGoods.class);
            System.out.println("========");
        });*/
        return list;
    }

    @Override
    public TbSeckillGoods findOne(Long id) {
        return (TbSeckillGoods) redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).get(id);
    }

    @Override
    public ResultData saveOrder(Long id, String userId) {

        try {
            //从用户的set集合中判断用户是否已下单   如果已下单或者正在下单，提示用户正在排队或有订单未支付
            Boolean member = redisTemplate.boundSetOps(Constant.USER_ID_PREFIX + id).isMember(userId);
            if (member) {
                //正在排队
                return ResultData.error("还有未支付的商品正在排队等待支付。请尽快支付");
            }
            //1.从redis中获取秒杀商品   -->修改，从redis的list队列中获取商品id
            id = (Long) redisTemplate.boundListOps(Constant.GOODS_PREFIX + id).rightPop();
            //2.判断商品是否存在，库存是否>0
            if (id == null) {
                //3.商品不存在，或库存<0，返回失败提示已售罄
                return ResultData.error("商品已售罄");
            }
            //将用户放入用户集合set
            redisTemplate.boundSetOps(Constant.USER_ID_PREFIX + id).add(userId);
            //创建OrderRecord对象记录用户下单信息。用户id、商品id，放到orderRecord的队列list中
            OrderRecord orderRecord = new OrderRecord(id, userId);
            redisTemplate.boundListOps(OrderRecord.class.getSimpleName()).leftPush(orderRecord);
            //通过线程池启动线程处理orderRecord中的数据
            Long finalId = id;
            executor.execute(() -> {
                orderCreate.createOrder(finalId, userId);
            });
            //executor.execute(orderCreateThread);
            return ResultData.success("下单成功");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultData.error("error");
        }
    }

    /**
     *
     * TbSeckillGoods tbSeckillGoods = (TbSeckillGoods) redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).get(id);
     *             //4.生成秒杀订单数据，将订单也放到redis中
     *             TbSeckillOrder order = new TbSeckillOrder();
     *             order.setUserId(userId);
     *             order.setSellerId(tbSeckillGoods.getSellerId());
     *             order.setSeckillId(idWorker.nextId());
     *             order.setMoney(tbSeckillGoods.getCostPrice());
     *             order.setCreateTime(new Date());
     *             order.setStatus("0");
     *             redisTemplate.boundHashOps(TbSeckillOrder.class.getSimpleName()).put(userId, order);
     *             //5.商品库存量-1
     *             tbSeckillGoods.setStockCount(tbSeckillGoods.getStockCount() - 1);
     *             //6.库存量是否<=0,<=0时将秒杀商品更新到数据库，删除秒杀商品缓存，>0时，将商品最新的信息更新到缓存
     *             tbSeckillGoodsMapper.updateByPrimaryKeySelective(tbSeckillGoods);
     *             if (tbSeckillGoods.getStockCount() <= 0) {
     *                 redisTemplate.boundHashOps(TbSeckillOrder.class.getSimpleName()).delete(id);
     *             } else {
     *                 redisTemplate.boundHashOps(TbSeckillOrder.class.getSimpleName()).put(id, tbSeckillGoods);
     *             }
     *             return ResultData.success("下单成功");
     *
     *
     *
     */



    //@Scheduled(cron = "0 0/30 * * * ?")
    @Scheduled(cron = "0 0/2 * * * ?")
    //@Scheduled(cron = "0 0 12,18 * * ?")
    public void cacheTask() {
        //1.查询商品数据    有效：status=1,库存量>0 stock_count>0,秒杀开始时间<=当前时间<秒杀结束时间
        Date nowDate = new Date();
        TbSeckillGoodsExample example = new TbSeckillGoodsExample();
        TbSeckillGoodsExample.Criteria criteria = example.createCriteria();
        criteria.andStatusEqualTo("1")
                .andStockCountGreaterThan(0)
                .andStartTimeLessThanOrEqualTo(nowDate)
                .andEndTimeGreaterThan(nowDate);
        List<TbSeckillGoods> list = tbSeckillGoodsMapper.selectByExample(example);
        //2.存入redis
        /*list.forEach((goods) -> {
            //jsonRedisTemplate.opsForHash();
            //jsonRedisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).put(goods.getId(), goods);
            redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).put(goods.getId(), goods);
        });*/
        for (TbSeckillGoods tbSeckillGoods : list) {
            redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).put(tbSeckillGoods.getId(), tbSeckillGoods);
            //为每一个库存商品创建一个list队列，队列中放和库存量相同数量的商品id
            createQueue(tbSeckillGoods.getId(), tbSeckillGoods.getStockCount());
        }


    }


    private void createQueue(long id, Integer count) {
        if (count > 0) {
            Long size = redisTemplate.boundListOps(Constant.GOODS_PREFIX + id).size();
            if (size == null || size.intValue() != count) {
                for (int i = 0; i < count; i++) {
                    redisTemplate.boundListOps(Constant.GOODS_PREFIX+ id).leftPush(id);
                }
            }
        }
    }


}
