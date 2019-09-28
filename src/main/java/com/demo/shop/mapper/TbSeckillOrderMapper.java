package com.demo.shop.mapper;

import com.demo.shop.entity.TbSeckillOrder;
import com.demo.shop.entity.TbSeckillOrderExample;
import java.util.List;

public interface TbSeckillOrderMapper {
    int deleteByPrimaryKey(Long id);

    int insert(TbSeckillOrder record);

    int insertSelective(TbSeckillOrder record);

    List<TbSeckillOrder> selectByExample(TbSeckillOrderExample example);

    TbSeckillOrder selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(TbSeckillOrder record);

    int updateByPrimaryKey(TbSeckillOrder record);
}