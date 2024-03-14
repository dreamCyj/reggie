package com.example.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.reggie.common.BaseContext;
import com.example.reggie.common.Result;
import com.example.reggie.dto.OrdersDto;
import com.example.reggie.dto.SetmealDto;
import com.example.reggie.entity.*;
import com.example.reggie.service.OrderDetailService;
import com.example.reggie.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;
    @PostMapping("/submit")
    public Result<String> submit(@RequestBody Orders orders){
        log.info("用户提交订单");
        orderService.submit(orders);
        return Result.success("下单成功");
    }
    @GetMapping("/userPage")
    public Result<Page<OrdersDto>> userPage(int page, int pageSize){
        Page<Orders> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getUserId, BaseContext.getCurrentId());
        queryWrapper.orderByDesc(Orders::getOrderTime);
        orderService.page(pageInfo, queryWrapper);
        //对象拷贝
        Page<OrdersDto> ordersDtoPage = new Page<>();
        BeanUtils.copyProperties(pageInfo,ordersDtoPage,"records");//把pageInfo中除了records的全部拷贝给dishDtoPage
        List<Orders> records = pageInfo.getRecords();//records为页面展示的菜品列表，需要单独处理
        List<OrdersDto> list = records.stream().map((item) -> {
            OrdersDto ordersDto = new OrdersDto();
            BeanUtils.copyProperties(item, ordersDto);
            String orderId = item.getNumber();//拿到每个item订单的的订单id
            //根据orderId到order_detail表查询对应订单详情信息
            LambdaQueryWrapper<OrderDetail> queryWrapper1 = new LambdaQueryWrapper<>();//根据id查到当前订单
            queryWrapper1.eq(OrderDetail::getOrderId, orderId);
            List<OrderDetail> orderDetailList = orderDetailService.list(queryWrapper1);
            if(orderDetailList != null){
                ordersDto.setOrderDetails(orderDetailList);
            }
            return ordersDto;
        }).toList();
        ordersDtoPage.setRecords(list);//records单独处理完成
        return Result.success(ordersDtoPage);
    }
    @GetMapping("/list")
    public Result<List<Orders>> list(){
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getUserId, BaseContext.getCurrentId());
        queryWrapper.orderByDesc(Orders::getOrderTime);
        List<Orders> list = orderService.list(queryWrapper);
        return Result.success(list);
    }
    @GetMapping("/page")
    public Result<Page<Orders>> page(int page, int pageSize, String number, String beginTime, String endTime){
        log.info("分页查询参数：{}, {}, {}, {}, {}", page, pageSize, number, beginTime, endTime);
        //构造分页构造器
        Page<Orders> pageInfo = new Page<>(page, pageSize);
        //构造条件构造器
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotEmpty(number), Orders::getNumber, number);
        if(beginTime != null && endTime != null){
            LocalDateTime beginTime1 = LocalDateTime.parse(beginTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            LocalDateTime endTime1 = LocalDateTime.parse(endTime,DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            queryWrapper.between(Orders::getOrderTime, beginTime1, endTime1);
        }
        queryWrapper.orderByDesc(Orders::getOrderTime);
        //执行查询
        orderService.page(pageInfo, queryWrapper);

        return Result.success(pageInfo);
    }

    @PutMapping
    public Result<String> setStatus(@RequestBody Orders orders){
        orderService.updateById(orders);
        return Result.success("正派送订单");
    }

}
