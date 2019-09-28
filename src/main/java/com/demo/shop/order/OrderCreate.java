package com.demo.shop.order;

import com.demo.shop.entity.TbSeckillGoods;
import com.demo.shop.entity.TbSeckillOrder;
import com.demo.shop.mapper.TbSeckillGoodsMapper;
import com.demo.shop.utils.Constant;
import com.demo.shop.utils.IdWorker;
import com.demo.shop.utils.OrderRecord;
import com.demo.shop.utils.ResultData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class OrderCreate {

    @Autowired
    private TbSeckillGoodsMapper tbSeckillGoodsMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private IdWorker idWorker;

    private Lock lock = new ReentrantLock();

    public void createOrder(Long id, String userId) {
        lock.lock();
        try {
            TbSeckillGoods tbSeckillGoods = (TbSeckillGoods) redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).get(id);
            //4.生成秒杀订单数据，将订单也放到redis中
            TbSeckillOrder order = new TbSeckillOrder();
            order.setUserId(userId);
            order.setSellerId(tbSeckillGoods.getSellerId());
            order.setSeckillId(idWorker.nextId());
            order.setMoney(tbSeckillGoods.getCostPrice());
            order.setCreateTime(new Date());
            order.setStatus("0");
            redisTemplate.boundHashOps(TbSeckillOrder.class.getSimpleName()).put(userId, order);
            //5.商品库存量-1
            tbSeckillGoods.setStockCount(tbSeckillGoods.getStockCount() - 1);
            //6.库存量是否<=0,<=0时将秒杀商品更新到数据库，删除秒杀商品缓存，>0时，将商品最新的信息更新到缓存
            tbSeckillGoodsMapper.updateByPrimaryKeySelective(tbSeckillGoods);
            if (tbSeckillGoods.getStockCount() <= 0) {
                redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).delete(id);
            } else {
                redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).put(id, tbSeckillGoods);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }


}
