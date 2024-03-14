package com.example.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.example.reggie.common.BaseContext;
import com.example.reggie.common.Result;

import com.example.reggie.entity.ShoppingCart;
import com.example.reggie.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/shoppingCart")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;
    @GetMapping("/list")
    public Result<List<ShoppingCart>> get(){
        log.info("查看购物车数据");
        Long userId = BaseContext.getCurrentId();
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, userId);
        List<ShoppingCart> shoppingCartList = shoppingCartService.list(queryWrapper);
        return Result.success(shoppingCartList);
    }
    @PostMapping("/add")
    public Result<ShoppingCart> add(@RequestBody ShoppingCart shoppingCart){
        log.info("购物车数据:{}", shoppingCart);
        // shoppingCartService.save(shoppingCart);
        //添加是一次一次的 每次添加菜品或套餐的一种 可以多次添加
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);
        Long dishId = shoppingCart.getDishId();
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, userId);//查询当前用户的购物车
        if(dishId != null){
            //此次添加的是菜品dish
            queryWrapper.eq(ShoppingCart::getDishId, dishId);
            //queryWrapper.eq(ShoppingCart::getDishFlavor, shoppingCart.getDishFlavor());//是否已有名称且口味相同的菜品存在 -->前端做的不能换口味
        }else {
            //此次添加的是套餐
            queryWrapper.eq(ShoppingCart::getSetmealId,shoppingCart.getSetmealId());
        }
        ShoppingCart one = shoppingCartService.getOne(queryWrapper);
        if(one != null){
            //表中已存在名称且口味相同的菜品  或者 套餐 ,在原来的基础上数量加一
            Integer number = one.getNumber();
            one.setNumber(number + 1);
            shoppingCartService.updateById(one);
        }else {
            //表中不存在 新增
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartService.save(shoppingCart);
            one = shoppingCart;
        }
        return Result.success(one);
    }
    @PostMapping("/sub")
    public Result<ShoppingCart> sub(@RequestBody ShoppingCart shoppingCart){ //根据dish或setmeal的id更改购物车中数量
        //更改的是dish
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(shoppingCart.getDishId() != null, ShoppingCart::getDishId, shoppingCart.getDishId());
        queryWrapper.eq(shoppingCart.getSetmealId() != null, ShoppingCart::getSetmealId, shoppingCart.getSetmealId());
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());//要有用户的概念 一切操作都只针对正在登录的用户
        ShoppingCart one = shoppingCartService.getOne(queryWrapper);
        Integer number = one.getNumber();
        if(number>=1){
            number -= 1;
            if(number != 0){
                one.setNumber(number);
                shoppingCartService.updateById(one);
            } else {
                one.setNumber(number);
                shoppingCartService.removeById(one);}
            }

        return Result.success(one);

    }
    @DeleteMapping("/clean")
    public Result<String> delete(){
        log.info("清空购物车");
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, BaseContext.getCurrentId());
        shoppingCartService.remove(queryWrapper);
        return Result.success("清空购物车");
    }

}
