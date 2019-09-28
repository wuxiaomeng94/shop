package com.demo.shop.controller;

import com.demo.shop.entity.TbSeckillGoods;
import com.demo.shop.service.TbSecKillGoodsService;
import com.demo.shop.utils.Constant;
import com.demo.shop.utils.ResultData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
//@RequestMapping(value = "shop")
public class ShopController {

    @Autowired
    private TbSecKillGoodsService tbSecKillGoodsService;


    @RequestMapping(value = "index")
    public String shopIndex() {
        return "index";
        //return "seckill-index";
    }


    @RequestMapping(value = "secKillGoodsList")
    @ResponseBody
    public ResultData secKillGoodsList() {
        ResultData result = ResultData.getInstance();
        result.setFlag(true);
        result.setMsg("success");
        List<TbSeckillGoods> list = tbSecKillGoodsService.findAll();
        result.setData(list);
        return result;
    }

    /*@RequestMapping(value = "detail")
    public String detail(String id, Model model) {
        model.addAttribute("id", id);
        return "seckill-item";
    }*/

    @RequestMapping(value = "secKillDetail")
    @ResponseBody
    public ResultData secKillDetail(Long id) {
        TbSeckillGoods tbSeckillGoods = tbSecKillGoodsService.findOne(id);
        ResultData result = ResultData.success("success");
        result.setData(tbSeckillGoods);
        return result;
    }


    @RequestMapping(value = "saveOrder")
    @ResponseBody
    public ResultData saveOrder(Long id) {
        ResultData resultData = tbSecKillGoodsService.saveOrder(id, Constant.USER_ID);
        return resultData;
    }

}
