package com.demo.shop.order;

import com.demo.shop.entity.TbSeckillGoods;
import com.demo.shop.entity.TbSeckillOrder;
import com.demo.shop.mapper.TbSeckillGoodsMapper;
import com.demo.shop.utils.Constant;
import com.demo.shop.utils.IdWorker;
import com.demo.shop.utils.OrderRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class OrderCreateThread implements Runnable {

    @Autowired
    private TbSeckillGoodsMapper tbSeckillGoodsMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private IdWorker idWorker;

    private Lock lock = new ReentrantLock();


    @Override
    public void run() {

        lock.lock();
        try {
            OrderRecord record = (OrderRecord) redisTemplate.boundListOps(OrderRecord.class.getSimpleName()).rightPop();
            if (record != null) {
                TbSeckillGoods tbSeckillGoods = (TbSeckillGoods) redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).get(record.getId());
                //4.生成秒杀订单数据，将订单也放到redis中
                TbSeckillOrder order = new TbSeckillOrder();
                order.setUserId(record.getUserId());
                order.setSellerId(tbSeckillGoods.getSellerId());
                order.setSeckillId(idWorker.nextId());
                order.setMoney(tbSeckillGoods.getCostPrice());
                order.setCreateTime(new Date());
                order.setStatus("0");
                redisTemplate.boundHashOps(TbSeckillOrder.class.getSimpleName()).put(record.getUserId(), order);
                //5.商品库存量-1

                tbSeckillGoods.setStockCount(tbSeckillGoods.getStockCount() - 1);
                //6.库存量是否<=0,<=0时将秒杀商品更新到数据库，删除秒杀商品缓存，>0时，将商品最新的信息更新到缓存
                tbSeckillGoodsMapper.updateByPrimaryKeySelective(tbSeckillGoods);
                if (tbSeckillGoods.getStockCount() <= 0) {
                    redisTemplate.boundHashOps(TbSeckillOrder.class.getSimpleName()).delete(record.getId());
                } else {
                    redisTemplate.boundHashOps(TbSeckillOrder.class.getSimpleName()).put(record.getId(), tbSeckillGoods);
                }
                //将用户放入用户集合set
                redisTemplate.boundSetOps(Constant.USER_ID_PREFIX + record.getId()).add(record.getUserId());
                //创建OrderRecord对象记录用户下单信息。用户id、商品id，放到orderRecord的队列list中
                OrderRecord orderRecord = new OrderRecord(record.getId(), record.getUserId());
                redisTemplate.boundListOps(OrderRecord.class.getSimpleName()).leftPush(orderRecord);
                //通过线程池启动线程处理orderRecord中的数据


                //return ResultData.success("下单成功");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }


    }
}
