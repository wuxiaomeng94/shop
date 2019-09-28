package com.demo.shop;


import com.demo.shop.entity.TbSeckillGoods;
import com.demo.shop.mapper.TbSeckillGoodsMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ShopApplicationTests {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    /*@Autowired
    private RedisTemplate<Object, Employee> employeeRedisTemplate;*/
    @Autowired
    private RedisTemplate<Object, Object> jsonRedisTemplate;
    @Autowired
    private TbSeckillGoodsMapper tbSeckillGoodsMapper;

    @Test
    public void contextLoads() {

        long id = 1;
        TbSeckillGoods tbSeckillGoods = tbSeckillGoodsMapper.selectByPrimaryKey(id);
        System.out.println(tbSeckillGoods);
        jsonRedisTemplate.opsForValue().set("goods"+id, tbSeckillGoods);
    }





}
